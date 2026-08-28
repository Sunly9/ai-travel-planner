package org.example.travel_project1.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.travel_project1.dto.ChatRequestDTO;
import org.example.travel_project1.dto.TravelResquestDTO;
import org.example.travel_project1.service.TravelService;
import org.example.travel_project1.vo.TravelRecommendVO;
import org.springframework.web.bind.annotation.*;
import org.example.travel_project1.vo.Result;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

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
    public Result<TravelRecommendVO> recommend (@Valid @RequestBody TravelResquestDTO travelResquestDTO) {
        System.out.println(travelResquestDTO.getCity());
        System.out.println(travelResquestDTO.getDays());
        System.out.println(travelResquestDTO.getBudget());
        TravelRecommendVO travelRecommendVO = travelService.recommend(travelResquestDTO.getCity(), travelResquestDTO.getBudget(),travelResquestDTO.getDays());
        return Result.ok(travelRecommendVO);
    }

    @PostMapping(value = "/recommend/stream", produces = "text/event-stream")
    public SseEmitter recommendStream (@Valid @RequestBody TravelResquestDTO travelResquestDTO) {
        return travelService.recommendStream(travelResquestDTO.getCity(), travelResquestDTO.getBudget(), travelResquestDTO.getDays());
    }

    @PostMapping(value = "/chat", produces = "text/event-stream")
    public SseEmitter chat (@Valid @RequestBody ChatRequestDTO chatRequestDTO) {
        return travelService.chat(chatRequestDTO.getMessage());

    }


}
