package com.back.web7_9_codecrete_be.domain.concerts.dto.concert;

import lombok.Getter;

@Getter
public class AutoCompleteItem {
    private String name;
    private Long Id;

    public AutoCompleteItem(String name, Long id) {
        this.name = name;
        Id = id;
    }
}
