package com.back.web7_9_codecrete_be.domain.concerts.repository;

import com.back.web7_9_codecrete_be.domain.concerts.entity.Concert;
import com.back.web7_9_codecrete_be.domain.concerts.entity.ConcertImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConcertImageRepository extends JpaRepository<ConcertImage, Long> {
    List<ConcertImage> getConcertImagesByConcert_ConcertId(Long concertConcertId);

    void deleteAllByConcert(Concert concert);

    @Modifying
    @Query("""
    DELETE 
    FROM 
    ConcertImage ci
    WHERE 
    ci.concert.concertId = :concertId
""")
    void deleteByConcertId(
            @Param("concertId")
            Long concertId);
}
