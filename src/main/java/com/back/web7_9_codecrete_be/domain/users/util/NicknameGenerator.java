package com.back.web7_9_codecrete_be.domain.users.util;

import com.back.web7_9_codecrete_be.domain.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.List;

@Component
@RequiredArgsConstructor
public class NicknameGenerator {

    private final UserRepository userRepository;
    private final SecureRandom random = new SecureRandom();

    // 접두어 후보들
    private static final List<String> PREFIXES = List.of(
            "공연덕후",
            "덕질러",
            "콘덕",
            "라이브덕후",
            "공연러"
    );

    public String generate() {
        while (true) {
            String prefix = PREFIXES.get(random.nextInt(PREFIXES.size()));
            String nickname = prefix + "_" + randomNumber6Digits();

            if (!userRepository.existsByNickname(nickname)) {
                return nickname;
            }
        }
    }

    private long randomNumber6Digits() {
        // 100000 ~ 999999
        return 100_000L + random.nextInt(900_000);
    }
}
