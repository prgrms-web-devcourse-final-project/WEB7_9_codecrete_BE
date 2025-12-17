package com.back.web7_9_codecrete_be.global.initData;

import com.back.web7_9_codecrete_be.domain.users.entity.SocialType;
import com.back.web7_9_codecrete_be.domain.users.entity.User;
import com.back.web7_9_codecrete_be.domain.users.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Profile("dev")
@Component
@RequiredArgsConstructor
public class BaseInitData {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @PostConstruct
    public void init() {
        createTestUser();
        createEmailTestUser();
        createAdminUser();
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

    private void createEmailTestUser() {
        if (userRepository.existsByEmail("gnldbs1004@naver.com")) {
            return;
        }

        User testEmailUser = User.builder()
                .email("gnldbs1004@naver.com")
                .nickname("이메일개발자")
                .password(passwordEncoder.encode("test1234!"))
                .birth(LocalDate.of(1999, 1, 1))
                .profileImage("https://example.com/profile.jpg")
                .socialType(SocialType.LOCAL)
                .socialId(null)
                .build();

        userRepository.save(testEmailUser);
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
}
