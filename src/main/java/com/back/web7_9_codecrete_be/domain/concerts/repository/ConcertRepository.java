package com.back.web7_9_codecrete_be.domain.concerts.repository;

import com.back.web7_9_codecrete_be.domain.concerts.dto.concert.ConcertDetailResponse;
import com.back.web7_9_codecrete_be.domain.concerts.dto.concert.ConcertItem;
import com.back.web7_9_codecrete_be.domain.concerts.entity.Concert;
import com.back.web7_9_codecrete_be.domain.users.entity.User;
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
                c
                FROM
                Concert c
                JOIN FETCH
                c.concertPlace cp
            """)
    List<ConcertItem> getConcertItems(Pageable pageable);

    @Query(value = """
                SELECT
                c
                FROM
                Concert c
                JOIN FETCH
                c.concertPlace cp
                ORDER BY
                c.apiConcertId
            """)
        // Kopis API의 ID 순서대로
    List<ConcertItem> getConcertItemsOrderByApiId(Pageable pageable);

    @Query("""
                SELECT
                c
                FROM
                Concert c
                JOIN FETCH
                c.concertPlace cp
                ORDER BY
                c.viewCount
                DESC,
                c.concertId
            """)
        // 조회수 기준 내림차순
    List<ConcertItem> getConcertItemsOrderByViewCountDesc(Pageable pageable);

    @Query("""
                SELECT
                c
                FROM
                Concert c
                JOIN FETCH
                c.concertPlace cp
                ORDER BY
                c.likeCount
                desc,
                c.concertId
            """)
        // 좋아요 기준 내림차순
    List<ConcertItem> getConcertItemsOrderByLikeCountDesc(Pageable pageable);


    @Query("""
                SELECT
                c
                FROM 
                Concert c
                JOIN FETCH
                c.concertPlace cp
                WHERE
                c.startDate >= :fromDate
                ORDER BY 
                c.startDate
                asc,
                c.concertId
            """)
    List<ConcertItem> getUpComingConcertItemsFromDateASC(
            Pageable pageable,
            @Param("fromDate") LocalDate fromDate
    );

    @Query("""
                SELECT
                c
                FROM 
                Concert c
                JOIN FETCH
                c.concertPlace cp
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
                c
                FROM 
                Concert c
                JOIN FETCH
                c.concertPlace cp
                WHERE 
                c.ticketTime IS NULL
                ORDER BY 
                c.startDate
                DESC,
                c.concertId
            """)
    List<ConcertItem> getNoTicketTimeConcertList(
            Pageable pageable
    );

    @Query("""
                SELECT
                c
                FROM
                Concert c,
                ConcertLike cl
                JOIN FETCH
                c.concertPlace cp
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
                c
                FROM 
                Concert c
                JOIN FETCH
                c.concertPlace cp
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
                c.concertPlace.address as placeAddress,
                c.ticketTime as ticketTime,
                c.ticketEndTime as ticketEndTime,
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
    ConcertDetailResponse getConcertDetailById(@Param("concertId") long concertId);


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
                SET c.viewCount = :viewCount
                where c.concertId = :concertId
            """)
    Integer concertViewCountSet(@Param("concertId") long concertId, @Param("viewCount") int viewCount);

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

    @Query("""
            SELECT
            count(c) 
            FROM 
            Concert c
            WHERE 
            c.ticketTime IS NOT NULL 
            AND 
            c.ticketTime >= :fromDate
            """
    )
    Long countTicketingConcertsFromLocalDateTime(
            @Param("fromDate") LocalDateTime localDateTime
    );


}
