package com.back.web7_9_codecrete_be.domain.concerts.repository;

import com.back.web7_9_codecrete_be.domain.concerts.entity.Concert;
import com.back.web7_9_codecrete_be.domain.concerts.entity.ConcertLike;
import com.back.web7_9_codecrete_be.domain.users.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ConcertLikeRepository extends JpaRepository<ConcertLike, Long> {
    ConcertLike findConcertLikeByConcertAndUser(Concert concert, User user);

    boolean existsConcertLikeByConcertAndUser(Concert concert, User user);

    List<ConcertLike> getConcertLikesByConcert(Concert concert);

    // Fetch 사용해서 N+1 문제 해결
    @Query("""
                SELECT 
                cl
                FROM
                ConcertLike cl
                JOIN FETCH 
                    cl.user u
                JOIN 
                    cl.concert c
                WHERE
                c.ticketTime 
                    BETWEEN 
                    :startDate
                    AND
                    :endDate
            """)
    List<ConcertLike> getTodayConcertTicketingLikes(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
}

