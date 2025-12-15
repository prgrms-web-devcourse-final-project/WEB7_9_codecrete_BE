package com.back.web7_9_codecrete_be.domain.concerts.service;

import com.back.web7_9_codecrete_be.domain.concerts.repository.ConcertPlaceRepository;
import com.back.web7_9_codecrete_be.domain.concerts.repository.ConcertRepository;
import com.back.web7_9_codecrete_be.domain.concerts.repository.TicketOfficeRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;

@Service
public class interparkApiService {
    private final RestClient restClient;

    public interparkApiService(ConcertRepository concertRepository, ConcertPlaceRepository placeRepository, TicketOfficeRepository ticketOfficeRepository) {
        this.restClient = RestClient.builder()
                .baseUrl("https://api-ticketfront.interpark.com/v1/goods/")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public LocalDateTime getTicketOpenDate(String goodsId){
        restClient.get()
                .uri(uriBuilder ->
                        uriBuilder.path("/"+goodsId)
                                .path("/summary")
                                .queryParam("goodsCode",goodsId)
                                .build()
                ).retrieve();

        return null;
    }


}
