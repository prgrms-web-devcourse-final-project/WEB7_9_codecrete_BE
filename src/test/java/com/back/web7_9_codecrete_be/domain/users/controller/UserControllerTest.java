package com.back.web7_9_codecrete_be.domain.users.controller;

import com.back.web7_9_codecrete_be.domain.auth.service.TokenService;
import com.back.web7_9_codecrete_be.domain.users.dto.response.UserResponse;
import com.back.web7_9_codecrete_be.domain.users.entity.User;
import com.back.web7_9_codecrete_be.domain.users.service.UserService;
import com.back.web7_9_codecrete_be.global.error.code.UserErrorCode;
import com.back.web7_9_codecrete_be.global.error.exception.BusinessException;
import com.back.web7_9_codecrete_be.global.rq.Rq;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private TokenService tokenService;

    @MockBean
    private Rq rq;

    private User mockUser;
    private UserResponse mockUserResponse;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .email("test@example.com")
                .nickname("testUser")
                .build();

        mockUserResponse = UserResponse.builder()
                .email("test@example.com")
                .nickname("testUser")
                .profileImageUrl("https://example.com/profile.jpg")
                .build();

        // Rq가 사용될 때 가짜 유저 반환 설정
        lenient().when(rq.getUser()).thenReturn(mockUser);
    }

    // 성공 테스트 시나리오

    @Test
    @WithMockUser
    @DisplayName("내 정보 조회 성공")
    void getMyInfo_Success() throws Exception {
        given(userService.getMyInfo(any())).willReturn(mockUserResponse);

        mockMvc.perform(get("/api/v1/users/me"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("OK"))
                .andExpect(jsonPath("$.msg").value("사용자 정보 조회 성공"))
                .andExpect(jsonPath("$.data.email").value("test@example.com"));
    }

    @Test
    @WithMockUser
    @DisplayName("닉네임 수정 성공")
    void updateNickname_Success() throws Exception {
        String jsonRequest = "{\"nickname\": \"newNickname\"}";
        UserResponse updatedResponse = UserResponse.builder().nickname("newNickname").build();

        given(userService.updateNickname(any(), any())).willReturn(updatedResponse);

        mockMvc.perform(patch("/api/v1/users/nickname")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("OK"))
                .andExpect(jsonPath("$.data.nickname").value("newNickname"));
    }

    @Test
    @WithMockUser
    @DisplayName("프로필 이미지 수정 성공")
    void updateProfileImage_Success() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "profile.jpg", MediaType.IMAGE_JPEG_VALUE, "content".getBytes());

        given(userService.updateProfileImage(any(), any())).willReturn("https://new-url.com");

        mockMvc.perform(multipart("/api/v1/users/profile-image")
                        .file(file)
                        .with(csrf())
                        .with(request -> {
                            request.setMethod("PATCH");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.msg").value("프로필 이미지가 변경되었습니다."));
    }

    @Test
    @WithMockUser
    @DisplayName("비밀번호 변경 성공")
    void updatePassword_Success() throws Exception {

        String jsonRequest = """
                
                {
                
                "currentPassword": "oldPassword1!",
                
                "newPassword": "newPassword1!"
                
                }
                
                """;

        mockMvc.perform(patch("/api/v1/users/password")

                        .with(csrf())

                        .contentType(MediaType.APPLICATION_JSON)

                        .content(jsonRequest))

                .andExpect(status().isOk());

        verify(userService).updatePassword(any(), any());

    }

    @Test
    @WithMockUser
    @DisplayName("회원 탈퇴 성공")
    void deleteMyAccount_Success() throws Exception {
        mockMvc.perform(delete("/api/v1/users/me")
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("OK"))
                .andExpect(jsonPath("$.msg").value("정상적으로 처리되었습니다."))
                .andExpect(jsonPath("$.data").value("회원 탈퇴가 완료되었습니다."));
    }

    // 실패 테스트 시나리오

    @Test
    @WithMockUser
    @DisplayName("닉네임 수정 실패 - 중복된 닉네임 (U-100)")
    void updateNickname_Fail_Duplicated() throws Exception {
        doThrow(new BusinessException(UserErrorCode.NICKNAME_DUPLICATED))
                .when(userService).updateNickname(any(), any());

        String jsonRequest = "{\"nickname\": \"existingNick\"}";

        mockMvc.perform(patch("/api/v1/users/nickname")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andDo(print())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.resultCode").value("U-100"))
                .andExpect(jsonPath("$.msg").value("이미 사용 중인 닉네임입니다."));
    }

    @Test
    @WithMockUser
    @DisplayName("비밀번호 변경 실패 - 현재 비밀번호 불일치 (U-110)")
    void updatePassword_Fail_InvalidPassword() throws Exception {
        String jsonRequest = """
                {
                    "currentPassword": "wrongPassword1!",
                    "newPassword": "newPassword123!"
                }
                """;

        doThrow(new BusinessException(UserErrorCode.INVALID_PASSWORD))
                .when(userService).updatePassword(any(), any());

        mockMvc.perform(patch("/api/v1/users/password")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("U-110"));
    }

    @Test
    @WithMockUser
    @DisplayName("비밀번호 변경 실패 - 유효성 검사 에러 (VALIDATION_ERROR)")
    void updatePassword_Fail_ValidationError() throws Exception {
        String jsonRequest = "{\"currentPassword\": \"short\", \"newPassword\": \"short\"}";

        mockMvc.perform(patch("/api/v1/users/password")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("VALIDATION_ERROR"));
    }
}