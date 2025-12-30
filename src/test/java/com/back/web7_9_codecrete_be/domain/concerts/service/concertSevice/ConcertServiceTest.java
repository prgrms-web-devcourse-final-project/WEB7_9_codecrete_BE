package com.back.web7_9_codecrete_be.domain.concerts.service.concertSevice;

import com.back.web7_9_codecrete_be.domain.concerts.dto.concert.*;
import com.back.web7_9_codecrete_be.domain.concerts.dto.concertPlace.PlaceDetailResponse;
import com.back.web7_9_codecrete_be.domain.concerts.entity.Concert;
import com.back.web7_9_codecrete_be.domain.concerts.entity.ConcertPlace;
import com.back.web7_9_codecrete_be.domain.concerts.repository.ConcertLikeRepository;
import com.back.web7_9_codecrete_be.domain.concerts.repository.ConcertPlaceRepository;
import com.back.web7_9_codecrete_be.domain.concerts.repository.ConcertRedisRepository;
import com.back.web7_9_codecrete_be.domain.concerts.repository.ConcertRepository;
import com.back.web7_9_codecrete_be.domain.concerts.service.ConcertService;
import com.back.web7_9_codecrete_be.domain.users.entity.SocialType;
import com.back.web7_9_codecrete_be.domain.users.entity.User;
import com.back.web7_9_codecrete_be.domain.users.repository.UserRepository;
import com.back.web7_9_codecrete_be.global.config.TestRedisConfig;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
public class ConcertServiceTest {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ConcertService concertService;

    @Autowired
    private ConcertRepository concertRepository;

    @Autowired
    private ConcertPlaceRepository concertPlaceRepository;

    @Autowired
    private ConcertLikeRepository concertLikeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ConcertRedisRepository concertRedisRepository;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        // test용 사용자 세팅
        User testUser = User.builder()
                .email("test@test.com")
                .nickname("테스트유저")
                .password(passwordEncoder.encode("test1234!"))
                .birth(LocalDate.of(1999, 1, 1))
                .profileImage("https://example.com/profile.jpg")
                .socialType(SocialType.LOCAL)
                .socialId(null)
                .build();
        userRepository.save(testUser);
        // test용 장소 세팅
        ConcertPlace place = concertPlaceRepository.findAll().stream()
                .findFirst()
                .orElseGet(() ->
                        concertPlaceRepository.save(
                                new ConcertPlace(
                                        "테스트 공연장",
                                        "서울특별시 중구 테스트로 123",
                                        37.5665,
                                        126.9780,
                                        5000,
                                        "API-CONCERT-PLACE-1"
                                )
                        )
                );

        LocalDateTime now = LocalDateTime.now();
        // test용 공연 세팅(예매일 O)
        concertRepository.save(
                new Concert(
                        place,
                        "예매일 존재 공연",
                        "예매일이 존재하는 공연",
                        LocalDate.now().plusDays(5),
                        LocalDate.now().plusDays(7),
                        // 내일 00시
                        LocalDateTime.of(LocalDate.now().plusDays(1), LocalTime.MIN),
                        // 모레 00시
                        LocalDateTime.of(LocalDate.now().plusDays(2), LocalTime.MIN),
                        120000,
                        40000,
                        "https://example.com/poster2.jpg",
                        "test-concert-1"
                )
        );

        // test용 공연 세팅(예매일 X)
        concertRepository.save(
                new Concert(
                        place,
                        "예매일 없는 공연",
                        "예매일이 없는 공연",
                        LocalDate.now().plusDays(3),
                        LocalDate.now().plusDays(6),
                        null,
                        null,
                        100000,
                        30000,
                        "https://example.com/poster3.jpg",
                        "test-concert-2"
                )
        );
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
        concertRepository.deleteAll();
        concertPlaceRepository.deleteAll();
        concertRedisRepository.deleteAllCachedConcertDetail();
    }

    @Test
    void t1_getConcertListTest(){
        // given
        Pageable pageable = PageRequest.of(0, 3);

        // when
        List<ConcertItem> concertItems = concertService.getConcertsList(pageable, ListSort.VIEW);

        //then
        assertThat(concertItems).isNotEmpty();
        assertThat(concertItems.size()).isEqualTo(2);
        assertThat(concertItems.get(0)).isNotNull();
        assertThat(concertItems.get(0)).isInstanceOf(ConcertItem.class);
    }

    @Test
    @Transactional
    void t2_concertLikeTest(){
        // given
        User user = userRepository.findByEmail("test@test.com").get();

        //when
        Concert beforeConcert = concertRepository.getConcertByApiConcertId("test-concert-1");
        concertService.likeConcert(beforeConcert.getConcertId(), user);
        entityManager.flush();
        entityManager.clear();

        //then
        Concert afterConcert = concertRepository.getConcertByApiConcertId("test-concert-1");
        concertService.dislikeConcert(afterConcert.getConcertId(), user);
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
        User user = userRepository.findByEmail("test@test.com").get();
        Concert beforeConcert = concertRepository.getConcertByApiConcertId("test-concert-1");
        concertService.likeConcert(beforeConcert.getConcertId(), user);

        // when
        concertService.dislikeConcert(beforeConcert.getConcertId(), user);
        entityManager.flush();
        entityManager.clear();
        // then
        Concert afterConcert = concertRepository.getConcertByApiConcertId("test-concert-1");

        assertThat(beforeConcert).isNotNull();
        assertThat(afterConcert).isNotNull();
        assertThat(beforeConcert.getConcertId()).isEqualTo(afterConcert.getConcertId());
        assertThat(afterConcert.getLikeCount()).isEqualTo(0);
    }


    @Test
    @Transactional
    void t4_concertLikeListTest(){
        // given
        User user = userRepository.findByEmail("test@test.com").get();
        Concert beforeConcert = concertRepository.getConcertByApiConcertId("test-concert-1");
        concertService.likeConcert(beforeConcert.getConcertId(), user);
        Pageable pageable = PageRequest.of(0, 3);

        // when
        List<ConcertItem> likeCocertList = concertService.getLikedConcertsList(pageable,user);
        entityManager.flush();
        entityManager.clear();
        //then
        assertThat(likeCocertList).isNotEmpty();
        assertThat(likeCocertList.size()).isEqualTo(1);
        assertThat(likeCocertList.get(0)).isNotNull();
        assertThat(likeCocertList.get(0)).isInstanceOf(ConcertItem.class);

    }

    @Test
    @Transactional
    void t5_getNoTicketTimeConcertListTest(){
        Pageable pageable = PageRequest.of(0, 3);
        List<ConcertItem> concertItemList = concertService.getNoTicketTimeConcertsList(pageable);
        assertThat(concertItemList).isNotEmpty();
        assertThat(concertItemList.size()).isEqualTo(1);
        assertThat(concertItemList.get(0)).isNotNull();
        assertThat(concertItemList.get(0)).isInstanceOf(ConcertItem.class);
        assertThat(concertItemList.get(0).getTicketTime()).isNull();
        assertThat(concertItemList.get(0).getTicketEndTime()).isNull();
    }

    @Test
    @Transactional
    void t6_getConcertListByKeywordTest(){
        // given
        Pageable pageable = PageRequest.of(0, 3);
        String keyword = "예매";

        // when
        List<ConcertItem> serchResultList = concertService.getConcertListByKeyword(keyword, pageable);

        // then
        assertThat(serchResultList).isNotEmpty();
        assertThat(serchResultList.size()).isEqualTo(2);
        assertThat(serchResultList.get(0)).isNotNull();
        assertThat(serchResultList.get(0)).isInstanceOf(ConcertItem.class);
        assertThat(serchResultList.get(0).getName()).contains("예매");
        assertThat(serchResultList.get(1)).isInstanceOf(ConcertItem.class);
        assertThat(serchResultList.get(1).getName()).contains("예매");
    }

    @Test
    @Transactional
    void t7_autoCompleteSearchTest(){
        //given
        concertService.setAutoComplete();
        String keyword = "예매";

        //when
        List<AutoCompleteItem> searchResult = concertService.autoCompleteSearch(keyword,0,5);

        //then
        assertThat(searchResult).isNotEmpty();
        assertThat(searchResult.size()).isEqualTo(2);
        assertThat(searchResult.get(0)).isNotNull();
        assertThat(searchResult.get(0)).isInstanceOf(AutoCompleteItem.class);
        assertThat(searchResult.get(0).getName()).contains(keyword);
        assertThat(searchResult.get(1)).isNotNull();
        assertThat(searchResult.get(1)).isInstanceOf(AutoCompleteItem.class);
        assertThat(searchResult.get(1).getName()).contains(keyword);

        concertService.resetAutoComplete();
    }

    @Test
    @Transactional
    void t8_getConcertDetailTest(){
        // given
        Concert concert = concertRepository.getConcertByApiConcertId("test-concert-1");

        // when
        ConcertDetailResponse concertDetailResponse = concertService.getConcertDetail(concert.getConcertId());

        // then
        assertThat(concertDetailResponse).isNotNull();
        assertThat(concertDetailResponse.getConcertId()).isEqualTo(concert.getConcertId());
        assertThat(concertDetailResponse.getViewCount()).isEqualTo(concert.getViewCount()+1);
        concertRedisRepository.deleteViewCount(concert.getConcertId());
    }


    @Test
    @Transactional
    void t9_viewCountUpdateTest(){
        // given
        Concert concert = concertRepository.getConcertByApiConcertId("test-concert-1");

        // when
        ConcertDetailResponse concertDetailResponse = concertService.getConcertDetail(concert.getConcertId());
        concertService.viewCountUpdate();
        entityManager.flush();
        entityManager.clear();

        // then
        Concert afterConcert = concertRepository.getConcertByApiConcertId("test-concert-1");

        assertThat(afterConcert).isNotNull();
        assertThat(concert).isNotNull();
        assertThat(afterConcert.getConcertId()).isEqualTo(concert.getConcertId());
        assertThat(afterConcert.getViewCount()).isEqualTo(concert.getViewCount()+1);
        concertRedisRepository.deleteViewCount(concert.getConcertId());
    }

    @Test
    void t10_getTotalConcertsCountTest(){
        // Given: DB에 공연 데이터가 있다고 가정
        long dbCount = concertRepository.count();

        // When
        Long result = concertService.getTotalConcertsCount();

        // Then
        assertThat(result).isEqualTo(dbCount);
        // Redis에도 데이터가 들어갔는지 직접 확인
        assertThat(concertRedisRepository.getTotalConcertsCount(ListSort.VIEW)).isEqualTo(dbCount);
    }

    @Test
    @DisplayName("티켓팅 공연 개수 조회 - 현재 시간 이후의 공연 개수를 반환한다")
    void t11_getTotalTicketingConcertsCountTest() {
        // Given: DB 상태 확인
        long expectedCount = concertRepository.countTicketingConcertsFromLocalDateTime(
                LocalDateTime.of(LocalDate.now(), LocalTime.MIN)
        );

        // When
        Long result = concertService.getTotalTicketingConcertsCount();

        // Then
        assertThat(result).isEqualTo(expectedCount);
    }

    @Test
    @DisplayName("좋아요한 공연 개수 조회 - 유저별 좋아요 개수가 정확히 캐싱되어야 한다")
    void t12_getTotalLikedConcertsCountTest() {
        // 1. Given: 테스트 유저 및 좋아요 데이터 준비
        User user = userRepository.findByEmail("test@test.com").get();

        // 기존 캐시가 방해될 수 있으므로 초기화 (테스트용 메서드 혹은 Redis 직접 삭제)
        // concertRedisRepository.deleteUserLikedCount(user);

        long initialDbCount = concertLikeRepository.countByUser(user);

        // 2. When
        Long result = concertService.getTotalLikedConcertsCount(user);

        // 3. Then
        assertThat(result).isEqualTo(initialDbCount);

        // Redis에 저장된 값도 동일한지 확인
        assertThat(concertRedisRepository.getUserLikedCount(user)).isEqualTo(initialDbCount);
    }

    @Test
    @Transactional
    void t13_setConcertTicketingTimeTest(){
        // given
        Concert concert = concertRepository.getConcertByApiConcertId("test-concert-2");
        ConcertTicketTimeSetRequest concertTicketTimeSetRequest = new ConcertTicketTimeSetRequest(
                concert.getConcertId(),
                LocalDateTime.now().minusDays(5),
                LocalDateTime.now().minusDays(4)
        );

        // when
        concertService.setConcertTicketingTime(concertTicketTimeSetRequest);
        entityManager.flush();
        entityManager.clear();

        // then
        Concert result = concertRepository.getConcertByApiConcertId("test-concert-2");
        ConcertDetailResponse concertDetailResponse = concertService.getConcertDetail(result.getConcertId());

        assertThat(concertDetailResponse).isNotNull();
        assertThat(concertDetailResponse.getConcertId()).isEqualTo(concert.getConcertId());
        assertThat(concertDetailResponse.getTicketTime()).isNotNull();
        assertThat(concertDetailResponse.getTicketEndTime()).isNotNull();

        assertThat(result).isNotNull();
        assertThat(result.getConcertId()).isEqualTo(concert.getConcertId());
        assertThat(result.getTicketTime()).isNotNull();
        assertThat(result.getTicketEndTime()).isNotNull();
    }

    @Test
    void t14_getConcertDetailResponseTest(){
        // given
        ConcertPlace place = concertPlaceRepository.findAll().stream()
                .findFirst()
                .orElseGet(() ->
                        concertPlaceRepository.save(
                                new ConcertPlace(
                                        "테스트 공연장",
                                        "서울특별시 중구 테스트로 123",
                                        37.5665,
                                        126.9780,
                                        5000,
                                        "API-CONCERT-PLACE-1"
                                )
                        )
                );
        Concert concert = concertRepository.getConcertByApiConcertId("test-concert-1");

        // when
        PlaceDetailResponse placeDetailResponse = concertService.getConcertPlaceDetail(concert.getConcertId());

        // then
        assertThat(placeDetailResponse).isNotNull();
        assertThat(placeDetailResponse.getPlaceName()).isEqualTo(place.getPlaceName());
        assertThat(placeDetailResponse.getPlaceAddress()).isEqualTo(place.getAddress());
        assertThat(placeDetailResponse.getLat()).isEqualTo(place.getLat());
        assertThat(placeDetailResponse.getLon()).isEqualTo(place.getLon());
    }


}
