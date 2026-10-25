package org.example.travel_project1.repository;

import org.example.travel_project1.entity.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    List<Favorite> findByUserIdOrderByCreateTimeDesc(Long userId);

    Optional<Favorite> findByUserIdAndSpotName(Long userId, String spotName);

    boolean existsByUserIdAndSpotName(Long userId, String spotName);

    // 派生删除方法需要显式事务，否则运行时报
    // "No EntityManager with actual transaction available for current thread"
    @Transactional
    void deleteByUserIdAndSpotName(Long userId, String spotName);

    long countByUserId(Long userId);
}
