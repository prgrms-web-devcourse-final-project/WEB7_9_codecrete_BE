package com.back.web7_9_codecrete_be.domain.concerts.service;

import com.back.web7_9_codecrete_be.domain.concerts.entity.Concert;
import com.back.web7_9_codecrete_be.domain.concerts.entity.ConcertLike;
import com.back.web7_9_codecrete_be.domain.concerts.entity.TicketOffice;
import com.back.web7_9_codecrete_be.domain.concerts.repository.ConcertLikeRepository;
import com.back.web7_9_codecrete_be.domain.concerts.repository.ConcertRepository;
import com.back.web7_9_codecrete_be.domain.concerts.repository.TicketOfficeRepository;
import com.back.web7_9_codecrete_be.domain.email.service.EmailService;
import com.back.web7_9_codecrete_be.domain.users.entity.User;
import com.back.web7_9_codecrete_be.domain.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@EnableScheduling
@RequiredArgsConstructor
public class ConcertNotifyService {
    private final UserRepository userRepository;
    private final ConcertRepository concertRepository;
    private final TicketOfficeRepository ticketOfficeRepository;
    private final ConcertLikeRepository concertLikeRepository;
    private final EmailService emailService;


    private List<Concert> getTodayTicketingConcerts() {
        LocalDateTime startOfToday = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime endOfToday = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);

        // 오늘의 시작과 끝 사이에 있는 공연 전부 가져오기
        List<Concert> concerts = concertRepository.getConcertByTicketTimeBetween(startOfToday, endOfToday);
        return concerts;
    }


    private Map<Long, List<TicketOffice>> getAllTicketOfficesMapFromConcerts(List<Concert> concerts) {
        // 예매처 정보를 빠르게 가져오기 위한 Map 지정
        Map<Long, List<TicketOffice>> ticketOfficeMap = new HashMap<>();
        //공연에 대한 모든 예매처 정보를 가져와서 저장
        List<TicketOffice> ticketOffices = ticketOfficeRepository.findAllByConcerts(concerts);

        for (TicketOffice ticketOffice : ticketOffices) {
            log.info("예매처 조회");
            Long concertId = ticketOffice.getConcert().getConcertId();
            List<TicketOffice> ticketOfficeList = ticketOfficeMap.getOrDefault(concertId, new ArrayList<>());
            ticketOfficeList.add(ticketOffice);
            ticketOfficeMap.put(concertId, ticketOfficeList);
        }

        // 줄일 수 없긴 개뿔, 가능하네.
        /*
        for (Concert concert : concerts) {
            ticketOffices = ticketOfficeRepository.getTicketOfficesByConcert(concert);
            ticketOfficeMap.put(concert.getConcertId(), ticketOffices);
        }
        */
        // 예매처 맵 반환
        return ticketOfficeMap;
    }

    private Map<String, List<Long>> getSendingEmailFromLikeUser(List<Concert> concerts) {
        // 이메일 값을 키 값으로 해서 전송할 concert Id를 추가?
        Map<String, List<Long>> emailMap = new HashMap<>();

        LocalDateTime startOfToday = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime endOfToday = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);

        List<ConcertLike> concertLikes = concertLikeRepository.getTodayConcertTicketingLikes(startOfToday, endOfToday);
        for (ConcertLike concertLike : concertLikes) {
            log.info("사용자 email 조회");
            // map에 해당 사용자 email의 ConcertId list 가져오기, 없다면 새로은 arraylist 생성
            if(!concertLike.getUser().getUserSetting().isEmailNotifications()) continue;
            List<Long> tempList = emailMap.getOrDefault(concertLike.getUser().getEmail(), new ArrayList<>());
            // 임시 리스트에 concertId 추가
            tempList.add(concertLike.getConcert().getConcertId());
            // map에 유저 email 기준으로 해당 리스트 바꾸기;
            emailMap.put(concertLike.getUser().getEmail(), tempList);
        }
        /*
        for (Concert concert : concerts){
            // 오늘 예매 예정인 공연의 좋아요 목록을 전부 가져오기 // 쿼리 써서 개선 가능할 것 같은데?
            List<ConcertLike> concertLikes = concertLikeRepository.getConcertLikesByConcert(concert);
            for (ConcertLike concertLike : concertLikes){
                // map에 해당 사용자 email의 ConcertId list 가져오기, 없다면 새로은 arraylist 사용
                List<Long> tempList = emailMap.getOrDefault(concertLike.getUser().getEmail(),new ArrayList<>());
                // 임시 리스트에 concertId 추가
                tempList.add(concertLike.getConcert().getConcertId());
                // map에 유저 email 기준으로 해당 리스트 바꾸기;
                emailMap.put(concertLike.getUser().getEmail(),tempList);
            }
        }
         */
        return emailMap;
    }

    @Scheduled(cron = "0 0 9 * * *")
    public String sendTodayTicketingConcertsNotifyingEmail() {
        List<Concert> concerts = getTodayTicketingConcerts();
        // 빠른 조회를 위해 Map으로 변환
        Map<Long, Concert> concertMap = new HashMap<>();
        for (Concert concert : concerts) {
            concertMap.put(concert.getConcertId(), concert);
        }

        // 예매처 map 가져오기
        Map<Long, List<TicketOffice>> ticketOfficesMap = getAllTicketOfficesMapFromConcerts(concerts);
        // email에 따른 ConcertId 맵 가져오기
        Map<String, List<Long>> emailMap = getSendingEmailFromLikeUser(concerts);

        LocalDate today = LocalDate.now();

        int totalConcertsCount = concerts.size();
        int totalEmailCount = emailMap.size();

        for (String targetEmail : emailMap.keySet()) {

            StringBuilder htmlStringBuilder = new StringBuilder();
            StringBuilder textStringBuilder = new StringBuilder();

            // 전체 타이틀 부분(html)
            htmlStringBuilder.append("""
            <!doctype html>
            <html lang="ko">
            <head>
                <meta charset="UTF-8" />
                <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                <title>공연 예매 알림</title>
            </head>
            <body style="margin:0;padding:0;background-color:#fafafa;
                font-family:-apple-system,BlinkMacSystemFont,'Segoe UI','Malgun Gothic',
                'Apple SD Gothic Neo','Noto Sans KR',sans-serif;line-height:1.5;">
            
            <div style="max-width:680px;margin:40px auto;background:#ffffff;
                border-radius:16px;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,0.08);">
            
                <!-- Header -->
                <div style="padding:32px;background:linear-gradient(135deg,#1a1a1a 0%%,#2d2d2d 100%%);
                    color:#ffffff;">
                    <h1 style="margin:0 0 8px 0;font-size:26px;font-weight:700;letter-spacing:-0.5px;">
                        🎟 %s 오늘의 공연 예매 알림
                    </h1>
                    <div style="font-size:14px;opacity:0.85;">
                        예매 시작 공연을 알려드립니다.
                    </div>
                </div>
            
                <div style="padding:32px;">
            """.formatted(today));
            // 전체 타이틀 부분(text)
            textStringBuilder.append("""
                    [NCB] 공연 예매 알림입니다.
                    %s 오늘의 공연 예매 알림
                    예매 시작 공연을 알려드립니다.
                    ---------------------------------------------------------
                    """.formatted(today));

            // 개별 공연 내용 작성
            for (Long concertId : emailMap.get(targetEmail)) {

                Concert concert = concertMap.get(concertId);

                String posterImage = concert.getPosterUrl();
                if (posterImage == null || posterImage.isBlank()) {
                    posterImage = "https://via.placeholder.com/640x360?text=No+Image";
                }

                // 날짜 정보 파싱 (예: 2025년 12월 17일 → 17, DEC)
                String day = concert.getTicketTime().format(DateTimeFormatter.ofPattern("dd"));
                String month = concert.getTicketTime().format(DateTimeFormatter.ofPattern("MMM", java.util.Locale.ENGLISH)).toUpperCase();

                // 개별 공연 HTML 시작부
                htmlStringBuilder.append("""
                    <!-- 공연 카드 시작 -->
                    <div style="border:1px solid #e8e8e8;border-radius:12px;
                        padding:24px;margin-bottom:20px;background:#ffffff;">
                        
                        <!-- 포스터 이미지 -->
                        <img src="%s" alt="공연 포스터"
                            style="display:block;width:100%%;height:200px;
                            object-fit:cover;border-radius:8px;margin-bottom:16px;" />
                        
                        <!-- 공연 헤더 -->
                        <div style="display:flex;align-items:flex-start;gap:20px;margin-bottom:16px;">
                            
                            <!-- 날짜 박스 -->
                            <div style="width:64px;text-align:center;padding:12px 0;
                                background:#f5f5f5;border-radius:8px;flex-shrink:0;">
                                <div style="font-size:28px;font-weight:700;color:#1a1a1a;line-height:1;">
                                    %s
                                </div>
                                <div style="font-size:13px;color:#666;margin-top:4px;font-weight:500;">
                                    %s
                                </div>
                            </div>
                            
                            <!-- 공연 정보 -->
                            <div style="flex:1;">
                                <div style="font-size:16px;font-weight:600;color:#1a1a1a;margin-bottom:8px;">
                                    %s
                                </div>
                                <div style="font-size:12px;color:#666;">
                                    ⏰ %s
                                </div>
                            </div>
                        </div>
                """.formatted(
                        posterImage,
                        day,
                        month,
                        concert.getName(),
                        concert.getTicketTime()
                                .format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 HH시 mm분"))
                ));
                // 개별 공연 text 시작부
                textStringBuilder.append("""
                        공연명: %s
                        티켓팅 시간: %s
                        
                        """.formatted(concert.getName(), concert.getTicketTime().format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 HH시 mm분"))));

                // 공연 예매처 반복 처리
                for (TicketOffice ticketOffice : ticketOfficesMap.get(concertId)) {
                    // 개별 예매처 html
                    htmlStringBuilder.append("""
                        <!-- 예매처 -->
                        <div style="background:#f8f8f8;padding:16px;border-radius:8px;margin-bottom:8px;">
                            <div style="display:flex;justify-content:space-between;
                                align-items:center;flex-wrap:wrap;gap:12px;">
                                
                                <div style="flex:1;min-width:150px;">
                                    <div style="font-size:12px;color:#888;margin-bottom:4px;">
                                        예매처
                                    </div>
                                    <div style="font-size:14px;font-weight:600;color:#1a1a1a;">
                                        %s
                                    </div>
                                </div>
                                
                                <a href="%s" target="_blank"
                                    style="display:inline-block;padding:8px 24px;
                                    background:#1a1a1a;color:#ffffff;text-decoration:none;
                                    border-radius:6px;font-size:14px;font-weight:600;">
                                    예매하기
                                </a>
                            </div>
                        </div>
                    """.formatted(
                            ticketOffice.getTicketOfficeName(),
                            ticketOffice.getTicketOfficeUrl()
                    ));
                    // 개별 예매처 text
                    textStringBuilder.append("""
                            예매처: %s
                            예매링크: %s
                            """.formatted(ticketOffice.getTicketOfficeName(), ticketOffice.getTicketOfficeUrl()));
                }

                htmlStringBuilder.append("</div>"); // 공연 카드 종료
                textStringBuilder.append("""
                        ---------------------------------------------------------
                        """); // text 자름
            }

            // 공연 마지막 바닥 부분 처리
            htmlStringBuilder.append("""
                    <!-- 유의사항 -->
                    <div style="background:#f8f8f8;padding:20px 24px;margin-top:24px;border-radius:8px;">
                        <div style="display:flex;align-items:center;gap:6px;
                            font-size:14px;font-weight:600;color:#1a1a1a;margin-bottom:8px;">
                            ⚠️ 유의사항
                        </div>
                        <div style="font-size:12px;color:#666;line-height:1.6;">
                            공연 정보는 각 공연의 상황에 따라 변경될 수 있으니 
                            예매 전 반드시 확인해주세요.
                        </div>
                    </div>
                
                </div>
                
                <!-- Footer -->
                <div style="text-align:center;padding:32px;background:#fafafa;
                    color:#999;font-size:12px;line-height:1.6;">
                    이 메일은 자동으로 발송되었습니다.<br/>
                    © 2025 Concert Notification Service
                </div>
            
            </div>
            </body>
            </html>
            """);
            textStringBuilder.append("""
                    유의사항 : 공연 정보는 각 공연의 상황에 따라 변경될 수 있으니 예매 전 반드시 확인해주세요.
                    """);


            String htmlContent = htmlStringBuilder.toString();
            String textContent = textStringBuilder.toString();
            emailService.sendNotifyEmail(targetEmail, htmlContent,textContent);
        }
        log.info("일일 공연 예매 오픈 알림 : " +  totalConcertsCount + "건의 공연을 " + totalEmailCount + "명의 사용자에게 전송했습니다.");
        return totalConcertsCount + "건의 공연을 " + totalEmailCount + "명의 사용자에게 전송했습니다.";
    }
}


