package com.back.web7_9_codecrete_be.domain.concerts.repository;

import com.back.web7_9_codecrete_be.domain.concerts.entity.ConcertKopisApiLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConcertKopisApiLogRepository extends JpaRepository<ConcertKopisApiLog, Long> {
}
