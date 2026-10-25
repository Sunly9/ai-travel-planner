package org.example.travel_project1.repository;

import org.example.travel_project1.entity.TravelPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TravelPlanRepository extends JpaRepository<TravelPlan, Long> {

    List<TravelPlan> findByUserIdOrderByCreateTimeDesc(Long userId);

    long countByUserId(Long userId);

    /** 取一段时间内的创建时间，用于统计页的 7 天趋势 */
    @Query("select p.createTime from TravelPlan p where p.userId = :userId and p.createTime >= :from")
    List<LocalDateTime> findCreateTimesSince(@Param("userId") Long userId,
                                             @Param("from") LocalDateTime from);
}
