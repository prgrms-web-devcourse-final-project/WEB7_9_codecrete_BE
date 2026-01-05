package com.back.web7_9_codecrete_be.domain.concerts.repository;

import com.back.web7_9_codecrete_be.domain.concerts.entity.ConcertKopisApiLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

@Repository
public interface ConcertKopisApiLogRepository extends JpaRepository<ConcertKopisApiLog, Long> {
    List<ConcertKopisApiLog> getConcertKopisApiLogByStatusAndWorkType(String status, String workType);

    @Query("""
            SELECT
            createdDate
            FROM
            ConcertKopisApiLog
            ORDER BY
            createdDate
            ASC
            LIMIT 
            1
            """)
    LocalDateTime getLastSaveTime();

    boolean existsConcertKopisApiLogByStatusAndWorkType(String status, String workType);

    @Query("""
            SELECT
            backUpIndex 
            FROM
            ConcertKopisApiLog
            WHERE 
            status = 'fail'
            ORDER BY
            createdDate
            ASC
            LIMIT 
            1
            """)
    Optional<Long> getLastFailIndex();
}
