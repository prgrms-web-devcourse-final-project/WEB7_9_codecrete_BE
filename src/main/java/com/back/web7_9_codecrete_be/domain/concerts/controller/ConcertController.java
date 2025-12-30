package com.back.web7_9_codecrete_be.domain.concerts.controller;

import com.back.web7_9_codecrete_be.domain.concerts.dto.concert.*;
import com.back.web7_9_codecrete_be.domain.concerts.dto.concertPlace.PlaceDetailResponse;
import com.back.web7_9_codecrete_be.domain.concerts.dto.ticketOffice.TicketOfficeElement;
import com.back.web7_9_codecrete_be.domain.concerts.service.ConcertService;
import com.back.web7_9_codecrete_be.domain.users.entity.User;
import com.back.web7_9_codecrete_be.global.rq.Rq;
import com.back.web7_9_codecrete_be.global.rsData.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("api/v1/concerts/")
@RequiredArgsConstructor
@Tag(name = "Concerts", description = "공연에 대한 정보를 제공하는 API 입니다.")
public class ConcertController {
    private final ConcertService concertService;
    private final Rq rq;

    @Operation(summary = "공연목록",
            description = """
                    <h3>공연 전체 목록을 조회하는 통합 API입니다.</h3>
                    <hr/>
                    다양한 조회 기준에 따라 공연 목록을 조회합니다.<br/>
                    """)
    @GetMapping("list/{sort}")
    public RsData<List<ConcertItem>> getList(
            @Schema(description = """
                    <h3>리스트를 받아올 기준이 될 경로 변수입니다. <b>대문자</b>로 예시에 있는 것만 사용해 주세요.</h3>
                    <hr/>
                    <b>LIKE :</b> 좋아요 순<br/>
                    <b>VIEW :</b> 조회수 순<br/>
                    <b>TICKETING :</b> 오늘을 기준으로 다가오는 티켓팅 날짜 순<br/>
                    <b>UPCOMING :</b> 오늘을 기준으로 다가오는 공연 시작 날짜 순<br/>
                    <b>REGISTERED :</b> 가장 최근에 API에 등록된 공연 순<br/>
                    <hr/>
                    """, examples = {"LIKE", "VIEW", "TICKETING", "UPCOMING", "REGISTERED"})
            @PathVariable ListSort sort,
            @Schema(description = """
                    페이징 처리 또는 무한 스크롤 구현에 쓸 Pageable 객체입니다.<br/>
                    sort 부분은 사용하지 않으니 지워주시고 <strong>"page", "size"</strong> 만 넘겨주시면 됩니다. <font color="red">*sort 부분이 남아있으면 오류가 발생합니다.</font>
                    """)
            Pageable pageable
    ) {
        return RsData.success(concertService.getConcertsList(pageable, sort));
    }

    @Operation(summary = "좋아요 한 공연 조회", description = "좋아요를 누른 공연에 대한 목록을 조회합니다. 저장 날짜를 기준으로 내림차순 정렬로 표시합니다.(최신으로 추가된 목록순입니다.)")
    @GetMapping("likedConcertList")
    public RsData<List<ConcertItem>> getLikedConcertList(
            @Schema(description = "페이징 처리 또는 무한 스크롤 구현에 쓸 Pageable 객체입니다.")
            Pageable pageable
    ) {
        User user = rq.getUser();
        return RsData.success(concertService.getLikedConcertsList(pageable, user));
    }

    @Operation(summary = "공연 총 개수 조회", description = "저장된 공연의 총 개수를 조회합니다.")
    @GetMapping("totalConcertCount")
    public RsData<Long> getTotalConcertCount(){
        return RsData.success(concertService.getTotalConcertsCount());
    }

    @Operation(summary = "티켓팅 일정이 다가오는 공연의 개수 조회" ,
            description = "티켓팅 일정 정보가 있고, 티켓팅 일정이 다가오는 공연의 개수를 조회합니다. <br/>list/TICKETING에 대응하는 공연의 개수입니다.")
    @GetMapping("totalTicketingConcertCount")
    public RsData<Long> getTotalTicketingConcertCount(){
        return RsData.success(concertService.getTotalTicketingConcertsCount());
    }

    @Operation(summary = "좋아요 누른 공연의 개수 조회", description = "현재 로그인한 사용자가 좋아요를 누른 공연의 개수를 조회합니다.")
    @GetMapping("likedConcertCount")
    public RsData<Long> getLikedConcertCount(){
        User user = rq.getUser();
        return RsData.success(concertService.getTotalLikedConcertsCount(user));
    }


    @Operation(summary = "공연 상세 조회", description = "공연에 대한 상세 목록을 조회합니다.")
    @GetMapping("concertDetail")
    public RsData<ConcertDetailResponse> getConcertDetail(
            @RequestParam
            @Schema(description = """
                    <h3>조회 기준이 되는 concertId입니다.</h3>
                    <hr/>
                    DB에 저장되어 있는 공연의 ID 값을 기준으로 조회합니다. <br/>
                    <strong>?concertId={concertId}</strong> 로 값을 넘기시면 됩니다.
                    """)
            long concertId
    ) {
        return RsData.success(concertService.getConcertDetail(concertId));
    }

    @Operation(summary = "공연 예매처 조회", description = "공연에 대한 예매처들을 조회합니다.")
    @GetMapping("ticketOffices")
    public RsData<List<TicketOfficeElement>> getTicketOffices(
            @RequestParam
            @Schema(description = """
                    <h3>조회 기준이 되는 concertId입니다.</h3>
                    <hr/>
                    DB에 저장되어 있는 공연의 ID 값을 기준으로 조회합니다. <br/>
                    <strong>?concertId={concertId}</strong> 로 값을 넘기시면 됩니다.
                    """)
            long concertId
    ) {
        return RsData.success(concertService.getTicketOfficesList(concertId));
    }

    @Operation(summary = "공연 좋아요 기능", description = "사용자가 마음에 드는 공연에 대해 좋아요를 통해 저장할 수 있습니다.")
    @PostMapping("like/{concertId}")
    public RsData<Void> likeConcert(
            @PathVariable
            @Schema(description = """
                    <h3>좋아요를 누를 공연의 concertId입니다.</h3>
                    <hr/>
                    DB에 저장되어 있는 공연의 ID 값입니다. <br/>
                    <strong>/like/{concertId}</strong> 형태로 요청하면 해당 공연에 좋아요가 등록됩니다.
                    """)
            long concertId
    ) {
        User user = rq.getUser();
        concertService.likeConcert(concertId, user);
        return RsData.success(null);
    }

    @Operation(summary = "공연 좋아요 해제 기능", description = "좋아요를 해제할 수 있습니다.")
    @DeleteMapping("dislike/{concertId}")
    public RsData<Void> dislikeConcert(
            @PathVariable
            @Schema(description = """
                    <h3>좋아요를 해제할 공연의 concertId입니다.</h3>
                    <hr/>
                    DB에 저장되어 있는 공연의 ID 값입니다. <br/>
                    <strong>/dislike/{concertId}</strong> 형태로 요청하면 좋아요가 해제됩니다.
                    """)
            long concertId
    ) {
        User user = rq.getUser();
        concertService.dislikeConcert(concertId, user);
        return RsData.success(null);
    }

    @Operation(summary = "공연 좋아요 여부 확인", description = "좋아요 여부를 확인합니다.")
    @GetMapping("isLike/{concertId}")
    public RsData<ConcertLikeResponse> isLikeConcert(
            @PathVariable
            @Schema(description = """
                    <h3>좋아요 여부를 확인할 공연의 concertId입니다.</h3>
                    <hr/>
                    DB에 저장되어 있는 공연의 ID 값입니다. <br/>
                    <strong>/isLike/{concertId}</strong> 형태로 요청하면 좋아요 여부를 확인할 수 있습니다.
                    """)
            long concertId
    ) {
        User user = rq.getUser();
        return RsData.success(concertService.isLikeConcert(concertId, user));
    }

    // todo : 제목으로 만 검색 기능 구현 -> 추후 아티스트 정보랑 연동 <- 중요 / 정렬 기준? 최신등록순 정렬
    @Operation(summary = "공연 검색", description = "제목에 키워드를 포함하고 있는 공연 정보를 검색합니다.")
    @GetMapping("search")
    public RsData<List<ConcertItem>> searchConcert(
            @Schema(description = """
                    <h3>검색어가 되는 Keyword입니다.</h3>
                    <hr/>
                    <b>?keyword={keyword}</b> 로 값을 넘기시면 됩니다.<br/>
                    DB에서 해당 문자열을 가지고 있는 모든 결과값을 반환합니다.
                    """)
            @RequestParam String keyword,
            @Schema(description = "페이징 처리 또는 무한 스크롤 구현에 쓸 Pageable 객체입니다.")
            Pageable pageable
    ) {
        return RsData.success(concertService.getConcertListByKeyword(keyword, pageable));
    }

    @Operation(summary = "공연 검색 결과 개수" , description = "키워드를 포함하고 있는 공연의 총 개수를 반환합니다.")
    @GetMapping("searchCount")
    public RsData<Integer> getConcertSearchCount(
            @Schema(description = """
                    <h3>검색어가 되는 Keyword입니다.</h3>
                    <hr/>
                    <b>?keyword={keyword}</b> 로 값을 넘기시면 됩니다.<br/>
                    DB에서 해당 문자열을 가지고 있는 모든 결과값을 반환합니다.
                    """)
            @RequestParam String keyword
    ){
        return  RsData.success(concertService.getConcertSearchCountByKeyword(keyword));
    }

    @Operation(summary = "공연의 공연장소 상세 조회", description = "해당 공연의 공연장의 상세 정보를 표시합니다.")
    @GetMapping("placeDetail")
    public RsData<PlaceDetailResponse> placeDetail(
            @RequestParam
            @Schema(description = """
                    <h3>조회 기준이 되는 concertId입니다.</h3>
                    <hr/>
                    DB에 저장되어 있는 공연의 ID 값을 기준으로 해당 공연의 공연장 상세 정보를 조회합니다. <br/>
                    <strong>?concertId={concertId}</strong> 로 값을 넘기시면 됩니다.
                    """)
            long concertId
    ){
        return RsData.success(concertService.getConcertPlaceDetail(concertId));
    }

    @Operation(summary = "검색어 자동완성", description = "주어진 문자열을 가지고 있는 결과를 조회합니다.")
    @GetMapping("autoComplete")
    public RsData<List<AutoCompleteItem>> autoCompleteConcert(
            @RequestParam
            @Schema(description = """
                    <h3>검색어 입니다.</h3>
                    <hr/>
                    메모리에 캐싱되어 있는 공연의 정보들을 검색하고 표시합니다. <br/>
                    검색 결과는 조회수 순으로 나옵니다.
                    """)
            String keyword,
            @RequestParam
            @Schema(description = """
                    <h3>검색 시작 인덱스입니다.</h3>
                    <hr/>
                    결과 목록 중 start에 입력한 번호의 결과부터 데이터가 나옵니다.<br/>
                    """)
            int start,
            @RequestParam
            @Schema(description = """
                    <h3>검색 종료 인덱스입니다.</h3>
                    <hr/>
                    end에 입력한 번호까지 데이터가 나옵니다.<br/>
                    """)
            int end
    ){
        return RsData.success(concertService.autoCompleteSearch(keyword,start,end));
    }

}
