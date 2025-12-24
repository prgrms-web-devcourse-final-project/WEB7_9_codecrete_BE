package com.back.web7_9_codecrete_be.global.initData;

import com.back.web7_9_codecrete_be.domain.concerts.service.ConcertService;
import com.back.web7_9_codecrete_be.domain.concerts.service.KopisApiService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;

@Profile("prod")
@RequiredArgsConstructor
public class ProdInitData {
    private final KopisApiService kopisApiService;
    private final ConcertService concertService;

    @PostConstruct
    public void init(){

    }

    private void getConcertDataFromKopisApi() throws InterruptedException {
        kopisApiService.setConcertsData();
    }

    private void setConcertAutoCompletion(){
        concertService.setAutoComplete();
    }

}
