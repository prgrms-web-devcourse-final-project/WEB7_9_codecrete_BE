package com.back.web7_9_codecrete_be.domain.concerts.repository;

import com.back.web7_9_codecrete_be.domain.concerts.dto.concert.ConcertDetailResponse;
import com.back.web7_9_codecrete_be.domain.concerts.dto.concert.ConcertItem;
import com.back.web7_9_codecrete_be.domain.concerts.entity.Concert;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
    c.ticketTime as ticketTime,
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
    c.ticketTime as ticketTime,
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
    ORDER BY 
    c.apiConcertId
""")
    List<ConcertItem> getConcertItemsOrderByApiId(Pageable pageable);

    @Query("""
    SELECT
    new com.back.web7_9_codecrete_be.domain.concerts.dto.concert.ConcertItem(
    c.concertId as id,
    c.name as name,
    c.concertPlace.placeName as placeName,
    c.ticketTime as ticketTime,
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
    ORDER BY 
    c.viewCount
    DESC 
""")
    List<ConcertItem> getConcertItemsOrderByViewCountDesc(Pageable pageable);

    @Query("""
    SELECT
    new com.back.web7_9_codecrete_be.domain.concerts.dto.concert.ConcertItem(
    c.concertId as id,
    c.name as name,
    c.concertPlace.placeName as placeName,
    c.ticketTime as ticketTime,
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
    ORDER BY 
    c.likeCount
    desc 
""")
    List<ConcertItem> getConcertItemsOrderByLikeCountDesc(Pageable pageable);


    @Query("""
    SELECT
    new com.back.web7_9_codecrete_be.domain.concerts.dto.concert.ConcertItem(
    c.concertId as id,
    c.name as name,
    c.concertPlace.placeName as placeName,
    c.ticketTime as ticketTime,
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
    List<ConcertItem> getUpComingConcertItemsFromDateASC(
            Pageable pageable,
            @Param("fromDate") LocalDate fromDate
    );

    @Query("""
    SELECT
    new com.back.web7_9_codecrete_be.domain.concerts.dto.concert.ConcertItem(
    c.concertId as id,
    c.name as name,
    c.concertPlace.placeName as placeName,
    c.ticketTime as ticketTime,
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
    c.ticketTime >= :fromDate
    AND 
    c.ticketTime IS NOT NULL
    ORDER BY 
    c.ticketTime
    asc
""")
    List<ConcertItem> getUpComingTicketingConcertItemsFromDateASC(
            Pageable pageable,
            @Param("fromDate") LocalDateTime fromDate
    );

    @Query("""
    SELECT
    new com.back.web7_9_codecrete_be.domain.concerts.dto.concert.ConcertItem(
    c.concertId as id,
    c.name as name,
    c.concertPlace.placeName as placeName,
    c.ticketTime as ticketTime,
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
    c.ticketTime IS NULL
    ORDER BY 
    c.startDate
    DESC
""")
    List<ConcertItem> getNoTicketTimeConcertList(
            Pageable pageable
    );

    @Query("""
    SELECT 
    new com.back.web7_9_codecrete_be.domain.concerts.dto.concert.ConcertItem(
    c.concertId as id,
    c.name as name,
    c.concertPlace.placeName as placeName,
    c.ticketTime as ticketTime,
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
    cl.createDate
    DESC 
"""
    )
    List<ConcertItem> getLikedConcertsList(Pageable pageable,
                                           @Param("userId") Long userId);
    @Query("""
    SELECT
    new com.back.web7_9_codecrete_be.domain.concerts.dto.concert.ConcertItem(
    c.concertId as id,
    c.name as name,
    c.concertPlace.placeName as placeName,
    c.ticketTime as ticketTime,
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
    c.name LIKE %:keyword%
    ORDER BY 
    c.concertId
    DESC
""")
    List<ConcertItem> getConcertItemsByKeyword(
            @Param("keyword")
            String keyword,
            Pageable pageable);

    @Query("""
    SELECT
    new com.back.web7_9_codecrete_be.domain.concerts.dto.concert.ConcertDetailResponse(
    c.concertId as concertId,
    c.name as name,
    c.content as description,
    c.concertPlace.placeName as placeName,
    c.ticketTime as ticketTime,
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


    @Modifying
    @Query("""
    UPDATE 
    Concert c
    SET c.viewCount = c.viewCount + 1
    where c.concertId = :concertId
""")
    Integer concertViewCountUp(@Param("concertId") long concertId);

    @Modifying
    @Query("""
    UPDATE 
    Concert c
    SET c.likeCount = c.likeCount + 1
    where c.concertId = :concertId
""")
    Integer concertLikeCountUp(@Param("concertId") long concertId);

    @Modifying
    @Query("""
    UPDATE 
    Concert c
    SET c.likeCount = c.likeCount - 1
    where c.concertId = :concertId
""")
    Integer concertLikeCountDown(@Param("concertId") long concertId);


    Concert getConcertByConcertId(Long concertId);

    List<Concert> getConcertByTicketTimeAfterAndTicketTimeBefore(LocalDateTime ticketTimeAfter, LocalDateTime ticketTimeBefore);

    List<Concert> getConcertByTicketTimeBetween(LocalDateTime ticketTimeAfter, LocalDateTime ticketTimeBefore);
    
   
    @Query("""
        select distinct c
        from Concert c
        join ConcertArtist ca on ca.concert = c
        where ca.artist.id in :artistIds
    """)
    List<Concert> findDistinctByArtistIds(@Param("artistIds") List<Long> artistIds);
}
