package com.back.web7_9_codecrete_be.domain.concerts.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.RedisZSetCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

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


    public void addAllAutoCompleteWord(List<String> autoCompleteWords) {
        for (String title : autoCompleteWords) {
            redisTemplate.opsForValue().set(DATE_KEY + title, title);

            for(int i = 0 ;i<title.length();i++){
                for(int j = i+1; j<= title.length();j++ ){
                    String subWord = title.substring(i,j);
                    redisTemplate.opsForZSet().add(INDEX_KEY + subWord, title, 0);
                }
            }
        }
    }

    public List<String> getAutoCompleteWord(String keyword,int start, int end) {
        Set<String> results = redisTemplate.opsForZSet().reverseRange(INDEX_KEY + keyword, start, end);

        return results != null ? new ArrayList<>(results) : Collections.emptyList();
    }

    public void deleteAutoCompleteWords() {
        Set<String> keys = redisTemplate.keys("index:*");
        Set<String> datas = redisTemplate.keys("data:*");
        if (keys != null || !keys.isEmpty()) redisTemplate.delete(keys);
        if (datas != null || !keys.isEmpty()) redisTemplate.delete(datas);
    }
}
