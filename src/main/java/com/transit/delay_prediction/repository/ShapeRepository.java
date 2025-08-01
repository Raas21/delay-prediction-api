package com.transit.delay_prediction.repository;

import com.transit.delay_prediction.entity.Shape;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShapeRepository extends JpaRepository<Shape, String> {
    List<Shape> findByShapeIdOrderByShapePtSequence(String shapeId);
}