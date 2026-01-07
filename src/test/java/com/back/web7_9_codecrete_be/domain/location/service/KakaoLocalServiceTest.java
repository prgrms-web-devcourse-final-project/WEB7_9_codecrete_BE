package com.back.web7_9_codecrete_be.domain.location.service;

import com.back.web7_9_codecrete_be.domain.location.dto.response.kakao.KakaoLocalResponse;
import com.back.web7_9_codecrete_be.global.config.WebClientConfig;
import com.back.web7_9_codecrete_be.global.error.exception.BusinessException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = { KakaoLocalService.class, WebClientConfig.class }
)       //다른 api들 까지 검사 말고, 카카오 api만 검사하려고 이렇게 설정
@Import(WebClientConfig.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)     //테스트마다 인스턴스를 새로 만들지 말고, 1개만 사용하게 하기
class KakaoLocalServiceTest {

    private static MockWebServer server;        //가짜 카카오 서버를 테스트에서 공유함

    @Autowired
    private KakaoLocalService kakaoLocalService;        //내가 검사하고싶은 서비스

    @DynamicPropertySource      //스프링 컨텍스트가 만들어지기 전에 property를 동적으로 주입하는 훅
                                //스프링에게 ‘이 설정값은 이렇게 써라’라고 알려주는 등록기
    static void overrideProps(DynamicPropertyRegistry r) {
        try {
            if (server == null) {
                server = new MockWebServer();
                server.start();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        //  카카오 API 주소와 키를 Mock서버, 테스트 키로 바꾸기
        r.add("kakao.base-url", () -> server.url("/").toString());      // MockWebServer는 랜덤 포트라서 동적으로 넣어줘야 함
        r.add("kakao.restapi-key", () -> "KakaoAK test-key");
    }

    //순서 : DynamicPropertySource  -> Spring Context -> WebClient Bean 생성


    @AfterAll
    static void shutdown() throws IOException {
        if (server != null) server.shutdown();
    }

    @BeforeEach
    void resetQueueByDesign() {
        // 이 테스트는 서버 재시작으로 큐를 리셋하지 않고,
        // 테스트 구조로 큐 격리를 보장
    }

    @Test
    void searchNearbyRestaurantsTest() throws Exception {
        server.enqueue(new MockResponse()       // setBody에 있는 요청에 대한 응답을 enqueue에 넣기
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json; charset=UTF-8")
                .setBody("""        
                {
                  "documents": [
                    { "place_name": "테스트식당", "x": "127.0", "y": "37.5" }
                  ]
                }
                """));      //setBody : 실제 JSON 응답 body

        //kakasLocalService에 있는 searchNearByRestaurants 함수에 위도, 경도를 넣고 WebClient가 호출해서 응답을 반환
        List<KakaoLocalResponse.Document> docs =
                kakaoLocalService.searchNearbyRestaurantsCached(37.5, 127.0);

        assertThat(docs).hasSize(1);        //응답 배열의 크기가 1인지
        assertThat(docs.get(0).getPlace_name()).isEqualTo("테스트식당"); // 제대로 필드가 들어갔는지 확인

        //여기서부터는 MockWebServer가 실제로 받은 요청을 검사
        var req = server.takeRequest();
        assertThat(req.getMethod()).isEqualTo("GET");       //컨트롤러가 POST지만, 이거는 WebClient에서 GET을 하는거라서 GET이 맞음, 즉 내 서버 -> 카카오 서버 요청이라서
        assertThat(req.getHeader("Authorization")).isEqualTo("KakaoAK test-key");   //인증 헤더가 잘 들어갔는지 확인

        assertThat(req.getPath()).startsWith("/v2/local/search/keyword.json?");
        String encoded = URLEncoder.encode("음식점", StandardCharsets.UTF_8);
        assertThat(req.getPath()).contains("query=" + encoded);
        assertThat(req.getPath()).contains("y=37.5");
        assertThat(req.getPath()).contains("x=127.0");
        assertThat(req.getPath()).contains("radius=1000");
        assertThat(req.getPath()).contains("sort=distance");
    }

//  이제부터 밑에있는 테스트는 위에 있는 테스트와 비슷하므로 따로 추가 설명은 안썼습니다
    
    @Test
    void searchNearbyCafesTest() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json; charset=UTF-8")
                .setBody("""
                {
                  "documents": [
                    { "place_name": "테스트카페", "x": "126.978", "y": "37.5665" }
                  ]
                }
                """));

        List<KakaoLocalResponse.Document> docs =
                kakaoLocalService.searchNearbyCafesCached(37.5665, 126.9780);

        assertThat(docs).hasSize(1);
        assertThat(docs.get(0).getPlace_name()).isEqualTo("테스트카페");

        var req = server.takeRequest();
        assertThat(req.getMethod()).isEqualTo("GET");
        assertThat(req.getHeader("Authorization")).isEqualTo("KakaoAK test-key");

        assertThat(req.getPath()).startsWith("/v2/local/search/keyword.json?");
        String encoded = URLEncoder.encode("카페", StandardCharsets.UTF_8);
        assertThat(req.getPath()).contains("query=" + encoded);
        assertThat(req.getPath()).contains("category_group_code=CE7");
        assertThat(req.getPath()).contains("y=37.5665");
        assertThat(req.getPath()).contains("x=126.978");
        assertThat(req.getPath()).contains("radius=1000");
        assertThat(req.getPath()).contains("sort=distance");
    }

    @Test
    void coordinateToAddressNameTest() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json; charset=UTF-8")
                .setBody("""
                {
                  "documents": [
                    {
                      "road_address": { "address_name": "서울특별시 중구 세종대로 110" },
                      "address": { "address_name": "서울특별시 중구 태평로1가 31" }
                    }
                  ]
                }
                """));

        String address = kakaoLocalService.coordinateToAddressName(37.5665, 126.9780);

        assertThat(address).isEqualTo("서울특별시 중구 세종대로 110");

        var req = server.takeRequest();
        assertThat(req.getMethod()).isEqualTo("GET");
        assertThat(req.getHeader("Authorization")).isEqualTo("KakaoAK test-key");

        assertThat(req.getPath()).startsWith("/v2/local/geo/coord2address.json?");
        assertThat(req.getPath()).contains("x=126.978");
        assertThat(req.getPath()).contains("y=37.5665");
    }

    @Test
    void coordinateToAddressName_documents가비면_BusinessException() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json; charset=UTF-8")
                .setBody("{\"documents\":[{\"place_name\":\"테스트카페\",\"x\":\"126.9780\",\"y\":\"37.5665\"}]}"));

        assertThatThrownBy(() -> kakaoLocalService.coordinateToAddressName(37.0, 127.0))
                .isInstanceOf(BusinessException.class);

        var req = server.takeRequest();
        assertThat(req.getMethod()).isEqualTo("GET");
        assertThat(req.getPath()).startsWith("/v2/local/geo/coord2address.json?");
    }
}
