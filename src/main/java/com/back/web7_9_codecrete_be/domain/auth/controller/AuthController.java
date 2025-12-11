package com.back.web7_9_codecrete_be.domain.auth.controller;

import com.back.web7_9_codecrete_be.domain.auth.dto.request.LoginRequest;
import com.back.web7_9_codecrete_be.domain.auth.dto.request.SignupRequest;
import com.back.web7_9_codecrete_be.domain.auth.dto.response.LoginResponse;
import com.back.web7_9_codecrete_be.domain.auth.service.AuthService;
import com.back.web7_9_codecrete_be.global.rsData.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
