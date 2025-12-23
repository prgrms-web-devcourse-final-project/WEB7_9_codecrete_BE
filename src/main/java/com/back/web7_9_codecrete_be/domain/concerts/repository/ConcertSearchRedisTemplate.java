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
    private static final String DATE_KEY  = "data:";
    private static final String CONCERT_ID_KEY = "concertName:";

    public void addAllWordsWithWeight(List<WeightedString> weightedStrings) {
        // PipeLine 사용해서 한번에 처리 -> IO 시간 감소
        redisTemplate.executePipelined((RedisCallback<?>) connection ->{
            for (WeightedString weightedString : weightedStrings) {
                String id = String.valueOf(weightedString.getConcertId());
                String word = weightedString.getWord();
                double score = weightedString.getScore();

                // 개별 문자들에 대한 키-값 설정
                connection.commands().set((CONCERT_ID_KEY + word).getBytes(StandardCharsets.UTF_8), id.getBytes(StandardCharsets.UTF_8));

                for(int i = 0 ;i<word.length();i++){
                    for(int j = i+1;j<= word.length();j++ ){
                        String subWord = word.substring(i,j);

                        // 공백은 검색어 인덱스에서 제외
                        if(subWord.isBlank()) continue;

                        byte[] indexKey = (INDEX_KEY + subWord).getBytes(StandardCharsets.UTF_8);
                        connection.zAdd(indexKey,score,word.getBytes(StandardCharsets.UTF_8));
                    }
                }
            }
            return null;
        });
    }

    public List<AutoCompleteItem> getAutoCompleteWord(String keyword, int start, int end) {
        Set<String> results = redisTemplate.opsForZSet().reverseRange(INDEX_KEY + keyword, 0, 9);
        List<String> resultList = new ArrayList<>(results);
        return resultList.stream().map(name ->{
            Long id = Long.valueOf(redisTemplate.opsForValue().get(CONCERT_ID_KEY + name));
            return new AutoCompleteItem(name,id);
        }).toList();
    }

    public void deleteAutoCompleteWords() {
        Set<String> keys = redisTemplate.keys("index:*");
        Set<String> datas = redisTemplate.keys("data:*");
        if (keys != null || !keys.isEmpty()) redisTemplate.delete(keys);
        if (datas != null || !keys.isEmpty()) redisTemplate.delete(datas);
    }

    public Long getConcertIdByName(String concertName) {
        String raw = redisTemplate.opsForValue().get(CONCERT_ID_KEY + concertName);
        return raw == null ? null : Long.parseLong(raw);
    }
}
