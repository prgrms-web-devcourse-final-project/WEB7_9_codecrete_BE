package com.back.web7_9_codecrete_be.global.scheduler;

import com.back.web7_9_codecrete_be.domain.users.service.UserCleanupService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class UserCleanupScheduler {

    private final UserCleanupService userCleanupService;

    // 매일 새벽 3시 실행
    @Scheduled(cron = "0 0 3 * * ?")
    public void run() {
        userCleanupService.cleanup(
                LocalDateTime.now().minusDays(30)
        );
    }
}
