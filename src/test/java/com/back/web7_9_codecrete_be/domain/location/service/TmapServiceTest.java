package com.back.web7_9_codecrete_be.domain.location.service;

import com.back.web7_9_codecrete_be.global.config.WebClientConfig;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        TmapService.class,
        WebClientConfig.class
})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TmapServiceTest {

    private static MockWebServer server;        //가짜 tmap 서버를 테스트에서 공유함

    @Autowired
    private TmapService tmapService;

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry r) {
        try {
            if (server == null) {
                server = new MockWebServer();
                server.start();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        //  tmap API 주소와 키를 Mock서버, 테스트 키로 바꾸기
        r.add("tmap.base-url", () -> server.url("/").toString());      // MockWebServer는 랜덤 포트라서 동적으로 넣어줘야 함
        r.add("tmap.restapi-key", () -> "test-key");
    }
    @AfterAll
    static void shutdown() throws IOException {
        if (server != null) server.shutdown();
    }
    @Test
    void getRoute() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json; charset=UTF-8")
                .setBody("""
        {
          "metaData": {
            "plan": {
              "itineraries": [
                {
                  "totalTime": 600,
                  "totalFare": { "regular": 1250 }
                }
              ]
            }
          }
        }
        """));

        String result = tmapService.getRoute(
                126.977969, 37.566535,
                126.986037, 37.563617
        );

        assertThat(result).contains("totalTime");
        assertThat(result).contains("1250");

        var req = server.takeRequest();
        assertThat(req.getMethod()).isEqualTo("POST");
    }
}
