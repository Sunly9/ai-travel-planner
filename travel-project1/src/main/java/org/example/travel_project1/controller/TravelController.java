package org.example.travel_project1.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.travel_project1.dto.ChatRequestDTO;
import org.example.travel_project1.dto.TravelRequestDTO;
import org.example.travel_project1.service.TravelService;
import org.example.travel_project1.vo.TravelRecommendVO;
import org.springframework.web.bind.annotation.*;
import org.example.travel_project1.vo.Result;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@RestController
@RequestMapping("/api/travel")
@RequiredArgsConstructor
public class TravelController {

    private final TravelService travelService;

    @GetMapping("/hello")
    public Result<String> hello () {
        return Result.ok("hello world");
    }

    @PostMapping("/recommend")
    public Result<TravelRecommendVO> recommend (@Valid @RequestBody TravelRequestDTO travelRequestDTO) {
        log.info("同步生成行程: city={}, days={}, budget={}",
                travelRequestDTO.getCity(), travelRequestDTO.getDays(), travelRequestDTO.getBudget());
        TravelRecommendVO travelRecommendVO = travelService.recommend(travelRequestDTO.getCity(), travelRequestDTO.getBudget(), travelRequestDTO.getDays());
        return Result.ok(travelRecommendVO);
    }

    @PostMapping(value = "/recommend/stream", produces = "text/event-stream")
    public SseEmitter recommendStream (@Valid @RequestBody TravelRequestDTO travelRequestDTO) {
        return travelService.recommendStream(travelRequestDTO.getCity(), travelRequestDTO.getBudget(), travelRequestDTO.getDays());
    }

    @PostMapping(value = "/chat", produces = "text/event-stream")
    public SseEmitter chat (@Valid @RequestBody ChatRequestDTO chatRequestDTO) {
        return travelService.chat(chatRequestDTO.getMessage());

    }


}
