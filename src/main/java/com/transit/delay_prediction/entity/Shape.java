package com.transit.delay_prediction.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Shape {
    @Id
    private String id; // Composite of shapeId and shapePtSequence
    private String shapeId;
    private double shapePtLat;
    private double shapePtLon;
    private int shapePtSequence;

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getShapeId() { return shapeId; }
    public void setShapeId(String shapeId) { this.shapeId = shapeId; }
    public double getShapePtLat() { return shapePtLat; }
    public void setShapePtLat(double shapePtLat) { this.shapePtLat = shapePtLat; }
    public double getShapePtLon() { return shapePtLon; }
    public void setShapePtLon(double shapePtLon) { this.shapePtLon = shapePtLon; }
    public int getShapePtSequence() { return shapePtSequence; }
    public void setShapePtSequence(int shapePtSequence) { this.shapePtSequence = shapePtSequence; }
}