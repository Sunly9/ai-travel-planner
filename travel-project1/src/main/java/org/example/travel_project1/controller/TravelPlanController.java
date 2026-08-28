package org.example.travel_project1.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.travel_project1.dto.SavePlanDTO;
import org.example.travel_project1.entity.TravelPlan;
import org.example.travel_project1.repository.TravelPlanRepository;
import org.example.travel_project1.vo.Result;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/plans")
@RequiredArgsConstructor
public class TravelPlanController {

    private final TravelPlanRepository travelPlanRepository;

    /**
     * 保存行程
     * POST /api/plans
     */
    @PostMapping
    public Result<Map<String, Object>> savePlan(HttpServletRequest request, @Valid @RequestBody SavePlanDTO dto) {
        Long userId = (Long) request.getAttribute("userId");

        TravelPlan plan = new TravelPlan();
        plan.setUserId(userId);
        plan.setCity(dto.getCity());
        plan.setDays(dto.getDays());
        plan.setBudget(dto.getBudget());
        plan.setPlanData(dto.getPlanData());
        plan.setCreateTime(LocalDateTime.now());

        TravelPlan saved = travelPlanRepository.save(plan);

        Map<String, Object> result = new HashMap<>();
        result.put("id", saved.getId());
        result.put("city", saved.getCity());
        result.put("days", saved.getDays());
        result.put("createTime", saved.getCreateTime());
        return Result.ok(result);
    }

    /**
     * 获取当前用户的所有行程
     * GET /api/plans
     */
    @GetMapping
    public Result<List<TravelPlan>> listPlans(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        List<TravelPlan> plans = travelPlanRepository.findByUserIdOrderByCreateTimeDesc(userId);
        return Result.ok(plans);
    }

    /**
     * 获取行程详情
     * GET /api/plans/{id}
     */
    @GetMapping("/{id}")
    public Result<TravelPlan> getPlan(HttpServletRequest request, @PathVariable Long id) {
        Long userId = (Long) request.getAttribute("userId");
        TravelPlan plan = travelPlanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("行程不存在"));
        if (!plan.getUserId().equals(userId)) {
            throw new RuntimeException("无权访问");
        }
        return Result.ok(plan);
    }

    /**
     * 更新行程
     * PUT /api/plans/{id}
     */
    @PutMapping("/{id}")
    public Result<TravelPlan> updatePlan(HttpServletRequest request, @PathVariable Long id, @Valid @RequestBody SavePlanDTO dto) {
        Long userId = (Long) request.getAttribute("userId");
        TravelPlan plan = travelPlanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("行程不存在"));
        if (!plan.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作");
        }
        plan.setCity(dto.getCity());
        plan.setDays(dto.getDays());
        plan.setBudget(dto.getBudget());
        plan.setPlanData(dto.getPlanData());
        plan.setUpdateTime(LocalDateTime.now());
        TravelPlan updated = travelPlanRepository.save(plan);
        return Result.ok(updated);
    }

    /**
     * 删除行程
     * DELETE /api/plans/{id}
     */
    @DeleteMapping("/{id}")
    public Result<Void> deletePlan(HttpServletRequest request, @PathVariable Long id) {
        Long userId = (Long) request.getAttribute("userId");
        TravelPlan plan = travelPlanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("行程不存在"));
        if (!plan.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作");
        }
        travelPlanRepository.delete(plan);
        return Result.ok();
    }
}
