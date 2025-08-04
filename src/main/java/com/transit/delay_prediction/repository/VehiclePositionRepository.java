package com.transit.delay_prediction.repository;

import com.transit.delay_prediction.entity.VehiclePosition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VehiclePositionRepository extends JpaRepository<VehiclePosition, Long> {
}