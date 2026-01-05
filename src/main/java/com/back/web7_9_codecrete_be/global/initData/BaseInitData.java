package com.back.web7_9_codecrete_be.global.initData;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.back.web7_9_codecrete_be.domain.concerts.entity.Concert;
import com.back.web7_9_codecrete_be.domain.concerts.entity.ConcertPlace;
import com.back.web7_9_codecrete_be.domain.concerts.repository.ConcertPlaceRepository;
import com.back.web7_9_codecrete_be.domain.concerts.repository.ConcertRepository;
import com.back.web7_9_codecrete_be.domain.users.entity.SocialType;
import com.back.web7_9_codecrete_be.domain.users.entity.User;
import com.back.web7_9_codecrete_be.domain.users.repository.UserRepository;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@Profile("dev")
@Component
@RequiredArgsConstructor
public class BaseInitData {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	private final ConcertRepository concertRepository;
	private final ConcertPlaceRepository concertPlaceRepository;

	@PostConstruct
	public void init() {
		createTestUser();
		createAdminUser();
		createConcertsForChatTest();
	}

	private void createTestUser() {
		if (userRepository.existsByEmail("test@test.com")) {
			return;
		}

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
	}

	private void createAdminUser() {
		if (userRepository.existsByEmail("admin@test.com")) {
			return;
		}

		User adminUser = User.builder()
			.email("admin@test.com")
			.nickname("어드민")
			.password(passwordEncoder.encode("admin1234!"))
			.birth(LocalDate.of(1990, 1, 1))
			.profileImage("https://example.com/profile.jpg")
			.socialType(SocialType.LOCAL)
			.socialId(null)
			.build();

		// dev 전용 어드민 권한 부여
		adminUser.promoteToAdmin();

		userRepository.save(adminUser);
	}

	/* =========================
	 * Concert / ConcertPlace Init (Chat Test)
	 * ========================= */
	private void createConcertsForChatTest() {
		if (concertRepository.count() > 0) {
			return;
		}

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

		// 채팅 가능 (정책 기간 중)
		concertRepository.save(
			new Concert(
				place,
				"채팅 가능 공연",
				"1/2~1/8 채팅 가능한 공연",
				LocalDate.now().plusMonths(1),
				LocalDate.now().plusMonths(1).plusDays(2),
				LocalDateTime.now().plusDays(3),
				LocalDateTime.now().plusDays(7),
				150000,
				50000,
				"https://example.com/poster1.jpg",
				"서울특별시",
				"API-CONCERT-CHAT-1"
			)
		);

		concertRepository.save(
			new Concert(
				place,
				"채팅 가능 공연",
				"1/9~1/15 채팅 가능한 공연",
				LocalDate.now().plusMonths(1),
				LocalDate.now().plusMonths(1).plusDays(2),
				LocalDateTime.now().plusDays(11),
				LocalDateTime.now().plusDays(18),
				180000,
				70000,
				"https://example.com/poster1.jpg",
				"서울특별시",
				"API-CONCERT-CHAT-2"
			)
		);

		// 채팅 불가 (정책 시작 전)
		concertRepository.save(
			new Concert(
				place,
				"채팅 불가 공연 - 시작 전",
				"아직 채팅이 오픈되지 않은 공연",
				LocalDate.now().plusMonths(2),
				LocalDate.now().plusMonths(2).plusDays(2),
				LocalDateTime.now().plusMonths(1),
				LocalDateTime.now().plusMonths(1).plusDays(7),
				120000,
				40000,
				"https://example.com/poster2.jpg",
				"서울특별시",
				"API-CONCERT-CHAT-3"
			)
		);

		// 채팅 불가 (정책 종료 후)
		concertRepository.save(
			new Concert(
				place,
				"채팅 종료된 공연",
				"채팅 가능 기간이 지난 공연",
				LocalDate.now().minusDays(10),
				LocalDate.now().minusDays(7),
				LocalDateTime.of(2025, 11, 1, 0, 0),
				LocalDateTime.of(2025, 11, 15, 0, 0),
				100000,
				30000,
				"https://example.com/poster3.jpg",
				"서울특별시",
				"API-CONCERT-CHAT-4"
			)
		);
	}
}
