package com.back.web7_9_codecrete_be.domain.concerts.service;

import com.back.web7_9_codecrete_be.domain.concerts.entity.ConcertKopisApiLog;
import com.back.web7_9_codecrete_be.domain.concerts.repository.ConcertKopisApiLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ConcertKopisApiLogService {
    private final ConcertKopisApiLogRepository concertKopisApiLogRepository;

    public void saveErrorLog(String workType,Exception e,Long index) {
        ConcertKopisApiLog concertKopisApiLog = new ConcertKopisApiLog(workType,"fail", e.getMessage(), index);
        concertKopisApiLogRepository.save(concertKopisApiLog);
    }

    public void saveSuccessLog(String workType,String description,Long index) {
        ConcertKopisApiLog concertKopisApiLog = new ConcertKopisApiLog(workType,"success", description, index);
        concertKopisApiLogRepository.save(concertKopisApiLog);
    }

    public void saveStartLog(String workType,String description,Long index) {
        ConcertKopisApiLog concertKopisApiLog = new ConcertKopisApiLog(workType,"start", description, index);
        concertKopisApiLogRepository.save(concertKopisApiLog);
    }

    public boolean isInitComplete() {
        return concertKopisApiLogRepository.existsConcertKopisApiLogByStatusAndWorkType("success","save");
    }

    public LocalDateTime getLastSaveTime(){
        return concertKopisApiLogRepository.getLastSaveTime();
    }

    public Long getLastSaveFailIndex(){
        return concertKopisApiLogRepository.getLastFailIndex().orElse(0L);
    }

}
