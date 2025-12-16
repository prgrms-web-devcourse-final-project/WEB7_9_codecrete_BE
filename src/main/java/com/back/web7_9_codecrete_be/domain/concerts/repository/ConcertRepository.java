package com.back.web7_9_codecrete_be.domain.concerts.repository;

import com.back.web7_9_codecrete_be.domain.concerts.dto.concert.ConcertDetailResponse;
import com.back.web7_9_codecrete_be.domain.concerts.dto.concert.ConcertItem;
import com.back.web7_9_codecrete_be.domain.concerts.entity.Concert;
import com.back.web7_9_codecrete_be.domain.concerts.entity.ConcertPlace;
import com.back.web7_9_codecrete_be.domain.concerts.entity.ConcertTime;
import com.back.web7_9_codecrete_be.domain.users.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ConcertRepository extends JpaRepository<Concert, Long> {
    Concert getConcertByApiConcertId(String apiConcertId);

    @Query("""
    SELECT
    new com.back.web7_9_codecrete_be.domain.concerts.dto.concert.ConcertItem(
    c.concertId as id,
    c.name as name,
    c.concertPlace.placeName as placeName,
    c.startDate as startDate,
    c.endDate as endDate,
    c.posterUrl as posterUrl,
    c.maxPrice as maxPrice,
    c.minPrice as minPrice,
    c.viewCount as viewCount,
    c.likeCount as likeCount
    )
    FROM 
    Concert c
""")
    List<ConcertItem> getConcertItems(Pageable pageable);

    @Query("""
    SELECT
    new com.back.web7_9_codecrete_be.domain.concerts.dto.concert.ConcertItem(
    c.concertId as id,
    c.name as name,
    c.concertPlace.placeName as placeName,
    c.startDate as startDate,
    c.endDate as endDate,
    c.posterUrl as posterUrl,
    c.maxPrice as maxPrice,
    c.minPrice as minPrice,
    c.viewCount as viewCount,
    c.likeCount as likeCount
    )
    FROM 
    Concert c
    WHERE
    c.startDate >= :fromDate
    ORDER BY 
    c.startDate
    asc
""")
    List<ConcertItem> getUpComingConcertItems(
            Pageable pageable,
            @Param("fromDate") LocalDate fromDate
    );

    @Query("""
    SELECT 
    new com.back.web7_9_codecrete_be.domain.concerts.dto.concert.ConcertItem(
    c.concertId as id,
    c.name as name,
    c.concertPlace.placeName as placeName,
    c.startDate as startDate,
    c.endDate as endDate,
    c.posterUrl as posterUrl,
    c.maxPrice as maxPrice,
    c.minPrice as minPrice,
    c.viewCount as viewCount,
    c.likeCount as likeCount
    )
    FROM 
    Concert c,
    ConcertLike cl
    WHERE 
    c.concertId = cl.concert.concertId
    AND 
    cl.user.id = :userId
    ORDER BY
    cl.createdAt
    DESC 
"""
    )
    List<ConcertItem> getLikedConcertsList(Pageable pageable,
                                           @Param("userId") Long userId);

    @Query("""
    SELECT
    new com.back.web7_9_codecrete_be.domain.concerts.dto.concert.ConcertDetailResponse(
    c.concertId as concertId,
    c.name as name,
    c.content as description,
    c.concertPlace.placeName as placeName,
    c.startDate as startDate,
    c.endDate as endDate,
    c.posterUrl as posterUrl,
    c.maxPrice as maxPrice,
    c.minPrice as minPrice,
    c.viewCount as viewCount,
    c.likeCount as likeCount
    )
    FROM
    Concert c
    WHERE
    c.concertId = :concertId
""")
    ConcertDetailResponse getConcertDetailById(@Param("concertId")long concertId);



    Concert getConcertByConcertId(Long concertId);
}
