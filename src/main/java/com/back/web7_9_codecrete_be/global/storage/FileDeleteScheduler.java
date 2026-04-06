package com.back.web7_9_codecrete_be.global.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class FileDeleteScheduler {

    private final FileDeleteQueueRepository repository;
    private final S3Client s3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.aws.region.static}")
    private String region;

    @Scheduled(fixedDelay = 600000) // 10분마다 실행
    public void deleteFiles() {

        List<FileDeleteQueue> targets =
                repository.findAllByDeleteAtBefore(LocalDateTime.now());

        for (FileDeleteQueue file : targets) {
            try {
                String key = extractKey(file.getFileUrl());

                s3Client.deleteObject(builder -> builder
                        .bucket(bucket)
                        .key(key)
                );

            } catch (Exception e) {
                // 실패해도 계속 진행
            }
        }

        repository.deleteAll(targets);
    }

    private String extractKey(String fileUrl) {
        String prefix = "https://" + bucket + ".s3." + region + ".amazonaws.com/";
        return fileUrl.substring(prefix.length());
    }
}
