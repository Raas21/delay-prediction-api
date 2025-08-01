package com.transit.delay_prediction.repository;

import com.transit.delay_prediction.entity.StopTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StopTimeRepository extends JpaRepository<StopTime, String> {
    List<StopTime> findByTripTripIdOrderByStopSequence(String tripId);
}