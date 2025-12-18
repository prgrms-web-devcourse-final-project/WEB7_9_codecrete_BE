package com.back.web7_9_codecrete_be.domain.concerts.controller;

import com.back.web7_9_codecrete_be.domain.concerts.dto.KopisApiDto.concert.ConcertListResponse;
import com.back.web7_9_codecrete_be.domain.concerts.dto.KopisApiDto.concertPlace.ConcertPlaceListResponse;
import com.back.web7_9_codecrete_be.domain.concerts.dto.KopisApiDto.result.SetResultResponse;
import com.back.web7_9_codecrete_be.domain.concerts.dto.concert.ConcertDetailResponse;
import com.back.web7_9_codecrete_be.domain.concerts.dto.concert.ConcertItem;
import com.back.web7_9_codecrete_be.domain.concerts.dto.concert.ConcertTicketTimeSetRequest;
import com.back.web7_9_codecrete_be.domain.concerts.dto.concert.ConcertUpdateRequest;
import com.back.web7_9_codecrete_be.domain.concerts.service.ConcertNotifyService;
import com.back.web7_9_codecrete_be.domain.concerts.service.ConcertService;
import com.back.web7_9_codecrete_be.domain.concerts.service.KopisApiService;
import com.back.web7_9_codecrete_be.global.rsData.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/concerts/")
@Tag(name = "Concerts Admin", description = "공연에 대해서 관리하는 API입니다. ")
public class ConcertAdminController { // todo : 인증 권한 추가하기
    private final ConcertService concertService;
    private final KopisApiService kopisApiService;
    private final ConcertNotifyService concertNotifyService;


    @Operation(summary = "초기 공연 정보 저장", description = "25년 12월부터 앞으로 6개월 이후까지의 전체 공연의 정보를 가져와서 저장합니다. 대략 10~12분 정도 시간이 소요됩니다.")
    @PostMapping("setConcertData")
    public RsData<SetResultResponse> setConcert() throws InterruptedException {
        return RsData.success(kopisApiService.setConcertsList());
    }

    @Operation(summary = "공연 정보 갱신", description = "공연 정보를 직접 갱신합니다.")
    @PatchMapping("updateConcert/{concertId}")
    public RsData<ConcertItem> updateConcert(
            @PathVariable
            @Schema(description = """
                    <h3>갱신 대상이 될 공연의 concertId입니다.</h3>
                    <hr/>
                    DB에 저장되어 있는 공연의 ID 값입니다. <br/>
                    <strong>/updateConcert/{concertId}</strong> 형태로 요청하면 됩니다.
                    """)
            Long concertId,
            @RequestBody
            @Schema(description = """
                    <h3>공연 갱신 요청 정보입니다.</h3>
                    <hr/>
                    공연의 제목, 설명 등 수정이 필요한 정보들을 전달합니다.
                    """)
            ConcertUpdateRequest concertUpdateRequest
    ) {
        ConcertItem concertItem = concertService.updateConcert(concertId, concertUpdateRequest);
        return RsData.success("공연 정보 수정이 완료되었습니다.", concertItem);
    }

    @Operation(summary = "예매 시간이 없는 공연 목록 조회", description = "예매 시간이 없는 공연들을 공연시간 내림차순으로 출력합니다.")
    @GetMapping("noTicketTimeList")
    public List<ConcertItem> getNoTicketTimeConcertsList(
            @Schema(description = "무한스크롤 및 페이징 처리에 사용할 Pageable입니다.")
            Pageable pageable
    ) {
        return concertService.getNoTicketTimeConcertsList(pageable);
    }

    @Operation(summary = "공연을 삭제합니다.", description = "해당 공연을 삭제합니다.")
    @DeleteMapping("deleteConcert/{concertId}")
    public RsData<Void> deleteConcert(
            @PathVariable
            @Schema(description = """
                    <h3>삭제 대상이 될 공연의 concertId입니다.</h3>
                    <hr/>
                    DB에 저장되어 있는 공연의 ID 값입니다. <br/>
                    <strong>/deleteConcert/{concertId}</strong> 형태로 요청하면 됩니다.
                    """)
            Long concertId
    ) {
        concertService.deleteConcert(concertId);
        return RsData.success("공연 정보 삭제에 성공하였습니다.", null);
    }

    @Operation(summary = "예매 시간 등록", description = "개별 공연에 대한 예매 시간을 설정합니다.")
    @PatchMapping("ticketTimeSet")
    public RsData<ConcertDetailResponse> ticketTimeSet(
            @RequestBody
            @Schema(description = """
                    <h3>공연 예매 시간 설정 정보입니다.</h3>
                    <hr/>
                    공연 ID와 예매 시작 시간 정보를 전달합니다.
                    """)
            ConcertTicketTimeSetRequest concertTicketTimeSetRequest
    ) {
        return RsData.success(concertService.setConcertTime(concertTicketTimeSetRequest));
    }

    @Operation(summary = "개별 공연 API통한 갱신", description = "개별 공연에 대해서 공연 예술 통합망(Kopis)을 통해 데이터를 조회하고 해당 데이터를 갱신합니다.")
    @PatchMapping("updateConcertByKopisAPI/{concertId}")
    public RsData<ConcertDetailResponse> updateConcertByKopisAPI(
            @PathVariable
            @Schema(description = """
                    <h3>갱신 대상이 될 공연의 concertId입니다.</h3>
                    <hr/>
                    DB에 저장되어 있는 공연의 ID 값입니다. <br/>
                    <strong>/updateConcertByKopisAPI/{concertId}</strong> 형태로 요청하면 됩니다.
                    """)
            Long concertId
    ) {
        kopisApiService.concertUpdateByKopisApi(concertId);
        return RsData.success(concertService.getConcertDetail(concertId));
    }

    @Operation(summary = "공연 목록 갱신", description = "전체 공연에 대해서 공연 예술 통합망(Kopis)을 통해 데이터를 조회하고, 바뀐 내용을 갱신하고 추가된 공연을 가져옵니다.")
    @PostMapping("updateConcertData")
    public RsData<SetResultResponse> updateConcert() throws InterruptedException {
        return RsData.success(kopisApiService.updateConcertData());
    }

    @Operation(summary = "알림 이메일 전송", description = "예매일이 오늘인 공연에 대해 알림 이메일을 전송합니다.")
    @PostMapping("sendTicketingEmail")
    public RsData<String> sendTicketingEmail(){
        String resultMessage = concertNotifyService.sendTodayTicketingConcertsNotifyingEmail();
        return RsData.success(resultMessage,null);
    }

}
