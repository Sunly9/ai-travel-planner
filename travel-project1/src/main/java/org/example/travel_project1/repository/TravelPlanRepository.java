package org.example.travel_project1.repository;

import org.example.travel_project1.entity.TravelPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TravelPlanRepository extends JpaRepository<TravelPlan, Long> {

    List<TravelPlan> findByUserIdOrderByCreateTimeDesc(Long userId);

    long countByUserId(Long userId);
}
