package com.back.web7_9_codecrete_be.domain.concerts.repository;

import com.back.web7_9_codecrete_be.domain.concerts.dto.concert.AutoCompleteItem;
import com.back.web7_9_codecrete_be.domain.concerts.dto.concert.WeightedString;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ConcertSearchRedisTemplate {
    private final RedisTemplate<String,String> redisTemplate;

    private static final String INDEX_KEY = "index:";
    private static final String CONCERT_ID_KEY = "concertId:";

    public void addAllWordsWithWeight(List<WeightedString> weightedStrings) {
        // PipeLine 사용해서 한번에 처리 -> IO 시간 감소
        // 대량의 파이프라인 작업시 Zset 양이 너무 많으면 set 처리가 누락되는 문제 발생 -> 한번에 batch X, 여러번 나누어서 처리
        int batchSize = 50;
        for(int i = 0; i < weightedStrings.size(); i+=batchSize) {
            List<WeightedString> batch = weightedStrings.subList(i, Math.min(i + batchSize, weightedStrings.size()));
            redisTemplate.executePipelined((RedisCallback<?>) connection ->{
                for (WeightedString weightedString : batch) {
                    String id = String.valueOf(weightedString.getConcertId());
                    String word = weightedString.getWord();
                    double score = weightedString.getScore();

                    // 직렬화 도구 명시적 사용 (Template 설정과 일치)
                    byte[] key = redisTemplate.getStringSerializer().serialize(CONCERT_ID_KEY + id);
                    byte[] value = redisTemplate.getStringSerializer().serialize(word);
                    byte[] idByte = redisTemplate.getStringSerializer().serialize(id);

                    // 검색 결과를 ID - 제목 쌍으로 저장
                    connection.commands().set(key,value);

                    // 역 인덱싱
                    for(int k = 0 ;k<word.length();k++){
                        for(int j = k+1;j<= word.length();j++ ){
                            String subWord = word.substring(k,j);
                            // 공백은 검색어 인덱스에서 제외
                            if(subWord.isBlank()) continue;
                            // 서브 문자열을 인덱스 키, 값은 ID 값으로 해서 저장
                            byte[] indexKey = redisTemplate.getStringSerializer().serialize(INDEX_KEY + subWord);
                            connection.zAdd(indexKey, score, idByte);
                        }
                    }
                }
                return null;
            });
        }
    }

    public List<AutoCompleteItem> getAutoCompleteWord(String keyword, int start, int end) {
        Set<String> results = redisTemplate.opsForZSet().reverseRange(INDEX_KEY + keyword, start, end);
        List<String> resultList = new ArrayList<>(results);
        return resultList.stream().map(id ->{
            String name = redisTemplate.opsForValue().get(CONCERT_ID_KEY + id);
            return new AutoCompleteItem(name,Long.valueOf(id));
        }).toList();
    }

    public void deleteAutoCompleteWords() {
        Set<String> keys = redisTemplate.keys("index:*");
        Set<String> concertIdKeys = redisTemplate.keys("concertId:*");
        redisTemplate.delete(keys);
        redisTemplate.delete(concertIdKeys);
        log.info("자동 검색 키워드 삭제: " + keys.size() + "개의 키워드, " + concertIdKeys.size() + "개의 제목이 삭제되었습니다.");
    }

    public Long getConcertIdByName(String concertName) {
        String raw = redisTemplate.opsForValue().get(CONCERT_ID_KEY + concertName);
        return raw == null ? null : Long.parseLong(raw);
    }
}
