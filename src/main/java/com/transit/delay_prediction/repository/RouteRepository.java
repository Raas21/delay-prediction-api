package com.transit.delay_prediction.repository;

import com.transit.delay_prediction.entity.Route;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RouteRepository extends JpaRepository<Route, String> {
    List<Route> findByRouteShortName(String routeShortName);
}