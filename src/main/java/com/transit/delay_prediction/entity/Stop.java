package com.transit.delay_prediction.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Stop {
    @Id
    private String stopId;
    private String stopName;
    private String stopDesc;
    private double stopLat;
    private double stopLon;
    private String zoneId;
    private String stopUrl;
    private int locationType;
    private String parentStation;

    // Getters and setters
    public String getStopId() { return stopId; }
    public void setStopId(String stopId) { this.stopId = stopId; }
    public String getStopName() { return stopName; }
    public void setStopName(String stopName) { this.stopName = stopName; }
    public String getStopDesc() { return stopDesc; }
    public void setStopDesc(String stopDesc) { this.stopDesc = stopDesc; }
    public double getStopLat() { return stopLat; }
    public void setStopLat(double stopLat) { this.stopLat = stopLat; }
    public double getStopLon() { return stopLon; }
    public void setStopLon(double stopLon) { this.stopLon = stopLon; }
    public String getZoneId() { return zoneId; }
    public void setZoneId(String zoneId) { this.zoneId = zoneId; }
    public String getStopUrl() { return stopUrl; }
    public void setStopUrl(String stopUrl) { this.stopUrl = stopUrl; }
    public int getLocationType() { return locationType; }
    public void setLocationType(int locationType) { this.locationType = locationType; }
    public String getParentStation() { return parentStation; }
    public void setParentStation(String parentStation) { this.parentStation = parentStation; }
}