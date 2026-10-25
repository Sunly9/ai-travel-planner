package org.example.travel_project1.repository;

import org.example.travel_project1.entity.GeoCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GeoCacheRepository extends JpaRepository<GeoCache, Long> {

    Optional<GeoCache> findByAddressAndCity(String address, String city);
}
