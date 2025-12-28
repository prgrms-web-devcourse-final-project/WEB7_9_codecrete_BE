package com.back.web7_9_codecrete_be.domain.users.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.back.web7_9_codecrete_be.domain.auth.dto.request.SignupRequest;
import com.back.web7_9_codecrete_be.domain.auth.service.TokenService;
import com.back.web7_9_codecrete_be.domain.chats.service.ChatUserCacheService;
import com.back.web7_9_codecrete_be.domain.email.service.EmailService;
import com.back.web7_9_codecrete_be.domain.users.dto.request.UserSettingUpdateRequest;
import com.back.web7_9_codecrete_be.domain.users.dto.request.UserUpdateNicknameRequest;
import com.back.web7_9_codecrete_be.domain.users.dto.request.UserUpdatePasswordRequest;
import com.back.web7_9_codecrete_be.domain.users.dto.response.UserResponse;
import com.back.web7_9_codecrete_be.domain.users.dto.response.UserSettingResponse;
import com.back.web7_9_codecrete_be.domain.users.entity.SocialType;
import com.back.web7_9_codecrete_be.domain.users.entity.User;
import com.back.web7_9_codecrete_be.domain.users.entity.UserSetting;
import com.back.web7_9_codecrete_be.domain.users.repository.UserRepository;
import com.back.web7_9_codecrete_be.domain.users.repository.UserRestoreTokenRedisRepository;
import com.back.web7_9_codecrete_be.global.error.code.UserErrorCode;
import com.back.web7_9_codecrete_be.global.error.exception.BusinessException;
import com.back.web7_9_codecrete_be.global.storage.FileStorageService;
import com.back.web7_9_codecrete_be.global.storage.ImageFileValidator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserService {
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final UserRestoreTokenRedisRepository userRestoreTokenRedisRepository;
    private final EmailService emailService;
    private final ChatUserCacheService chatUserCacheService;

    private final ImageFileValidator imageFileValidator;

    // 내 정보 조회
    @Transactional(readOnly = true)
    public UserResponse getMyInfo(User user) {
        validateActiveUser(user);
        return UserResponse.from(user);
    }

    // 회원 가입(로컬)
    public User createLocalUser(SignupRequest req, String encodedPassword) {

        User user = User.builder()
                .email(req.getEmail())
                .nickname(req.getNickname())
                .password(encodedPassword)
                .birth(LocalDate.parse(req.getBirth()))
                .profileImage(req.getProfileImage())
                .socialType(SocialType.LOCAL)
                .socialId(null)
                .build();

        user.initSetting();

        userRepository.save(user);

        return user;
    }

    // 회원 가입(소셜)
    public User createSocialUser(
            String email,
            String nickname,
            String profileImage,
            SocialType socialType,
            String socialId
    ) {

        User user = User.builder()
                .email(email)
                .nickname(nickname)
                .password(null)
                .birth(null)
                .profileImage(profileImage)
                .socialType(socialType)
                .socialId(socialId)
                .build();

        user.initSetting();

        userRepository.save(user);

        return user;
    }

    // 닉네임 수정
    public UserResponse updateNickname(User user, UserUpdateNicknameRequest req) {
        validateActiveUser(user);
        String newNickname = req.getNickname();

        // 닉네임이 변경되는 경우에만 중복 검사
        if (!newNickname.equals(user.getNickname())) {
            if (userRepository.existsByNickname(newNickname)) {
                throw new BusinessException(UserErrorCode.NICKNAME_DUPLICATED);
            }
        }

        user.updateNickname(req.getNickname());
        userRepository.save(user);

        chatUserCacheService.removeChatUserCache(user.getEmail());

        return UserResponse.from(user);
    }

    // 회원 탈퇴
    public void deleteMyAccount(User user) {
        validateActiveUser(user);
        user.softDelete();
        userRepository.save(user);

        // 로그아웃 처리
        tokenService.removeTokens(user);
    }

    // 프로필 이미지 수정
    public String updateProfileImage(User user, MultipartFile file) {
        validateActiveUser(user);

        imageFileValidator.validateImageFile(file);

        // 기존 이미지 URL 보관
        String oldImageUrl = user.getProfileImage();

        // 새 이미지 업로드
        String newImageUrl = fileStorageService.upload(file, "users/profile");

        // DB 업데이트
        user.updateProfileImage(newImageUrl);
        userRepository.save(user);

        // 기존 이미지 즉시 삭제
        // TODO: 추후 지연 삭제 스케줄러로 리팩토링
        if (oldImageUrl != null) {
            try {
                fileStorageService.delete(oldImageUrl);
            } catch (Exception e) {
                log.warn(
                        "기존 프로필 이미지 삭제 실패 userId={}, url={}",
                        user.getId(),
                        oldImageUrl,
                        e
                );
            }
        }

        return newImageUrl;
    }


    // 활성 사용자 검증
    private void validateActiveUser(User user) {
        if (user.getIsDeleted()) {
            throw new BusinessException(UserErrorCode.USER_DELETED);
        }
    }

    // 비밀번호 변경
    public void updatePassword(User user, UserUpdatePasswordRequest req) {
        validateActiveUser(user);

        // 현재 비밀번호 검증
        if (!passwordEncoder.matches(req.getCurrentPassword(), user.getPassword())) {
            throw new BusinessException(UserErrorCode.INVALID_PASSWORD);
        }

        user.changePassword(passwordEncoder.encode(req.getNewPassword()));
        userRepository.save(user);

        // 비밀번호 변경 시 로그아웃 처리
        tokenService.removeTokens(user);
    }

    public void sendRestoreLink(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        if (!user.getIsDeleted()) {
            throw new BusinessException(UserErrorCode.USER_NOT_DELETED);
        }

        if (user.getDeletedDate()
                .isBefore(LocalDateTime.now().minusDays(30))) {
            throw new BusinessException(UserErrorCode.USER_RESTORE_EXPIRED);
        }

        String token = UUID.randomUUID().toString();
        userRestoreTokenRedisRepository.save(token, email);

        String restoreUrl = "https://frontend-domain.com/users/restore?token=" + token;

        // 이미 있는 메일 서비스 사용
        emailService.sendRestoreLink(email, restoreUrl);
    }

    public void restoreByToken(String token) {
        String email = userRestoreTokenRedisRepository.findEmailByToken(token);

        if (email == null) {
            throw new BusinessException(UserErrorCode.INVALID_RESTORE_TOKEN);
        }

        User user = userRepository.findByEmailAndIsDeletedTrue(email)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        if (user.getDeletedDate()
                .isBefore(LocalDateTime.now().minusDays(30))) {
            throw new BusinessException(UserErrorCode.USER_RESTORE_EXPIRED);
        }

        user.restore();
        userRestoreTokenRedisRepository.delete(token);
    }

    @Transactional(readOnly = true)
    public UserSettingResponse getMySettings(User user) {
        validateActiveUser(user);

        User managedUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        return UserSettingResponse.from(managedUser.getUserSetting());
    }

    public void updateMySettings(User user, UserSettingUpdateRequest req) {
        validateActiveUser(user);

        User managedUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        UserSetting setting = managedUser.getUserSetting();

        if (req.getEmailNotifications() != null) {
            setting.changeEmailNotifications(req.getEmailNotifications());
        }
        if (req.getDarkMode() != null) {
            setting.changeDarkMode(req.getDarkMode());
        }
    }
}
