package com.back.web7_9_codecrete_be.domain.auth.controller;

import com.back.web7_9_codecrete_be.domain.auth.dto.request.EmailSendRequest;
import com.back.web7_9_codecrete_be.domain.auth.dto.request.EmailVerifyRequest;
import com.back.web7_9_codecrete_be.domain.auth.dto.request.LoginRequest;
import com.back.web7_9_codecrete_be.domain.auth.dto.request.SignupRequest;
import com.back.web7_9_codecrete_be.domain.auth.dto.response.LoginResponse;
import com.back.web7_9_codecrete_be.domain.auth.service.AuthService;
import com.back.web7_9_codecrete_be.global.rsData.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "인증/인가 API")
public class AuthController {
    private final AuthService authService;

    @Operation(summary = "회원가입", description = "사용자 이메일, 비밀번호, 닉네임, 생년월일을 이용하여 회원가입을 진행합니다.")
    @PostMapping("/signup")
    public RsData<?> signUp(@RequestBody SignupRequest req) {
        authService.signUp(req);
        return RsData.success("회원가입이 완료되었습니다.");
    }

    @Operation(summary = "로그인", description = "이메일/비밀번호로 로그인합니다. 성공 시 사용자 닉네임을 반환합니다.")
    @PostMapping("/login")
    public RsData<?> login(@RequestBody LoginRequest req) {
        LoginResponse response = authService.login(req);
        return RsData.success("로그인 성공", response);
    }

    @Operation(summary = "로그아웃", description = "현재 로그인된 사용자 정보를 기반으로 로그아웃합니다.")
    @PostMapping("/logout")
    public RsData<?> logout() {
        authService.logout(); // 지금은 단순 성공 처리
        return RsData.success("로그아웃 되었습니다.");
    }

    @Operation(summary = "이메일 인증코드 전송", description = "입력된 이메일로 인증코드를 전송합니다.")
    @PostMapping("/email/send")
    public RsData<?> sendVerificationCode(@RequestBody EmailSendRequest req) {
        authService.sendVerificationCode(req.getEmail());
        return RsData.success("인증코드가 발송되었습니다.");
    }

    @Operation(summary = "이메일 인증코드 검증", description = "사용자가 입력한 인증코드가 맞는지 확인합니다.")
    @PostMapping("/email/verify")
    public RsData<?> verifyEmailCode(@RequestBody EmailVerifyRequest req) {
        authService.verifyEmailCode(req.getEmail(), req.getCode());
        return RsData.success("이메일 인증이 완료되었습니다.");
    }

    @Operation(summary = "닉네임 중복 체크", description = "닉네임이 사용 가능한지 확인합니다.")
    @GetMapping("/nickname/check")
    public RsData<?> checkNickname(@RequestParam String nickname) {
        boolean available = authService.isNicknameAvailable(nickname);
        return RsData.success("닉네임 사용 가능 여부 확인", available);
    }

    @Operation(summary = "임시 비밀번호 재발급", description = "특정 이메일로 임시 비밀번호를 발송합니다.")
    @PostMapping("/password/reset")
    public RsData<?> resetPassword(@RequestBody EmailSendRequest req) {
        authService.resetPassword(req.getEmail());
        return RsData.success("임시 비밀번호가 이메일로 발송되었습니다.");
    }
}
