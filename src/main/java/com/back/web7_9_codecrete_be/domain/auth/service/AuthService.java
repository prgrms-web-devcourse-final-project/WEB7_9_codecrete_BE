package com.back.web7_9_codecrete_be.domain.auth.service;

import com.back.web7_9_codecrete_be.domain.auth.dto.request.LoginRequest;
import com.back.web7_9_codecrete_be.domain.auth.dto.request.SignupRequest;
import com.back.web7_9_codecrete_be.domain.auth.dto.response.LoginResponse;
import com.back.web7_9_codecrete_be.domain.email.service.EmailService;
import com.back.web7_9_codecrete_be.domain.users.entity.User;
import com.back.web7_9_codecrete_be.domain.users.repository.UserRepository;
import com.back.web7_9_codecrete_be.global.error.code.AuthErrorCode;
import com.back.web7_9_codecrete_be.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    // 회원가입
    public void signUp(SignupRequest req) {

        // 이메일 중복 체크
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new BusinessException(AuthErrorCode.EMAIL_DUPLICATED);
        }

        // 이메일 인증 여부 확인
        if (!emailService.isVerified(req.getEmail())) {
            throw new BusinessException(AuthErrorCode.EMAIL_NOT_VERIFIED);
        }

        // 닉네임 중복 체크
        if (userRepository.existsByNickname(req.getNickname())) {
            throw new BusinessException(AuthErrorCode.NICKNAME_DUPLICATED);
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

    // 이메일 인증코드 전송
    public void sendVerificationCode(String email) {
        emailService.createAndSendVerificationCode(email);
    }

    // 이메일 인증코드 검증
    public void verifyEmailCode(String email, String code) {
        emailService.verifyCode(email, code);
    }

    // 닉네임 중복 체크
    public boolean isNicknameAvailable(String nickname) {
        return !userRepository.existsByNickname(nickname);
    }

    // 임시 비밀번호 재발급
    public void resetPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(AuthErrorCode.USER_NOT_FOUND));

        String tempPassword = generateTempPassword();

        // 비밀번호 변경
        user.changePassword(passwordEncoder.encode(tempPassword));
        userRepository.save(user);

        // 이메일 발송
        emailService.sendNewPassword(email, tempPassword);
    }

    // 임시 비밀번호 생성
    private String generateTempPassword() {
        SecureRandom random = new SecureRandom();
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < 10; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
