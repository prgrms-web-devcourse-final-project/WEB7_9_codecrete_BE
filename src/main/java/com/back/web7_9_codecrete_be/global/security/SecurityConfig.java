package com.back.web7_9_codecrete_be.global.security;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.back.web7_9_codecrete_be.domain.auth.service.TokenService;

import lombok.RequiredArgsConstructor;

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

			// 세션 관리 설정 - Stateless
			.sessionManagement((session) -> session
				.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

			// Authorization 설정
			.authorizeHttpRequests(auth -> auth
				.requestMatchers(
					"/actuator/**",
					"/api/v1/auth/**",      // 로그인/회원가입은 허용
					"/v3/api-docs/**",       // Swagger
					"/swagger-ui/**",         // Swagger UI
					"/h2-console/**",       // H2 Console
					"/api/v1/location/**",      //location 정보 조회 도메인(임시)
					"/api/v1/concerts/**",     // concert 정보 조회 도메인
					"/api/v1/artists/**",    // artist 정보 저장 도메인(임시)
          			"/api/v1/users/**"
				).permitAll()

				// ADMIN 전용
				.requestMatchers("/api/v1/admin/**").hasRole("ADMIN")

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

	// CORS 설정(로컬 프론트 통신 허용)
	@Bean
	public UrlBasedCorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();

		configuration.setAllowedOrigins(List.of("http://localhost:3000", "https://web-6-7-codecrete-fe.vercel.app", "https://www.naeconcertbutakhae.shop"));
		configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));

		configuration.setAllowedHeaders(List.of("*"));

		//쿠키 자동으로 넘어가게 설정
		configuration.setAllowCredentials(true);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/api/**", configuration);

		return source;
	}
}
