package com.back.web7_9_codecrete_be.domain.auth.service;

import com.back.web7_9_codecrete_be.domain.auth.dto.google.GoogleUserInfo;
import com.back.web7_9_codecrete_be.domain.auth.dto.kakao.KakaoUserInfo;
import com.back.web7_9_codecrete_be.domain.auth.dto.request.LoginRequest;
import com.back.web7_9_codecrete_be.domain.auth.dto.request.SignupRequest;
import com.back.web7_9_codecrete_be.domain.auth.dto.response.LoginResponse;
import com.back.web7_9_codecrete_be.domain.email.service.EmailService;
import com.back.web7_9_codecrete_be.domain.users.entity.SocialType;
import com.back.web7_9_codecrete_be.domain.users.entity.User;
import com.back.web7_9_codecrete_be.domain.users.repository.UserRepository;
import com.back.web7_9_codecrete_be.domain.users.service.UserService;
import com.back.web7_9_codecrete_be.domain.users.util.NicknameGenerator;
import com.back.web7_9_codecrete_be.global.error.code.AuthErrorCode;
import com.back.web7_9_codecrete_be.global.error.code.UserErrorCode;
import com.back.web7_9_codecrete_be.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {
    private final UserRepository userRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final TokenService tokenService;
    private final KakaoOAuthService kakaoOAuthService;
    private final GoogleOAuthService googleOAuthService;
    private final NicknameGenerator nicknameGenerator;

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
            throw new BusinessException(UserErrorCode.NICKNAME_DUPLICATED);
        }

        userService.createLocalUser(req, passwordEncoder.encode(req.getPassword()));

        emailService.clearVerifiedEmail(req.getEmail());
    }

    // 로그인
    public LoginResponse login(LoginRequest req) {
        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new BusinessException(AuthErrorCode.USER_NOT_FOUND));

        if (user.getIsDeleted()) {
            throw new BusinessException(UserErrorCode.USER_DELETED);
        }

        if (user.getSocialType() != SocialType.LOCAL) {
            throw new BusinessException(AuthErrorCode.SOCIAL_USER_CANNOT_LOGIN);
        }

        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new BusinessException(AuthErrorCode.INVALID_PASSWORD);
        }
        tokenService.issueTokens(user);

        return new LoginResponse(user.getId(), user.getNickname());
    }

    // 이메일 인증코드 전송
    public void sendVerificationCode(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(AuthErrorCode.EMAIL_DUPLICATED);
        }
        emailService.createAndSendVerificationCode(email);
    }

    // 이메일 인증코드 검증
    public void verifyEmailCode(String email, String code) {
        emailService.verifyCode(email, code);
    }

    // 닉네임 중복 체크
    public void isNicknameAvailable(String nickname) {
        if (userRepository.existsByNickname(nickname)) {
            throw new BusinessException(UserErrorCode.NICKNAME_DUPLICATED);
        }
    }

    // 임시 비밀번호 재발급
    public void resetPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(AuthErrorCode.USER_NOT_FOUND));

        if (user.getIsDeleted()) {
            throw new BusinessException(UserErrorCode.USER_DELETED);
        }

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

    @Transactional
    public LoginResponse kakaoLogin(String code) {

        // 1. 인가 코드 → 카카오 Access Token
        String kakaoAccessToken = kakaoOAuthService.getAccessToken(code);

        // 2. Access Token → 사용자 정보
        KakaoUserInfo kakaoUserInfo = kakaoOAuthService.getUserInfo(kakaoAccessToken);

        if (kakaoUserInfo.getEmail() == null) {
            throw new BusinessException(AuthErrorCode.SOCIAL_EMAIL_NOT_PROVIDED);
        }

        // 3. 소셜 ID 기준 사용자 조회
        User user = userRepository
                .findBySocialTypeAndSocialId(
                        SocialType.KAKAO,
                        kakaoUserInfo.getSocialId()
                )
                .orElseGet(() -> registerKakaoUser(kakaoUserInfo));

        // 4. 탈퇴 사용자 체크
        if (user.getIsDeleted()) {
            throw new BusinessException(UserErrorCode.USER_DELETED);
        }

        // 5. 토큰 발급
        tokenService.issueTokens(user);

        return new LoginResponse(user.getId(), user.getNickname());
    }

    private User registerKakaoUser(KakaoUserInfo info) {

        String nickname = nicknameGenerator.generate();

        return userService.createSocialUser(
                info.getEmail(),
                nickname,
                info.getProfileImageUrl(),
                SocialType.KAKAO,
                info.getSocialId()
        );
    }

    @Transactional
    public LoginResponse googleLogin(String code) {

        // 1. 인가 코드 → 구글 Access Token
        String googleAccessToken = googleOAuthService.getAccessToken(code);

        // 2. Access Token → 사용자 정보
        GoogleUserInfo googleUserInfo = googleOAuthService.getUserInfo(googleAccessToken);

        if (googleUserInfo.getEmail() == null) {
            throw new BusinessException(AuthErrorCode.SOCIAL_EMAIL_NOT_PROVIDED);
        }

        // 3. 소셜 ID 기준 사용자 조회
        User user = userRepository
                .findBySocialTypeAndSocialId(
                        SocialType.GOOGLE,
                        googleUserInfo.getSocialId()
                )
                .orElseGet(() -> registerGoogleUser(googleUserInfo));

        // 4. 탈퇴 사용자 체크
        if (user.getIsDeleted()) {
            throw new BusinessException(UserErrorCode.USER_DELETED);
        }

        // 5. 토큰 발급
        tokenService.issueTokens(user);

        return new LoginResponse(user.getId(), user.getNickname());
    }

    private User registerGoogleUser(GoogleUserInfo info) {

        String nickname = nicknameGenerator.generate();

        return userService.createSocialUser(
                info.getEmail(),
                nickname,
                info.getProfileImageUrl(),
                SocialType.GOOGLE,
                info.getSocialId()
        );
    }
}
