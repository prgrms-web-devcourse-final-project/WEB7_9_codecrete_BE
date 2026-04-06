package com.back.web7_9_codecrete_be.global.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3FileStorageService implements FileStorageService {

    private final S3Client s3Client;
    private final ImageFileValidator imageFileValidator;
    private final FileDeleteQueueRepository fileDeleteQueueRepository;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.aws.region.static}")
    private String region;

    // 공용 업로드 메서드
    @Override
    public String upload(MultipartFile file, String basePath) {

        imageFileValidator.validateImageFile(file);

        String originalFilename = file.getOriginalFilename();

        // 확장자 추출
        String extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();

        // 파일명 제거 (UUID만 사용)
        String fileName = UUID.randomUUID() + extension;

        String key = basePath + "/" + fileName;

        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(
                    putObjectRequest,
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize())
            );

        } catch (IOException e) {
            throw new RuntimeException("S3 파일 업로드 실패", e);
        }

        return "https://" + bucket + ".s3." + region + ".amazonaws.com/" + key;
    }

    @Override
    public void delete(String fileUrl) {

        if (fileUrl == null || fileUrl.isBlank()) {
            return;
        }

        fileDeleteQueueRepository.save(
                FileDeleteQueue.builder()
                        .fileUrl(fileUrl)
                        .deleteAt(LocalDateTime.now().plusMinutes(30))
                        .build()
        );
    }
}
