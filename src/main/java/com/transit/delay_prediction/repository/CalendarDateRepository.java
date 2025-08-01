package com.transit.delay_prediction.repository;

import com.transit.delay_prediction.entity.CalendarDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface CalendarDateRepository extends JpaRepository<CalendarDate, String> {
    List<CalendarDate> findByDate(LocalDate date);
}