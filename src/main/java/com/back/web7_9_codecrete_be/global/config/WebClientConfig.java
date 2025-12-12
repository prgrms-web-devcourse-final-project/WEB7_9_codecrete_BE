package com.back.web7_9_codecrete_be.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Configuration
public class WebClientConfig {

    @Value("${mailgun.api-key}")
    private String mailgunApiKey;

    @Value("${mailgun.domain}")
    private String mailgunDomain;

    @Value("${tmap.api-key}")
    private String tmapApiKey;

    @Value("${kakao.restapi-key}")
    private String kakaomapApiKey;

    @Bean
    public WebClient mailgunClient() {
        String auth = "api:" + mailgunApiKey;
        String encoded = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
        String authHeader = "Basic " + encoded;

        return WebClient.builder()
                .baseUrl("https://api.mailgun.net/v3/" + mailgunDomain)
                .clientConnector(new ReactorClientHttpConnector())
                .defaultHeader(HttpHeaders.AUTHORIZATION, authHeader)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .build();
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public WebClient kakaoWebClient() {
        return WebClient.builder()
                .baseUrl("https://dapi.kakao.com")
                .defaultHeader("Authorization", kakaomapApiKey)
                .build();
    }


    @Bean
    public WebClient TmapClient(){
        return WebClient.builder()
                .baseUrl("https://apis.openapi.sk.com")
                .defaultHeader("appKey", tmapApiKey)
                .build();
    }
}
