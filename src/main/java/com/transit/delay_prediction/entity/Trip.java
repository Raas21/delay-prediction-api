package com.transit.delay_prediction.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;

@Entity
public class Trip {
    @Id
    private String tripId;
    @ManyToOne
    @JoinColumn(name = "route_id")
    private Route route;
    private String serviceId;
    private String tripHeadsign;
    private int directionId;
    private String blockId;
    private String shapeId;

    // Getters and setters
    public String getTripId() { return tripId; }
    public void setTripId(String tripId) { this.tripId = tripId; }
    public Route getRoute() { return route; }
    public void setRoute(Route route) { this.route = route; }
    public String getServiceId() { return serviceId; }
    public void setServiceId(String serviceId) { this.serviceId = serviceId; }
    public String getTripHeadsign() { return tripHeadsign; }
    public void setTripHeadsign(String tripHeadsign) { this.tripHeadsign = tripHeadsign; }
    public int getDirectionId() { return directionId; }
    public void setDirectionId(int directionId) { this.directionId = directionId; }
    public String getBlockId() { return blockId; }
    public void setBlockId(String blockId) { this.blockId = blockId; }
    public String getShapeId() { return shapeId; }
    public void setShapeId(String shapeId) { this.shapeId = shapeId; }
}