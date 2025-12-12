package com.back.web7_9_codecrete_be.domain.auth.service;

import com.back.web7_9_codecrete_be.domain.auth.dto.request.LoginRequest;
import com.back.web7_9_codecrete_be.domain.auth.dto.request.SignupRequest;
import com.back.web7_9_codecrete_be.domain.auth.dto.response.LoginResponse;
import com.back.web7_9_codecrete_be.domain.users.entity.User;
import com.back.web7_9_codecrete_be.domain.users.repository.UserRepository;
import com.back.web7_9_codecrete_be.global.error.code.AuthErrorCode;
import com.back.web7_9_codecrete_be.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // 회원가입
    public void signUp(SignupRequest req) {

        if (userRepository.existsByEmail(req.getEmail())) {
            throw new BusinessException(AuthErrorCode.EMAIL_DUPLICATED);
        }

        User user = User.builder()
                .email(req.getEmail())
                .nickname(req.getNickname())
                .password(passwordEncoder.encode(req.getPassword()))
                .birth(LocalDate.parse(req.getBirth()))
                .profileImage(req.getProfileImage())
                .build();

        userRepository.save(user);
    }

    // 로그인
    public LoginResponse login(LoginRequest req) {
        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new BusinessException(AuthErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new BusinessException(AuthErrorCode.INVALID_PASSWORD);
        }

        return new LoginResponse(user.getId(), user.getNickname());
    }

    // 로그아웃
    public void logout() {
        // JWT 적용 후 → 블랙리스트 처리 or 쿠키 삭제 등
    }
}
