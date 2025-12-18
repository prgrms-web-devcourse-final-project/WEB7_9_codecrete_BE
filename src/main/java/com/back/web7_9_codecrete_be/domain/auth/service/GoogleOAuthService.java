package com.back.web7_9_codecrete_be.domain.auth.service;

import com.back.web7_9_codecrete_be.domain.auth.dto.google.GoogleTokenResponse;
import com.back.web7_9_codecrete_be.domain.auth.dto.google.GoogleUserInfo;
import com.back.web7_9_codecrete_be.domain.auth.dto.google.GoogleUserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class GoogleOAuthService {

    @Value("${oauth.google.client-id}")
    private String clientId;

    @Value("${oauth.google.client-secret}")
    private String clientSecret;

    @Value("${oauth.google.redirect-uri}")
    private String redirectUri;

    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String USER_INFO_URL = "https://www.googleapis.com/oauth2/v2/userinfo";

    private final RestTemplate restTemplate = new RestTemplate();

    // 인가 코드 → 액세스 토큰
    public String getAccessToken(String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("redirect_uri", redirectUri);
        params.add("code", code);

        HttpEntity<MultiValueMap<String, String>> request =
                new HttpEntity<>(params, headers);

        ResponseEntity<GoogleTokenResponse> response =
                restTemplate.postForEntity(
                        TOKEN_URL,
                        request,
                        GoogleTokenResponse.class
                );

        return response.getBody().getAccessToken();
    }

    // 액세스 토큰 → 구글 사용자 정보
    public GoogleUserInfo getUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<GoogleUserResponse> response =
                restTemplate.exchange(
                        USER_INFO_URL,
                        HttpMethod.GET,
                        request,
                        GoogleUserResponse.class
                );

        return response.getBody().toUserInfo();
    }
}
