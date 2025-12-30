package com.back.web7_9_codecrete_be.domain.users.service;

import com.back.web7_9_codecrete_be.domain.auth.service.TokenService;
import com.back.web7_9_codecrete_be.domain.chats.service.ChatUserCacheService;
import com.back.web7_9_codecrete_be.domain.users.dto.request.UserUpdateNicknameRequest;
import com.back.web7_9_codecrete_be.domain.users.dto.request.UserUpdatePasswordRequest;
import com.back.web7_9_codecrete_be.domain.users.dto.response.UserResponse;
import com.back.web7_9_codecrete_be.domain.users.entity.User;
import com.back.web7_9_codecrete_be.domain.users.repository.UserRepository;
import com.back.web7_9_codecrete_be.global.error.code.FileErrorCode;
import com.back.web7_9_codecrete_be.global.error.code.UserErrorCode;
import com.back.web7_9_codecrete_be.global.error.exception.BusinessException;
import com.back.web7_9_codecrete_be.global.storage.FileStorageService;
import com.back.web7_9_codecrete_be.global.storage.ImageFileValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private TokenService tokenService;
    @Mock private FileStorageService fileStorageService;
    @Mock private ImageFileValidator imageFileValidator;
    @Mock private ChatUserCacheService chatUserCacheService;

    private User user;

    // 테스트 유저 주입
    @BeforeEach
    void setUp() {
        user = User.builder()
                .email("test@example.com")
                .nickname("oldNick")
                .password("encodedPassword")
                .build();
    }

    @Test
    @DisplayName("내 정보 조회 성공")
    void getMyInfo_success() {
        UserResponse response = userService.getMyInfo(user);

        assertThat(response.getEmail()).isEqualTo("test@example.com");
        assertThat(response.getNickname()).isEqualTo("oldNick");
    }

    @Test
    @DisplayName("내 정보 조회 실패 - 삭제된 유저")
    void getMyInfo_fail_deletedUser() {
        user.softDelete();

        assertThatThrownBy(() ->
                userService.getMyInfo(user))
                .isInstanceOf(BusinessException.class)
                .hasMessage(UserErrorCode.USER_DELETED.getMessage());
    }

    @Test
    @DisplayName("닉네임 수정 성공")
    void updateNickname_success() {
        UserUpdateNicknameRequest req =
                new UserUpdateNicknameRequest("newNick");

        given(userRepository.existsByNickname("newNick"))
                .willReturn(false);

        UserResponse response = userService.updateNickname(user, req);

        assertThat(response.getNickname()).isEqualTo("newNick");
        verify(userRepository).save(user);
        verify(chatUserCacheService).removeChatUserCache(user.getEmail());
    }

    @Test
    @DisplayName("닉네임 수정 실패 - 중복 닉네임")
    void updateNickname_fail_duplicated() {
        UserUpdateNicknameRequest req =
                new UserUpdateNicknameRequest("dupNick");

        given(userRepository.existsByNickname("dupNick"))
                .willReturn(true);

        assertThatThrownBy(() ->
                userService.updateNickname(user, req))
                .isInstanceOf(BusinessException.class)
                .hasMessage(UserErrorCode.NICKNAME_DUPLICATED.getMessage());
    }

    @Test
    @DisplayName("비밀번호 변경 성공")
    void updatePassword_success() {
        UserUpdatePasswordRequest req = new UserUpdatePasswordRequest();
        ReflectionTestUtils.setField(req, "currentPassword", "OldPassword1!");
        ReflectionTestUtils.setField(req, "newPassword", "NewPassword1!");

        given(passwordEncoder.matches("OldPassword1!", "encodedPassword"))
                .willReturn(true);
        given(passwordEncoder.encode("NewPassword1!"))
                .willReturn("newEncodedPw");

        userService.updatePassword(user, req);

        assertThat(user.getPassword()).isEqualTo("newEncodedPw");
        verify(userRepository).save(user);
        verify(tokenService).removeTokens(user);
    }

    @Test
    @DisplayName("비밀번호 변경 실패 - 현재 비밀번호 불일치")
    void updatePassword_fail_invalidPassword() {
        UserUpdatePasswordRequest req = new UserUpdatePasswordRequest();
        ReflectionTestUtils.setField(req, "currentPassword", "WrongPassword1!");
        ReflectionTestUtils.setField(req, "newPassword", "NewPassword1!");

        given(passwordEncoder.matches(any(), any()))
                .willReturn(false);

        assertThatThrownBy(() ->
                userService.updatePassword(user, req))
                .isInstanceOf(BusinessException.class)
                .hasMessage(UserErrorCode.INVALID_PASSWORD.getMessage());

        verify(userRepository, never()).save(any());
        verify(tokenService, never()).removeTokens(any());
    }

    @Test
    @DisplayName("회원 탈퇴 성공")
    void deleteMyAccount_success() {
        userService.deleteMyAccount(user);

        assertThat(user.getIsDeleted()).isTrue();
        verify(userRepository).save(user);
        verify(tokenService).removeTokens(user);
    }

    @Test
    @DisplayName("프로필 이미지 수정 성공")
    void updateProfileImage_success() {
        MultipartFile file = mock(MultipartFile.class);

        given(fileStorageService.upload(file, "users/profile"))
                .willReturn("new-image-url");

        String result = userService.updateProfileImage(user, file);

        assertThat(result).isEqualTo("new-image-url");
        assertThat(user.getProfileImage()).isEqualTo("new-image-url");
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("프로필 이미지 수정 실패 - 이미지 검증 실패")
    void updateProfileImage_fail_invalidImage() {
        MultipartFile file = mock(MultipartFile.class);

        doThrow(new BusinessException(FileErrorCode.INVALID_IMAGE_TYPE))
                .when(imageFileValidator)
                .validateImageFile(file);

        assertThatThrownBy(() ->
                userService.updateProfileImage(user, file))
                .isInstanceOf(BusinessException.class)
                .hasMessage(FileErrorCode.INVALID_IMAGE_TYPE.getMessage());

        verify(fileStorageService, never()).upload(any(), any());
    }
}
