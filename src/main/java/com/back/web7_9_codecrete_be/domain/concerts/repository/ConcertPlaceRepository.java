package com.back.web7_9_codecrete_be.domain.concerts.repository;

import com.back.web7_9_codecrete_be.domain.concerts.entity.ConcertPlace;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ConcertPlaceRepository extends JpaRepository<ConcertPlace, Long> {
    ConcertPlace findByApiConcertPlaceId(String apiConcertPlaceId);

    ConcertPlace getConcertPlaceByApiConcertPlaceId(String apiConcertPlaceId, Sort sort);

    ConcertPlace getConcertPlaceByApiConcertPlaceId(String apiConcertPlaceId);

    @Query("""
    SELECT
    cp
    FROM
    Concert c
    JOIN c.concertPlace cp
    WHERE
    c.concertId = :concertId
""")
    ConcertPlace getConcertPlaceByConcertId(
            @Param("concertId")long concertId);
}
