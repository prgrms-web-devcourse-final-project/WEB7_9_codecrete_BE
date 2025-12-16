package com.back.web7_9_codecrete_be.domain.email.service;

import com.back.web7_9_codecrete_be.domain.email.repository.VerificationCodeRedisRepository;
import com.back.web7_9_codecrete_be.domain.email.repository.VerifiedEmailRedisRepository;
import com.back.web7_9_codecrete_be.global.error.code.MailErrorCode;
import com.back.web7_9_codecrete_be.global.error.exception.BusinessException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.security.SecureRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final VerificationCodeRedisRepository verificationCodeRedisRepository;
    private final VerifiedEmailRedisRepository verifiedEmailRedisRepository;
    private final WebClient mailgunClient;

    @Value("${mailgun.from}")
    private String fromEmail;

    // 임시 복구 링크 기본 URL
    // TODO: 프론트 도메인 확정 시 application.yml로 분리 예정
    private String restoreBaseUrl = "https://example.com/users/restore";

    private static final String CHAR_SET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int CODE_LENGTH = 6;
    private static final int TTL_SECONDS = 300;

    // 메일 전송 공통 메서드
    private void sendEmail(String toEmail, String subject, String content) {
        try {
            LinkedMultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("from", fromEmail);
            form.add("to", toEmail);
            form.add("subject", subject);
            form.add("text", content);

            String response = mailgunClient.post()
                    .uri("/messages")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData(form))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.info("메일 전송 완료: {} | 응답={}", toEmail, response);

        } catch (Exception e) {
            log.error("메일 전송 실패: {}", e.getMessage());
            throw new BusinessException(MailErrorCode.MAIL_SEND_FAILURE);
        }
    }

    // 인증코드 이메일 전송
    @Transactional
    public void createAndSendVerificationCode(String email) {
        String code = generateVerificationCode();

        // 기존 코드 있으면 삭제
        verificationCodeRedisRepository.deleteByEmail(email);

        // Redis 저장 (TTL 5분)
        verificationCodeRedisRepository.save(email, code, TTL_SECONDS);

        String content = """
                안녕하세요. NCB 입니다.

                아래 인증 코드를 입력해 주세요.

                인증코드: %s
                (유효시간 5분)
                """.formatted(code);

        sendEmail(email, "[NCB] 이메일 인증 코드", content);
    }

    // 인증코드 생성
    private String generateVerificationCode() {
        SecureRandom random = new SecureRandom();
        StringBuilder builder = new StringBuilder(CODE_LENGTH);

        for (int i = 0; i < CODE_LENGTH; i++) {
            builder.append(CHAR_SET.charAt(random.nextInt(CHAR_SET.length())));
        }
        return builder.toString();
    }

    // 인증코드 검증
    @Transactional
    public void verifyCode(String email, String inputCode) {
        String savedCode = verificationCodeRedisRepository.findByEmail(email);

        if (savedCode == null) {
            throw new BusinessException(MailErrorCode.VERIFICATION_CODE_EXPIRED);
        }

        if (!savedCode.equals(inputCode)) {
            throw new BusinessException(MailErrorCode.VERIFICATION_CODE_MISMATCH);
        }

        // 성공 시 Redis에서 삭제
        verificationCodeRedisRepository.deleteByEmail(email);

        // 인증 완료 상태 저장 (TTL 30분)
        verifiedEmailRedisRepository.save(email);

        log.info("[이메일 인증 성공] {}", email);
    }

    public boolean isVerified(String email) {
        return verifiedEmailRedisRepository.exists(email);
    }

    // 임시 비밀번호 발급 이메일 전송
    public void sendNewPassword(String email, String newPassword) {
        String content = """
                안녕하세요. NCB 입니다.

                요청하신 임시 비밀번호를 발급해드립니다.
                로그인 후 반드시 새 비밀번호로 변경해주세요.

                임시 비밀번호: %s
                """.formatted(newPassword);

        sendEmail(email, "[NCB] 임시 비밀번호 안내", content);
    }

    public void sendRestoreLink(String email, String token) {
        String link = restoreBaseUrl + "?token=" + token;

        String content = """
                안녕하세요. NCB입니다.

                아래 링크를 클릭하시면 계정 복구가 완료됩니다.
                (링크는 15분간 유효합니다.)

                %s
                """.formatted(link);

        sendEmail(email, "[NCB] 계정 복구 안내", content);
    }

    @Transactional
    public void clearVerifiedEmail(String email) {
        verifiedEmailRedisRepository.delete(email);
    }
}
