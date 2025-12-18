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
import java.time.LocalDate;

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
    private void sendEmail(String toEmail, String subject,String htmlContent ,String textContent) {
        try {
            LinkedMultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("from", fromEmail);
            form.add("to", toEmail);
            form.add("subject", subject);
            form.add("html", htmlContent);
            form.add("text", textContent);

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

        String htmlContent = """
                <!doctype html>
                <html lang="ko">
                <body style="margin:0;padding:0;background-color:#fafafa;
                font-family:-apple-system,BlinkMacSystemFont,system-ui,Roboto,
                Helvetica Neue,Segoe UI,Apple SD Gothic Neo,Noto Sans KR,Malgun Gothic,sans-serif;">
                
                <div style="max-width:480px;margin:40px auto;background-color:#ffffff;
                border-radius:12px;overflow:hidden;
                box-shadow:0 2px 8px rgba(0,0,0,0.08);">
                
                    <!-- Header -->
                    <div style="padding:24px;background-color:#1a1a1a;color:#ffffff;">
                        <div style="font-size:20px;font-weight:700;">
                            🔐 인증 코드 안내
                        </div>
                    </div>
                
                    <!-- Content -->
                    <div style="padding:28px;color:#1a1a1a;line-height:1.6;">
                
                        <p style="margin:0 0 12px 0;">
                            안녕하세요. <strong>NCB</strong> 입니다.
                        </p>
                
                        <p style="margin:0 0 16px 0;">
                            아래 <strong>인증 코드</strong>를 입력해 주세요.
                        </p>
                
                        <!-- OTP -->
                        <div style="background-color:#f5f5f5;
                        padding:16px 12px;border-radius:8px;
                        text-align:center;
                        font-size:22px;font-weight:700;
                        letter-spacing:4px;">
                            %s
                        </div>
                
                        <p style="margin:12px 0 0 0;font-size:13px;color:#666;">
                            ⏱ 유효시간: <strong>5분</strong>
                        </p>
                
                        <p style="margin-top:24px;font-size:12px;color:#999;">
                            본 메일은 자동으로 발송된 메일입니다.<br/>
                            본인이 요청하지 않은 경우, 해당 메일을 무시해 주세요.
                        </p>
                    </div>
                
                </div>
                
                </body>
                </html>
                """.formatted(code);

        String textContent = """
                안녕하세요. NCB 입니다.

                아래 인증 코드를 입력해 주세요.

                인증코드: %s
                (유효시간 5분)
                """.formatted(code);

        sendEmail(email, "[NCB] 이메일 인증 코드", htmlContent, textContent);
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
        String htmlContent = """
                <!doctype html>
                <html lang="ko">
                <body style="margin:0;padding:0;background-color:#fafafa;
                font-family:-apple-system,BlinkMacSystemFont,system-ui,Roboto,
                Helvetica Neue,Segoe UI,Apple SD Gothic Neo,Noto Sans KR,Malgun Gothic,sans-serif;">
                
                <div style="max-width:520px;margin:40px auto;background-color:#ffffff;
                border-radius:12px;overflow:hidden;
                box-shadow:0 2px 8px rgba(0,0,0,0.08);">
                
                    <!-- Header -->
                    <div style="padding:28px;background-color:#1a1a1a;color:#ffffff;">
                        <div style="font-size:22px;font-weight:700;">
                            🔐 임시 비밀번호 안내
                        </div>
                    </div>
                
                    <!-- Content -->
                    <div style="padding:28px;color:#1a1a1a;line-height:1.6;">
                
                        <p style="margin:0 0 12px 0;">
                            안녕하세요. <strong>NCB</strong> 입니다.
                        </p>
                
                        <p style="margin:0 0 16px 0;">
                            요청하신 임시 비밀번호를 발급해드립니다.<br/>
                            로그인 후 반드시 <strong>새 비밀번호로 변경</strong>해주세요.
                        </p>
                
                        <div style="background-color:#f5f5f5;
                        padding:16px;border-radius:8px;
                        font-size:16px;font-weight:700;
                        text-align:center;letter-spacing:1px;">
                            %s
                        </div>
                
                        <p style="margin:20px 0 0 0;font-size:12px;color:#666;">
                            본 메일은 자동으로 발송된 메일입니다.
                        </p>
                    </div>
                
                </div>
                
                </body>
                </html>
                """.formatted(newPassword);

        String textContent = """
                안녕하세요. NCB 입니다.

                요청하신 임시 비밀번호를 발급해드립니다.
                로그인 후 반드시 새 비밀번호로 변경해주세요.

                임시 비밀번호: %s
                """.formatted(newPassword);

        sendEmail(email, "[NCB] 임시 비밀번호 안내",htmlContent, textContent);
    }

    public void sendRestoreLink(String email, String token) {
        String link = restoreBaseUrl + "?token=" + token;
        String htmlContent = """
                <!doctype html>
                <html lang="ko">
                <body style="margin:0;padding:0;background-color:#fafafa;
                font-family:-apple-system,BlinkMacSystemFont,system-ui,Roboto,
                Helvetica Neue,Segoe UI,Apple SD Gothic Neo,Noto Sans KR,Malgun Gothic,sans-serif;">
                
                <div style="max-width:520px;margin:40px auto;background-color:#ffffff;
                border-radius:12px;overflow:hidden;
                box-shadow:0 2px 8px rgba(0,0,0,0.08);">
                
                    <!-- Header -->
                    <div style="padding:28px;background-color:#1a1a1a;color:#ffffff;">
                        <div style="font-size:22px;font-weight:700;">
                            🔐 계정 복구 안내
                        </div>
                    </div>
                
                    <!-- Content -->
                    <div style="padding:28px;color:#1a1a1a;line-height:1.6;">
                
                        <p style="margin:0 0 12px 0;">
                            안녕하세요. <strong>NCB</strong>입니다.
                        </p>
                
                        <p style="margin:0 0 16px 0;">
                            아래 버튼을 클릭하시면 <strong>계정 복구가 완료</strong>됩니다.<br/>
                            <span style="font-size:13px;color:#666;">
                                (해당 링크는 <strong>15분간</strong> 유효합니다.)
                            </span>
                        </p>
                
                        <!-- Button -->
                        <div style="text-align:center;margin:24px 0;">
                            <a href="%s"
                               target="_blank"
                               style="display:inline-block;padding:12px 28px;
                               background-color:#1a1a1a;color:#ffffff;
                               text-decoration:none;border-radius:6px;
                               font-size:15px;font-weight:600;">
                                계정 복구하기
                            </a>
                        </div>
                
                        <!-- Fallback Link -->
                        <p style="font-size:12px;color:#666;word-break:break-all;">
                            버튼이 정상적으로 동작하지 않는 경우,<br/>
                            아래 링크를 복사하여 브라우저에 붙여넣어 주세요.
                        </p>
                
                        <p style="font-size:12px;">
                            <a href="%s" target="_blank" style="color:#1a1a1a;">
                                %s
                            </a>
                        </p>
                
                        <p style="margin-top:24px;font-size:12px;color:#999;">
                            본 메일은 자동으로 발송된 메일입니다.
                        </p>
                    </div>
                
                </div>
                
                </body>
                </html>
                """.formatted(link,link,link);

        String textContent = """
                안녕하세요. NCB입니다.

                아래 링크를 클릭하시면 계정 복구가 완료됩니다.
                (링크는 15분간 유효합니다.)

                %s
                """.formatted(link);

        sendEmail(email, "[NCB] 계정 복구 안내",htmlContent, textContent);
    }

    public void sendNotifyEmail(String email, String htmlContent, String textContent) {
        String subject = "[NCB] "+ LocalDate.now().toString() + " 오늘의 예매 알림입니다.";
        sendEmail(email, subject, htmlContent, textContent);
//        sendHtmlEmail(email, subject, htmlContent);
    }

    @Transactional
    public void clearVerifiedEmail(String email) {
        verifiedEmailRedisRepository.delete(email);
    }
}
