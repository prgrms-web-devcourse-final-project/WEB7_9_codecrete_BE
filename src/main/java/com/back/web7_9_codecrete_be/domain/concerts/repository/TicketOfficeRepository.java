package com.back.web7_9_codecrete_be.domain.concerts.repository;

import com.back.web7_9_codecrete_be.domain.concerts.entity.Concert;
import com.back.web7_9_codecrete_be.domain.concerts.entity.TicketOffice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketOfficeRepository  extends JpaRepository<TicketOffice, Long> {
    List<TicketOffice> getTicketOfficesByConcert(Concert concert);

    void deleteAllByConcert(Concert concert);

    @Modifying
    @Query("""
    DELETE
    FROM 
    TicketOffice t
    WHERE 
    t.concert.concertId = :concertId
""")
    void deleteByConcertId(
            @Param("concertId")
            Long concertId);

    List<TicketOffice> getTicketOfficesByConcert_ConcertId(Long concertConcertId);
}
