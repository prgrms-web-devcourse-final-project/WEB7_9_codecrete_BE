package com.back.web7_9_codecrete_be.global.security;

import com.back.web7_9_codecrete_be.domain.auth.service.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final CustomUserDetailService customUserDetailService;
    private final TokenService tokenService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())

                // 기본 로그인 폼 비활성화
                .formLogin(form -> form.disable())

                // HTTP Basic 인증 비활성화
                .httpBasic(basic -> basic.disable())

                // H2 Console 설정
                .headers(headers -> headers.frameOptions(frame -> frame.disable()))

                // Authorization 설정
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/v1/auth/**",      // 로그인/회원가입은 허용
                                "/v3/api-docs/**",       // Swagger
                                "/swagger-ui/**",         // Swagger UI
                                "/h2-console/**"        // H2 Console
                        ).permitAll()
                        .anyRequest().authenticated()
                )

                .addFilterBefore(
                        new JwtAuthenticationFilter(jwtTokenProvider, jwtProperties, tokenService),
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
}
