package com.back.web7_9_codecrete_be.domain.email.service;

import com.back.web7_9_codecrete_be.domain.email.entity.VerificationCode;
import com.back.web7_9_codecrete_be.domain.email.entity.VerifiedEmail;
import com.back.web7_9_codecrete_be.domain.email.repository.VerificationCodeRepository;
import com.back.web7_9_codecrete_be.domain.email.repository.VerifiedEmailRepository;
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
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final VerificationCodeRepository verificationCodeRepository;
    private final VerifiedEmailRepository verifiedEmailRepository;
    private final WebClient mailgunClient;

    @Value("${mailgun.from}")
    private String fromEmail;

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
        verificationCodeRepository.deleteByEmail(email);

        // DB 저장
        VerificationCode entity = VerificationCode.builder()
                .email(email)
                .code(code)
                .expireAt(LocalDateTime.now().plusSeconds(TTL_SECONDS))
                .build();

        verificationCodeRepository.save(entity);

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
        VerificationCode saved = verificationCodeRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(MailErrorCode.VERIFICATION_CODE_EXPIRED));

        if (saved.isExpired()) {
            verificationCodeRepository.deleteByEmail(email);
            throw new BusinessException(MailErrorCode.VERIFICATION_CODE_EXPIRED);
        }

        if (!saved.getCode().equals(inputCode)) {
            throw new BusinessException(MailErrorCode.VERIFICATION_CODE_MISMATCH);
        }

        // 성공 시 삭제
        verificationCodeRepository.deleteByEmail(email);


        // 인증 완료 상태 저장
        verifiedEmailRepository.save(new VerifiedEmail(email));

        log.info("[이메일 인증 성공] {}", email);
    }

    public boolean isVerified(String email) {
        return verifiedEmailRepository.existsByEmail(email);
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
}
