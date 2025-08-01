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

import java.io.File;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

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

    public void ingestGtfsStaticData(String gtfsFolderPath) throws Exception {
        File gtfsDir = new File(gtfsFolderPath);
        if (!gtfsDir.exists() || !gtfsDir.isDirectory()) {
            throw new IllegalArgumentException("Invalid GTFS directory: " + gtfsFolderPath);
        }

        GtfsReader reader = new GtfsReader();
        reader.setEntityStore(new GtfsRelationalDaoImpl());
        reader.setInputLocation(gtfsDir);
        reader.run();

        GtfsRelationalDaoImpl dao = (GtfsRelationalDaoImpl) reader.getEntityStore();

        // Ingest agencies
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

        // Ingest routes
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

        // Ingest stops
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
            entity.setParentStation(gtfsStop.getParentStation() != null ? gtfsStop.getParentStation() : null);
            stopRepository.save(entity);
            logger.info("Saved stop: {}", gtfsStop.getId().getId());
        }

        // Ingest calendars
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
            entity.setStartDate(LocalDate.parse(gtfsCalendar.getStartDate().toString(), DateTimeFormatter.BASIC_ISO_DATE));
            entity.setEndDate(LocalDate.parse(gtfsCalendar.getEndDate().toString(), DateTimeFormatter.BASIC_ISO_DATE));
            calendarRepository.save(entity);
            logger.info("Saved calendar: {}", gtfsCalendar.getServiceId().getId());
        }

        // Ingest calendar dates
        for (org.onebusaway.gtfs.model.ServiceCalendarDate gtfsCalendarDate : dao.getAllCalendarDates()) {
            com.transit.delay_prediction.entity.CalendarDate entity = new com.transit.delay_prediction.entity.CalendarDate();
            String dateString = gtfsCalendarDate.getDate().toString();
            entity.setId(gtfsCalendarDate.getServiceId().getId() + "_" + dateString);
            entity.setServiceId(gtfsCalendarDate.getServiceId().getId());
            entity.setDate(LocalDate.parse(dateString, DateTimeFormatter.BASIC_ISO_DATE));
            entity.setExceptionType(gtfsCalendarDate.getExceptionType());
            calendarDateRepository.save(entity);
            logger.info("Saved calendar date: {}", entity.getId());
        }

        // Ingest shapes
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

        // Ingest trips
        for (org.onebusaway.gtfs.model.Trip gtfsTrip : dao.getAllTrips()) {
            com.transit.delay_prediction.entity.Trip entity = new com.transit.delay_prediction.entity.Trip();
            entity.setTripId(gtfsTrip.getId().getId());
            entity.setRoute(routeRepository.findById(gtfsTrip.getRoute().getId().getId()).orElse(null));
            entity.setServiceId(gtfsTrip.getServiceId().getId());
            entity.setTripHeadsign(gtfsTrip.getTripHeadsign());
            entity.setDirectionId(Integer.parseInt(gtfsTrip.getDirectionId()));
            entity.setBlockId(gtfsTrip.getBlockId());
            entity.setShapeId(gtfsTrip.getShapeId() != null ? gtfsTrip.getShapeId().getId() : null);
            tripRepository.save(entity);
            logger.info("Saved trip: {}", gtfsTrip.getId().getId());
        }

        // Ingest stop times
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
        return LocalTime.of(hours, minutes, secs);
    }
}