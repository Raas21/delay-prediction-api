package com.transit.delay_prediction.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.time.LocalDate;

@Entity
public class CalendarDate {
    @Id
    private String id; // Composite of serviceId and date
    private String serviceId;
    private LocalDate date;
    private int exceptionType;

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getServiceId() { return serviceId; }
    public void setServiceId(String serviceId) { this.serviceId = serviceId; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public int getExceptionType() { return exceptionType; }
    public void setExceptionType(int exceptionType) { this.exceptionType = exceptionType; }
}