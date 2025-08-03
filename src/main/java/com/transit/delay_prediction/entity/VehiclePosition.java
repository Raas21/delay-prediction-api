package com.transit.delay_prediction.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Entity representing a vehicle's real-time position from GTFS-RT feed.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class VehiclePosition {
    private String vehicleId;
    private String tripId;
    private String routeId;
    private String stopId;
    private double latitude;
    private double longitude;
    private LocalDateTime timestamp;
    private int delay; // Delay in seconds
}