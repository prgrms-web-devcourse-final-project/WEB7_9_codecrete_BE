package com.back.web7_9_codecrete_be.domain.location.repository;

import com.back.web7_9_codecrete_be.domain.location.entity.Location;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LocationRepository extends JpaRepository<Location, Long> {


    Location findByUserId(Long userId);
}
