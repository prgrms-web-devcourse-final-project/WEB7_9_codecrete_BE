//package com.back.web7_9_codecrete_be.domain.location.controller;
//
//import okhttp3.mockwebserver.MockResponse;
//import okhttp3.mockwebserver.MockWebServer;
//import org.junit.jupiter.api.*;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
//import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
//import org.springframework.context.annotation.Import;
//import org.springframework.test.context.ActiveProfiles;
//import org.springframework.test.context.DynamicPropertyRegistry;
//import org.springframework.test.context.DynamicPropertySource;
//import org.springframework.test.web.servlet.MockMvc;
//
//import com.back.web7_9_codecrete_be.domain.location.service.KakaoLocalService;
//import com.back.web7_9_codecrete_be.global.config.WebClientConfig; // 네 프로젝트 패키지에 맞게 import 수정
//
//import java.io.IOException;
//import java.net.URLEncoder;
//import java.nio.charset.StandardCharsets;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
//@AutoConfigureMockMvc(addFilters = false)
//@ActiveProfiles("test")@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
//@WebMvcTest(controllers = KakaoApiController.class)
//@Import({KakaoLocalService.class, WebClientConfig.class})
//@TestInstance(TestInstance.Lifecycle.PER_CLASS)
//class KakaoApiControllerTest {
//
//    private static MockWebServer server;
//
//    @Autowired
//    private MockMvc mockMvc;
//
//
//    @AfterAll
//    static void stopServer() throws IOException {
//        if (server != null) server.shutdown();
//    }
//
//    @DynamicPropertySource
//    static void overrideProps(DynamicPropertyRegistry r) {
//        try {
//            if (server == null) {
//                server = new MockWebServer();
//                server.start();
//            }
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//
//        r.add("kakao.base-url", () -> server.url("/").toString());
//        r.add("kakao.restapi-key", () -> "KakaoAK test-key");
//    }
//
//    @Test
//    void POST_restaurant_우리API호출하면_카카오연동을거쳐_응답이온다() throws Exception {
//        server.enqueue(new MockResponse()
//                .setResponseCode(200)
//                .addHeader("Content-Type", "application/json; charset=UTF-8")
//                .setBody("""
//                {
//                  "documents": [
//                    {
//                      "place_name": "테스트식당",
//                      "x": "127.0",
//                      "y": "37.5",
//                      "road_address_name": "서울 ...",
//                      "address_name": "서울 ...",
//                      "place_url": "https://place.map.kakao.com/111"
//                    }
//                  ]
//                }
//                """));
//
//        mockMvc.perform(post("/api/v1/location/kakao/restaurant")
//                        .param("lat", "37.5")
//                        .param("lon", "127.0"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$[0].place_name").value("테스트식당"))
//                .andExpect(jsonPath("$[0].x").value("127.0"))
//                .andExpect(jsonPath("$[0].y").value("37.5"));
//
//        var req = server.takeRequest();
//        assertThat(req.getMethod()).isEqualTo("GET");
//        assertThat(req.getHeader("Authorization")).isEqualTo("KakaoAK test-key");
//
//        assertThat(req.getPath()).startsWith("/v2/local/search/keyword.json?");
//        String encoded = URLEncoder.encode("음식점", StandardCharsets.UTF_8);
//        assertThat(req.getPath()).contains("query=" + encoded);
//        assertThat(req.getPath()).contains("y=37.5");
//        assertThat(req.getPath()).contains("x=127.0");
//        assertThat(req.getPath()).contains("radius=1000");
//        assertThat(req.getPath()).contains("sort=distance");
//    }
//
//    @Test
//    void POST_cafes_우리API호출하면_카카오연동을거쳐_응답이온다() throws Exception {
//        server.enqueue(new MockResponse()
//                .setResponseCode(200)
//                .addHeader("Content-Type", "application/json; charset=UTF-8")
//                .setBody("""
//                {
//                  "documents": [
//                    {
//                      "place_name": "테스트카페",
//                      "x": "126.9780",
//                      "y": "37.5665",
//                      "road_address_name": "서울 ...",
//                      "address_name": "서울 ...",
//                      "place_url": "https://place.map.kakao.com/222"
//                    }
//                  ]
//                }
//                """));
//
//        mockMvc.perform(post("/api/v1/location/kakao/cafes")
//                        .param("lat", "37.5665")
//                        .param("lon", "126.9780"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$[0].place_name").value("테스트카페"))
//                .andExpect(jsonPath("$[0].x").value("126.9780"))
//                .andExpect(jsonPath("$[0].y").value("37.5665"));
//
//        var req = server.takeRequest();
//        assertThat(req.getMethod()).isEqualTo("GET");
//        assertThat(req.getHeader("Authorization")).isEqualTo("KakaoAK test-key");
//
//        assertThat(req.getPath()).startsWith("/v2/local/search/keyword.json?");
//        String encoded = URLEncoder.encode("카페", StandardCharsets.UTF_8);
//        assertThat(req.getPath()).contains("query=" + encoded);
//        assertThat(req.getPath()).contains("category_group_code=CE7");
//        assertThat(req.getPath()).contains("y=37.5665");
//        assertThat(req.getPath()).contains("x=126.9780");
//        assertThat(req.getPath()).contains("radius=1000");
//        assertThat(req.getPath()).contains("sort=distance");
//    }
//
//    @Test
//    void GET_coord2address_우리API호출하면_RsData로주소가온다() throws Exception {
//        server.enqueue(new MockResponse()
//                .setResponseCode(200)
//                .addHeader("Content-Type", "application/json; charset=UTF-8")
//                .setBody("""
//                {
//                  "documents": [
//                    {
//                      "road_address": { "address_name": "서울특별시 중구 세종대로 110" },
//                      "address": { "address_name": "서울특별시 중구 태평로1가 31" }
//                    }
//                  ]
//                }
//                """));
//
//        mockMvc.perform(get("/api/v1/location/kakao/coord2address")
//                        .param("lat", "37.5665")
//                        .param("lon", "126.9780"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.status").value(200))
//                .andExpect(jsonPath("$.resultCode").value("OK"))
//                .andExpect(jsonPath("$.msg").value("좌표를 주소로 변환했습니다."))
//                .andExpect(jsonPath("$.data").value("서울특별시 중구 세종대로 110"));
//
//        var req = server.takeRequest();
//        assertThat(req.getMethod()).isEqualTo("GET");
//        assertThat(req.getHeader("Authorization")).isEqualTo("KakaoAK test-key");
//
//        assertThat(req.getPath()).startsWith("/v2/local/geo/coord2address.json?");
//        assertThat(req.getPath()).contains("x=126.978");
//        assertThat(req.getPath()).contains("y=37.5665");
//    }
//}
