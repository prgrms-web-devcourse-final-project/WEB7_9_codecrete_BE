package com.back.web7_9_codecrete_be.domain.concerts.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Slf4j
@Service
public class JsoupApiService {

    public String getHtmlByUrl(String url) {
        StringBuilder sb = new StringBuilder();
        String res = "";
        try {
            Document doc = Jsoup.connect(url).timeout(10*1000).get();
            Element nextData = doc.selectFirst("script#__NEXT_DATA__");
            log.info(doc.toString());
            String json = nextData.html();
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(json);
            String timestamp =
                    root.path("props")
                            .path("pageProps")
                            .path("dehydratedState")
                            .path("queries")
                            .get(0)
                            .path("state")
                            .path("data")
                            .path("availableBeginTimestamp")
                            .asText();
            log.info("timestamp: {}", timestamp);
            sb.append(timestamp);
        } catch (IOException e){
            log.error(e.getMessage());
        }
        return sb.toString();
    }
}
