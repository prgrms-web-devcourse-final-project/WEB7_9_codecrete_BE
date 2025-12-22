package com.back.web7_9_codecrete_be.domain.users.controller;

import com.back.web7_9_codecrete_be.domain.auth.service.TokenService;
import com.back.web7_9_codecrete_be.domain.users.dto.request.UserSettingUpdateRequest;
import com.back.web7_9_codecrete_be.domain.users.dto.request.UserUpdateNicknameRequest;
import com.back.web7_9_codecrete_be.domain.users.dto.request.UserUpdatePasswordRequest;
import com.back.web7_9_codecrete_be.domain.users.dto.response.UserResponse;
import com.back.web7_9_codecrete_be.domain.users.dto.response.UserSettingResponse;
import com.back.web7_9_codecrete_be.domain.users.entity.User;
import com.back.web7_9_codecrete_be.domain.users.service.UserService;
import com.back.web7_9_codecrete_be.global.rq.Rq;
import com.back.web7_9_codecrete_be.global.rsData.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User", description = "사용자 정보 API")
public class UserController {
    private final UserService userService;
    private final TokenService tokenService;
    private final Rq rq;

    @Operation(summary = "내 정보 조회", description = "현재 로그인된 사용자의 정보를 조회합니다.")
    @GetMapping("/me")
    public RsData<?> getMyInfo() {
        User user = rq.getUser();
        UserResponse response = userService.getMyInfo(user);
        return RsData.success("사용자 정보 조회 성공", response);
    }

    @Operation(summary = "내 닉네임 수정", description = "닉네임을 수정합니다.")
    @PatchMapping("/nickname")
    public RsData<?> updateNickname(
            @Valid @RequestBody UserUpdateNicknameRequest req
    ) {
        User user = rq.getUser();
        UserResponse response = userService.updateNickname(user, req);
        return RsData.success("사용자 닉네임 변경 완료", response);
    }

    @Operation(summary = "내 프로필 이미지 수정", description = "프로필 이미지를 multipart/form-data 형식으로 업로드하여 수정합니다.")
    @PatchMapping(value = "/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public RsData<?> updateProfileImage(
            @RequestPart("file") MultipartFile file
    ) {
        User user = rq.getUser();
        String imageUrl = userService.updateProfileImage(user, file);
        return RsData.success("프로필 이미지가 변경되었습니다.", imageUrl);
    }

    @Operation(summary = "비밀번호 변경", description = "현재 비밀번호를 확인한 후 새로운 비밀번호로 변경합니다.")
    @PatchMapping("/password")
    public RsData<?> updatePassword(
            @Valid @RequestBody UserUpdatePasswordRequest req
    ) {
        User user = rq.getUser();
        userService.updatePassword(user, req);
        return RsData.success("비밀번호가 변경되었습니다.");
    }

    @Operation(summary = "회원 탈퇴", description = "현재 로그인된 사용자를 탈퇴 처리합니다.")
    @DeleteMapping("/me")
    public RsData<?> deleteMyAccount() {
        User user = rq.getUser();

        userService.deleteMyAccount(user);
        tokenService.removeTokens(user); // refresh/access 토큰 정리

        return RsData.success("회원 탈퇴가 완료되었습니다.");
    }

    @Operation(summary = "계정 복구 링크 발송", description = "이메일로 계정 복구 링크를 발송합니다.")
    @PostMapping("/restore/request")
    public RsData<?> requestRestore(@RequestParam String email) {
        userService.sendRestoreLink(email);
        return RsData.success("계정 복구 링크가 이메일로 발송되었습니다.");
    }

    @Operation(summary = "계정 복구 (복구 링크)", description = "이메일로 받은 복구 링크를 통해 계정을 복구합니다.")
    @GetMapping("/restore/confirm")
    public RsData<?> restoreByToken(@RequestParam String token) {
        userService.restoreByToken(token);
        return RsData.success("계정이 성공적으로 복구되었습니다.");
    }

    @Operation(summary = "유저 설정 조회", description = "로그인한 사용자의 설정 정보를 조회합니다.")
    @GetMapping("/settings")
    public RsData<?> getMySettings() {
        User user = rq.getUser();
        UserSettingResponse response = userService.getMySettings(user);
        return RsData.success("유저 설정 조회 성공", response);
    }

    @Operation(summary = "유저 설정 수정", description = "로그인한 사용자의 설정 정보를 부분 수정합니다.")
    @PatchMapping("/settings")
    public RsData<?> updateMySettings(
            @RequestBody UserSettingUpdateRequest req
    ) {
        User user = rq.getUser();
        userService.updateMySettings(user, req);
        return RsData.success("유저 설정 수정 성공");
    }
}
