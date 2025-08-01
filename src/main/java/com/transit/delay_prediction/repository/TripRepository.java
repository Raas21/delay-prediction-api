package com.transit.delay_prediction.repository;

import com.transit.delay_prediction.entity.Trip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TripRepository extends JpaRepository<Trip, String> {
    List<Trip> findByRouteRouteId(String routeId);
}