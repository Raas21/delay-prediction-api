package com.transit.delay_prediction.service;

import com.transit.delay_prediction.entity.*;
import com.transit.delay_prediction.repository.*;
import org.onebusaway.gtfs.impl.GtfsRelationalDaoImpl;
import org.onebusaway.gtfs.model.*;
import org.onebusaway.gtfs.serialization.GtfsReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Service for ingesting GTFS static data into the PostgreSQL database.
 * Each entity type (agency, route, stop, etc.) is processed in a separate transaction
 * to ensure partial data is saved even if one section fails.
 */
@Service
public class GtfsStaticIngestionService {
    private static final Logger logger = LoggerFactory.getLogger(GtfsStaticIngestionService.class);

    @Autowired
    private AgencyRepository agencyRepository;
    @Autowired
    private CalendarRepository calendarRepository;
    @Autowired
    private CalendarDateRepository calendarDateRepository;
    @Autowired
    private RouteRepository routeRepository;
    @Autowired
    private ShapeRepository shapeRepository;
    @Autowired
    private StopRepository stopRepository;
    @Autowired
    private StopTimeRepository stopTimeRepository;
    @Autowired
    private TripRepository tripRepository;

    /**
     * Ingests GTFS static data from the specified folder path.
     * @param gtfsFolderPath Path to the GTFS directory containing files like agency.txt, routes.txt, etc.
     * @throws Exception If the directory is invalid or GTFS reading fails.
     */
    public void ingestGtfsStaticData(String gtfsFolderPath) throws Exception {
        logger.info("Starting ingestion for folder: {}", gtfsFolderPath);
        File gtfsDir = new File(gtfsFolderPath);
        if (!gtfsDir.exists() || !gtfsDir.isDirectory()) {
            logger.error("Invalid GTFS directory: {}", gtfsFolderPath);
            throw new IllegalArgumentException("Invalid GTFS directory: " + gtfsFolderPath);
        }

        GtfsReader reader = new GtfsReader();
        reader.setEntityStore(new GtfsRelationalDaoImpl());
        reader.setInputLocation(gtfsDir);
        logger.info("Reading GTFS files from: {}", gtfsFolderPath);
        try {
            reader.run();
        } catch (Exception e) {
            logger.error("Failed to read GTFS files: {}", e.getMessage(), e);
            throw e;
        }

        GtfsRelationalDaoImpl dao = (GtfsRelationalDaoImpl) reader.getEntityStore();
        logger.info("GTFS data loaded. Agencies: {}, Routes: {}, Stops: {}, Calendars: {}, CalendarDates: {}, Shapes: {}, Trips: {}, StopTimes: {}",
                dao.getAllAgencies().size(), dao.getAllRoutes().size(), dao.getAllStops().size(),
                dao.getAllCalendars().size(), dao.getAllCalendarDates().size(), dao.getAllShapePoints().size(),
                dao.getAllTrips().size(), dao.getAllStopTimes().size());

        ingestAgencies(dao);
        ingestRoutes(dao);
        ingestStops(dao);
        ingestCalendars(dao);
        ingestCalendarDates(dao);
        ingestShapes(dao);
        ingestTrips(dao);
        ingestStopTimes(dao);
    }

    @Transactional
    private void ingestAgencies(GtfsRelationalDaoImpl dao) {
        for (org.onebusaway.gtfs.model.Agency gtfsAgency : dao.getAllAgencies()) {
            com.transit.delay_prediction.entity.Agency entity = new com.transit.delay_prediction.entity.Agency();
            entity.setAgencyId(gtfsAgency.getId());
            entity.setAgencyName(gtfsAgency.getName());
            entity.setAgencyUrl(gtfsAgency.getUrl());
            entity.setAgencyTimezone(gtfsAgency.getTimezone());
            entity.setAgencyLang(gtfsAgency.getLang());
            entity.setAgencyPhone(gtfsAgency.getPhone());
            agencyRepository.save(entity);
            logger.info("Saved agency: {}", gtfsAgency.getId());
        }
    }

    @Transactional
    private void ingestRoutes(GtfsRelationalDaoImpl dao) {
        for (org.onebusaway.gtfs.model.Route gtfsRoute : dao.getAllRoutes()) {
            com.transit.delay_prediction.entity.Route entity = new com.transit.delay_prediction.entity.Route();
            entity.setRouteId(gtfsRoute.getId().getId());
            entity.setAgencyId(gtfsRoute.getAgency().getId());
            entity.setRouteShortName(gtfsRoute.getShortName());
            entity.setRouteLongName(gtfsRoute.getLongName());
            entity.setRouteDesc(gtfsRoute.getDesc());
            entity.setRouteType(gtfsRoute.getType());
            entity.setRouteColor(gtfsRoute.getColor());
            entity.setRouteTextColor(gtfsRoute.getTextColor());
            routeRepository.save(entity);
            logger.info("Saved route: {}", gtfsRoute.getId().getId());
        }
    }

    @Transactional
    private void ingestStops(GtfsRelationalDaoImpl dao) {
        for (org.onebusaway.gtfs.model.Stop gtfsStop : dao.getAllStops()) {
            com.transit.delay_prediction.entity.Stop entity = new com.transit.delay_prediction.entity.Stop();
            entity.setStopId(gtfsStop.getId().getId());
            entity.setStopName(gtfsStop.getName());
            entity.setStopDesc(gtfsStop.getDesc());
            entity.setStopLat(gtfsStop.getLat());
            entity.setStopLon(gtfsStop.getLon());
            entity.setZoneId(gtfsStop.getZoneId());
            entity.setStopUrl(gtfsStop.getUrl());
            entity.setLocationType(gtfsStop.getLocationType());
            String parentStation = gtfsStop.getParentStation();
            entity.setParentStation(parentStation);
            logger.info("Processing stop: {}, parentStation: {}", gtfsStop.getId().getId(), parentStation);
            stopRepository.save(entity);
            logger.info("Saved stop: {}", gtfsStop.getId().getId());
        }
    }

    @Transactional
    private void ingestCalendars(GtfsRelationalDaoImpl dao) {
        for (org.onebusaway.gtfs.model.ServiceCalendar gtfsCalendar : dao.getAllCalendars()) {
            com.transit.delay_prediction.entity.Calendar entity = new com.transit.delay_prediction.entity.Calendar();
            entity.setServiceId(gtfsCalendar.getServiceId().getId());
            entity.setMonday(gtfsCalendar.getMonday() == 1);
            entity.setTuesday(gtfsCalendar.getTuesday() == 1);
            entity.setWednesday(gtfsCalendar.getWednesday() == 1);
            entity.setThursday(gtfsCalendar.getThursday() == 1);
            entity.setFriday(gtfsCalendar.getFriday() == 1);
            entity.setSaturday(gtfsCalendar.getSaturday() == 1);
            entity.setSunday(gtfsCalendar.getSunday() == 1);
            String startDateString = gtfsCalendar.getStartDate().getYear() + String.format("%02d", gtfsCalendar.getStartDate().getMonth()) + String.format("%02d", gtfsCalendar.getStartDate().getDay());
            String endDateString = gtfsCalendar.getEndDate().getYear() + String.format("%02d", gtfsCalendar.getEndDate().getMonth()) + String.format("%02d", gtfsCalendar.getEndDate().getDay());
            entity.setStartDate(LocalDate.parse(startDateString, DateTimeFormatter.BASIC_ISO_DATE));
            entity.setEndDate(LocalDate.parse(endDateString, DateTimeFormatter.BASIC_ISO_DATE));
            calendarRepository.save(entity);
            logger.info("Saved calendar: {}", gtfsCalendar.getServiceId().getId());
        }
    }

    @Transactional
    private void ingestCalendarDates(GtfsRelationalDaoImpl dao) {
        for (org.onebusaway.gtfs.model.ServiceCalendarDate gtfsCalendarDate : dao.getAllCalendarDates()) {
            com.transit.delay_prediction.entity.CalendarDate entity = new com.transit.delay_prediction.entity.CalendarDate();
            String dateString = gtfsCalendarDate.getDate().getYear() + String.format("%02d", gtfsCalendarDate.getDate().getMonth()) + String.format("%02d", gtfsCalendarDate.getDate().getDay());
            entity.setId(gtfsCalendarDate.getServiceId().getId() + "_" + dateString);
            entity.setServiceId(gtfsCalendarDate.getServiceId().getId());
            entity.setDate(LocalDate.parse(dateString, DateTimeFormatter.BASIC_ISO_DATE));
            entity.setExceptionType(gtfsCalendarDate.getExceptionType());
            calendarDateRepository.save(entity);
            logger.info("Saved calendar date: {}", entity.getId());
        }
    }

    @Transactional
    private void ingestShapes(GtfsRelationalDaoImpl dao) {
        for (org.onebusaway.gtfs.model.ShapePoint gtfsShape : dao.getAllShapePoints()) {
            com.transit.delay_prediction.entity.Shape entity = new com.transit.delay_prediction.entity.Shape();
            entity.setId(gtfsShape.getShapeId().getId() + "_" + gtfsShape.getSequence());
            entity.setShapeId(gtfsShape.getShapeId().getId());
            entity.setShapePtLat(gtfsShape.getLat());
            entity.setShapePtLon(gtfsShape.getLon());
            entity.setShapePtSequence(gtfsShape.getSequence());
            shapeRepository.save(entity);
            logger.info("Saved shape: {}", entity.getId());
        }
    }

    @Transactional
    private void ingestTrips(GtfsRelationalDaoImpl dao) {
        for (org.onebusaway.gtfs.model.Trip gtfsTrip : dao.getAllTrips()) {
            com.transit.delay_prediction.entity.Trip entity = new com.transit.delay_prediction.entity.Trip();
            entity.setTripId(gtfsTrip.getId().getId());
            entity.setRoute(routeRepository.findById(gtfsTrip.getRoute().getId().getId()).orElse(null));
            entity.setServiceId(gtfsTrip.getServiceId().getId());
            entity.setTripHeadsign(gtfsTrip.getTripHeadsign());
            try {
                entity.setDirectionId(Integer.parseInt(gtfsTrip.getDirectionId()));
            } catch (NumberFormatException e) {
                entity.setDirectionId(0); // Default to 0 if direction_id is invalid
                logger.warn("Invalid direction_id for trip {}: {}", gtfsTrip.getId().getId(), gtfsTrip.getDirectionId());
            }
            entity.setBlockId(gtfsTrip.getBlockId());
            entity.setShapeId(gtfsTrip.getShapeId() != null ? gtfsTrip.getShapeId().getId() : null);
            tripRepository.save(entity);
            logger.info("Saved trip: {}", gtfsTrip.getId().getId());
        }
    }

    @Transactional
    private void ingestStopTimes(GtfsRelationalDaoImpl dao) {
        for (org.onebusaway.gtfs.model.StopTime gtfsStopTime : dao.getAllStopTimes()) {
            com.transit.delay_prediction.entity.StopTime entity = new com.transit.delay_prediction.entity.StopTime();
            entity.setId(gtfsStopTime.getTrip().getId().getId() + "_" + gtfsStopTime.getStopSequence());
            entity.setTrip(tripRepository.findById(gtfsStopTime.getTrip().getId().getId()).orElse(null));
            entity.setArrivalTime(parseGtfsTime(gtfsStopTime.getArrivalTime()));
            entity.setDepartureTime(parseGtfsTime(gtfsStopTime.getDepartureTime()));
            entity.setStop(stopRepository.findById(gtfsStopTime.getStop().getId().getId()).orElse(null));
            entity.setStopSequence(gtfsStopTime.getStopSequence());
            entity.setPickupType(gtfsStopTime.getPickupType());
            entity.setDropOffType(gtfsStopTime.getDropOffType());
            entity.setTimepoint(gtfsStopTime.getTimepoint());
            stopTimeRepository.save(entity);
            logger.info("Saved stop time: {}", entity.getId());
        }
    }

    private LocalTime parseGtfsTime(int seconds) {
        if (seconds < 0) return null;
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        int secs = seconds % 60;
        try {
            return LocalTime.of(hours, minutes, secs);
        } catch (Exception e) {
            logger.warn("Invalid time format for seconds: {}", seconds);
            return null;
        }
    }
}