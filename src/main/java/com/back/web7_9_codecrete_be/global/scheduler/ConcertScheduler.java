package com.back.web7_9_codecrete_be.global.scheduler;

import com.back.web7_9_codecrete_be.domain.concerts.controller.ConcertController;
import com.back.web7_9_codecrete_be.domain.concerts.repository.ConcertSearchRedisTemplate;
import com.back.web7_9_codecrete_be.domain.concerts.service.ConcertNotifyService;
import com.back.web7_9_codecrete_be.domain.concerts.service.ConcertService;
import com.back.web7_9_codecrete_be.domain.concerts.service.KopisApiService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ConcertScheduler {
    private final ConcertService concertService;
    private final KopisApiService kopisApiService;
    private final ConcertNotifyService concertNotifyService;

    // 공연 데이터 업데이트를 진행합니다.
    @Scheduled(cron = "0 0 2 * * MON")
    public void concertUpdateSchedule() throws InterruptedException {
        kopisApiService.updateConcertData();
    }

    // 공연 관련 정보를 갱신합니다.
    @Scheduled(cron = "0 10 3 * * *")
    public void concertDataUpdateSchedule() {
        concertService.viewCountUpdate();
    }

    // 이메일 알림을 전송합니다.
    @Scheduled(cron = "0 0 9 * * *")
    public void notificationSendSchedule() {
        concertNotifyService.sendTodayTicketingConcertsNotifyingEmail();
    }

}
