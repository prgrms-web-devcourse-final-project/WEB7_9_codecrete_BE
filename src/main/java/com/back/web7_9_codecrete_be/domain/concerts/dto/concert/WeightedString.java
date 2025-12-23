package com.back.web7_9_codecrete_be.domain.concerts.dto.concert;

import com.back.web7_9_codecrete_be.domain.concerts.entity.Concert;
import lombok.Getter;

@Getter
public class WeightedString {
    private Long concertId;
    private String word;
    private double score;

    public WeightedString(String word, int score) {
        this.word = word;
        this.score = score;
    }

    public WeightedString(Concert concert) {
        this.concertId = concert.getConcertId();
        this.word = concert.getName();
        this.score = ((double)concert.getViewCount()) * 0.1;
    }


}
