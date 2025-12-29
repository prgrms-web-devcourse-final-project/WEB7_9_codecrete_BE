package com.back.web7_9_codecrete_be.domain.concerts.service.concertSevice;

import com.back.web7_9_codecrete_be.domain.concerts.dto.concert.ConcertDetailResponse;
import com.back.web7_9_codecrete_be.domain.concerts.dto.concert.ConcertItem;
import com.back.web7_9_codecrete_be.domain.concerts.dto.concert.ListSort;
import com.back.web7_9_codecrete_be.domain.concerts.entity.Concert;
import com.back.web7_9_codecrete_be.domain.concerts.repository.ConcertRepository;
import com.back.web7_9_codecrete_be.domain.concerts.service.ConcertService;
import com.back.web7_9_codecrete_be.domain.users.entity.User;
import com.back.web7_9_codecrete_be.domain.users.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static reactor.netty.http.HttpConnectionLiveness.log;

@SpringBootTest
@Profile("test")
public class ConcertServiceTest {
    @Autowired
    private ConcertService concertService;

    @Autowired
    private ConcertRepository concertRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void t1_getConcertListTest(){
        // given
        Pageable pageable = PageRequest.of(0, 3);

        // when
        List<ConcertItem> concertItems = concertService.getConcertsList(pageable, ListSort.VIEW);

        //then
        assertThat(concertItems).isNotEmpty();
        assertThat(concertItems.size()).isEqualTo(3);
        assertThat(concertItems.get(0)).isNotNull();
        assertThat(concertItems.get(0)).isInstanceOf(ConcertItem.class);
    }

    @Test
    @Transactional
    void t2_concertLikeTest(){
        // given
        User user = userRepository.findById(1L).get();
        concertService.dislikeConcert(1L, user);
        entityManager.flush();
        entityManager.clear();

        //when
        Concert beforeConcert = concertRepository.findById(1L).get();
        concertService.likeConcert(1L, user);
        entityManager.flush();
        entityManager.clear();

        //then
        Concert afterConcert = concertRepository.findById(1L).get();
        concertService.dislikeConcert(1L, user);
        entityManager.flush();
        entityManager.clear();

        assertThat(beforeConcert).isNotNull();
        assertThat(afterConcert).isNotNull();
        assertThat(beforeConcert.getConcertId()).isEqualTo(afterConcert.getConcertId());
        assertThat(afterConcert.getLikeCount()).isEqualTo(beforeConcert.getLikeCount()+1);

    }

    @Test
    @Transactional
    void t3_concertDislikeTest(){
        // given
        User user = userRepository.findById(1L).get();
        entityManager.flush();
        entityManager.clear();
        // when
        Concert beforeConcert = concertRepository.findById(1L).get();
        concertService.dislikeConcert(1L, user);
        entityManager.flush();
        entityManager.clear();
        // then
        Concert afterConcert = concertRepository.findById(1L).get();

        assertThat(beforeConcert).isNotNull();
        assertThat(afterConcert).isNotNull();
        assertThat(beforeConcert.getConcertId()).isEqualTo(afterConcert.getConcertId());
        assertThat(afterConcert.getLikeCount()).isEqualTo(beforeConcert.getLikeCount()-1);
    }




}
