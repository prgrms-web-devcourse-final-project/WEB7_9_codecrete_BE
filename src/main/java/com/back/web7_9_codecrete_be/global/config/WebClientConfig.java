package com.back.web7_9_codecrete_be.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Collections;

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

    @Value("${kakao.base-url}")
    private String kakaoBaseUrl;

    @Value("${tmap.base-url}")
    private String tmapBaseUrl;

    @Value("${kakao.mobility.base-url}")
    private String kakaoMobilityUrl;

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
        RestTemplate restTemplate = new RestTemplate();
        // Wikidata API는 User-Agent 헤더를 필수로 요구
        ClientHttpRequestInterceptor userAgentInterceptor = (request, body, execution) -> {
            request.getHeaders().add("User-Agent", "CodecreteBE/1.0 (Educational Project; +https://github.com/your-repo)");
            return execution.execute(request, body);
        };
        restTemplate.setInterceptors(Collections.singletonList(userAgentInterceptor));
        return restTemplate;
    }


    @Bean
    public RestClient kakaoRestClient(){

        return RestClient.builder()
                .baseUrl(kakaoBaseUrl)
                .defaultHeader("Authorization", kakaomapApiKey)
                .build();

    }

    @Bean
    public RestClient kakaoMobilityClient(){
        return RestClient.builder()
                .baseUrl(kakaoMobilityUrl)
                .defaultHeader("Authorization", kakaomapApiKey)
                .build();

    }

    @Bean
    public RestClient tmapRestClient(){
        return RestClient.builder()
                .baseUrl(tmapBaseUrl)
                .defaultHeader("appKey", tmapApiKey)
                .build();
    }
}
