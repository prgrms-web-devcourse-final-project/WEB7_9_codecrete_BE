package com.back.web7_9_codecrete_be.domain.location.controller;

import com.back.web7_9_codecrete_be.domain.location.service.TmapService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/location")
public class TmapController {

    private final TmapService tmapService;

    @GetMapping("/tmap/transit")
    public String getTransit(
            @RequestParam double startX,
            @RequestParam double startY,
            @RequestParam double endX,
            @RequestParam double endY
    ) {
        return tmapService.getRoute(startX, startY, endX, endY);
    }
}