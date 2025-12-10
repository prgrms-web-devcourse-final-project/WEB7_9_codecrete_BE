package com.back.web7_9_codecrete_be.domain.concerts.service;

import com.back.web7_9_codecrete_be.domain.concerts.repository.ConcertsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ConcertsService {
    private final ConcertsRepository concertsRepository;

}
