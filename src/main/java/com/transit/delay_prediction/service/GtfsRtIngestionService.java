package com.transit.delay_prediction.service;

import com.google.transit.realtime.GtfsRealtime.FeedMessage;
import com.transit.delay_prediction.entity.VehiclePosition;
import com.transit.delay_prediction.repository.StopRepository;
import com.transit.delay_prediction.repository.StopTimeRepository;
import com.transit.delay_prediction.repository.TripRepository;
import com.transit.delay_prediction.repository.VehiclePositionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

/**
 * Service for ingesting GTFS-RT data from MTA BusTime API and storing in Redis, PostgreSQL, and Kafka.
 * Fetches vehicle positions every 30 seconds and processes data for Brooklyn routes.
 */
@Service
public class GtfsRtIngestionService {
    private static final Logger logger = LoggerFactory.getLogger(GtfsRtIngestionService.class);

    @Autowired
    private WebClient.Builder webClientBuilder;

    @Autowired
    private ReactiveRedisTemplate<String, VehiclePosition> redisTemplate;

    @Autowired
    private StopRepository stopRepository;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private StopTimeRepository stopTimeRepository;

    @Autowired
    private VehiclePositionRepository vehiclePositionRepository;

    @Autowired
    private KafkaTemplate<String, VehiclePosition> kafkaTemplate;

    @Value("${mta.bustime.api.key}")
    private String apiKey;

    @Value("${mta.bustime.api.url:http://gtfsrt.prod.obanyc.com/vehiclePositions}")
    private String apiUrl;

    private static final String TOPIC = "vehicle_positions";

    /**
     * Starts periodic GTFS-RT ingestion after service initialization.
     */
    @PostConstruct
    public void startIngestion() {
        Flux.interval(Duration.ofSeconds(30))
            .flatMap(i -> fetchGtfsRtData())
            .subscribe(
                vehiclePosition -> logger.info("Stored vehicle position: {}", vehiclePosition.getVehicleId()),
                error -> logger.error("Error during GTFS-RT ingestion: {}", error.getMessage(), error)
            );
        logger.info("Started periodic GTFS-RT ingestion (every 30 seconds)");
    }

    /**
     * Fetches GTFS-RT data from MTA BusTime API and processes vehicle positions.
     * @return Flux of VehiclePosition entities stored in Redis, PostgreSQL, and Kafka.
     */
    private Flux<VehiclePosition> fetchGtfsRtData() {
        return webClientBuilder.build()
            .get()
            .uri(apiUrl + "?key={key}", apiKey)
            .retrieve()
            .bodyToMono(FeedMessage.class)
            .doOnSuccess(feed -> logger.info("Received GTFS-RT feed with {} entities", feed.getEntityCount()))
            .doOnError(error -> logger.error("Failed to fetch GTFS-RT feed: {}", error.getMessage(), error))
            .flatMapMany(this::processFeedMessage)
            .flatMap(this::storeInRedisPostgresAndKafka);
    }

    /**
     * Processes GTFS-RT FeedMessage into VehiclePosition entities.
     * Filters for Brooklyn routes based on route_id starting with 'B'.
     * Estimates delay by comparing timestamp with scheduled stop_time.
     * @param feedMessage GTFS-RT feed message.
     * @return Flux of VehiclePosition entities.
     */
    private Flux<VehiclePosition> processFeedMessage(FeedMessage feedMessage) {
        return Flux.fromIterable(feedMessage.getEntityList())
            .filter(entity -> entity.hasVehicle() && entity.getVehicle().hasTrip())
            .filter(entity -> {
                String routeId = entity.getVehicle().getTrip().getRouteId();
                return routeId != null && routeId.startsWith("B");
            })
            .doOnNext(entity -> logger.debug("Processing entity with routeId: {}", 
                entity.getVehicle().getTrip().getRouteId()))
            .flatMap(entity -> {
                var vehicle = entity.getVehicle();
                var trip = vehicle.getTrip();
                VehiclePosition position = new VehiclePosition();
                position.setVehicleId(vehicle.getVehicle().getId());
                position.setTripId(trip.getTripId());
                position.setRouteId(trip.getRouteId());
                position.setStopId(vehicle.hasStopId() ? vehicle.getStopId() : null);
                position.setLatitude(vehicle.getPosition().getLatitude());
                position.setLongitude(vehicle.getPosition().getLongitude());
                position.setTimestamp(LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochSecond(vehicle.getTimestamp()),
                    ZoneId.of("America/New_York")
                ));

                // Estimate delay using stop_time if stop_id and trip_id are available
                if (vehicle.hasStopId() && vehicle.hasCurrentStopSequence()) {
                    return Mono.fromCallable(() -> stopTimeRepository.findByTripTripIdAndStopStopIdAndStopSequence(
                            trip.getTripId(), vehicle.getStopId(), vehicle.getCurrentStopSequence()))
                        .flatMap(stopTime -> {
                            if (stopTime != null && stopTime.getArrivalTime() != null) {
                                LocalDateTime scheduled = LocalDateTime.now()
                                    .with(stopTime.getArrivalTime());
                                long delaySeconds = ChronoUnit.SECONDS.between(
                                    scheduled, position.getTimestamp());
                                position.setDelay((int) delaySeconds);
                                logger.debug("Calculated delay for vehicleId={}: {} seconds", 
                                    vehicle.getVehicle().getId(), delaySeconds);
                            } else {
                                position.setDelay(0);
                                logger.debug("No stopTime found for tripId={}, stopId={}, sequence={}", 
                                    trip.getTripId(), vehicle.getStopId(), vehicle.getCurrentStopSequence());
                            }
                            return Mono.just(position);
                        })
                        .switchIfEmpty(Mono.just(position))
                        .doOnError(error -> logger.error("Error querying StopTime for tripId: {}, stopId: {}, sequence: {}: {}", 
                            trip.getTripId(), vehicle.getStopId(), vehicle.getCurrentStopSequence(), error.getMessage()));
                } else {
                    position.setDelay(0);
                    logger.debug("No stopId or stopSequence for vehicleId={}, setting delay=0", 
                        vehicle.getVehicle().getId());
                    return Mono.just(position);
                }
            })
            .doOnNext(position -> logger.debug("Processed vehicle position: {}", position.getVehicleId()));
    }

    /**
     * Stores VehiclePosition in Redis with a 5-minute TTL, PostgreSQL, and Kafka.
     * @param position VehiclePosition entity.
     * @return Mono of stored VehiclePosition.
     */
    private Mono<VehiclePosition> storeInRedisPostgresAndKafka(VehiclePosition position) {
        String key = "vehicle_position:" + position.getVehicleId();
        return redisTemplate.opsForValue()
            .set(key, position, Duration.ofMinutes(5))
            .then(Mono.fromCallable(() -> vehiclePositionRepository.save(position)))
            .doOnSuccess(p -> kafkaTemplate.send(TOPIC, position.getVehicleId(), position))
            .thenReturn(position)
            .doOnSuccess(p -> logger.info("Stored in Redis, PostgreSQL, and Kafka: key={}", key))
            .doOnError(error -> logger.error("Error storing in Redis/PostgreSQL/Kafka for key {}: {}", key, error.getMessage()));
    }
}