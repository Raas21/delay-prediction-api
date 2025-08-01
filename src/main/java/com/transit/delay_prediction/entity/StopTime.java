package com.transit.delay_prediction.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import java.time.LocalTime;

@Entity
public class StopTime {
    @Id
    private String id; // Composite of tripId and stopSequence
    @ManyToOne
    @JoinColumn(name = "trip_id")
    private Trip trip;
    private LocalTime arrivalTime;
    private LocalTime departureTime;
    @ManyToOne
    @JoinColumn(name = "stop_id")
    private Stop stop;
    private int stopSequence;
    private int pickupType;
    private int dropOffType;
    private int timepoint;

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Trip getTrip() { return trip; }
    public void setTrip(Trip trip) { this.trip = trip; }
    public LocalTime getArrivalTime() { return arrivalTime; }
    public void setArrivalTime(LocalTime arrivalTime) { this.arrivalTime = arrivalTime; }
    public LocalTime getDepartureTime() { return departureTime; }
    public void setDepartureTime(LocalTime departureTime) { this.departureTime = departureTime; }
    public Stop getStop() { return stop; }
    public void setStop(Stop stop) { this.stop = stop; }
    public int getStopSequence() { return stopSequence; }
    public void setStopSequence(int stopSequence) { this.stopSequence = stopSequence; }
    public int getPickupType() { return pickupType; }
    public void setPickupType(int pickupType) { this.pickupType = pickupType; }
    public int getDropOffType() { return dropOffType; }
    public void setDropOffType(int dropOffType) { this.dropOffType = dropOffType; }
    public int getTimepoint() { return timepoint; }
    public void setTimepoint(int timepoint) { this.timepoint = timepoint; }
}
