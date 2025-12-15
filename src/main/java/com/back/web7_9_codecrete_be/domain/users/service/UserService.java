package com.back.web7_9_codecrete_be.domain.users.service;


import com.back.web7_9_codecrete_be.domain.auth.service.TokenService;
import com.back.web7_9_codecrete_be.domain.users.dto.request.UserUpdateNicknameRequest;
import com.back.web7_9_codecrete_be.domain.users.dto.request.UserUpdatePasswordRequest;
import com.back.web7_9_codecrete_be.domain.users.dto.response.UserResponse;
import com.back.web7_9_codecrete_be.domain.users.entity.User;
import com.back.web7_9_codecrete_be.domain.users.repository.UserRepository;
import com.back.web7_9_codecrete_be.global.error.code.UserErrorCode;
import com.back.web7_9_codecrete_be.global.error.exception.BusinessException;
import com.back.web7_9_codecrete_be.global.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    // 내 정보 조회
    @Transactional(readOnly = true)
    public UserResponse getMyInfo(User user) {
        validateActiveUser(user);
        return UserResponse.from(user);
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

        // 파일 유효성 검사
        if (file == null || file.isEmpty()) {
            throw new BusinessException(UserErrorCode.INVALID_PROFILE_IMAGE);
        }

        String imageUrl = fileStorageService.upload(file);

        user.updateProfileImage(imageUrl);
        userRepository.save(user);

        return imageUrl;
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
}
