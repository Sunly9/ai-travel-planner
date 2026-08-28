package org.example.travel_project1.repository;

import org.example.travel_project1.entity.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    List<Favorite> findByUserIdOrderByCreateTimeDesc(Long userId);

    Optional<Favorite> findByUserIdAndSpotName(Long userId, String spotName);

    boolean existsByUserIdAndSpotName(Long userId, String spotName);

    void deleteByUserIdAndSpotName(Long userId, String spotName);

    long countByUserId(Long userId);
}
