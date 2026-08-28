package org.example.travel_project1.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.example.travel_project1.entity.Favorite;
import org.example.travel_project1.repository.FavoriteRepository;
import org.example.travel_project1.vo.Result;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteRepository favoriteRepository;

    /**
     * 添加收藏
     * POST /api/favorites
     */
    @PostMapping
    public Result<Favorite> addFavorite(HttpServletRequest request, @RequestBody Map<String, String> body) {
        Long userId = (Long) request.getAttribute("userId");
        String spotName = body.get("spotName");
        if (favoriteRepository.existsByUserIdAndSpotName(userId, spotName)) {
            return Result.fail(400, "已收藏过");
        }
        Favorite fav = new Favorite();
        fav.setUserId(userId);
        fav.setSpotName(spotName);
        fav.setSpotData(body.get("spotData"));
        fav.setCreateTime(LocalDateTime.now());
        return Result.ok(favoriteRepository.save(fav));
    }

    /**
     * 取消收藏
     * DELETE /api/favorites
     */
    @DeleteMapping
    public Result<Void> removeFavorite(HttpServletRequest request, @RequestBody Map<String, String> body) {
        Long userId = (Long) request.getAttribute("userId");
        favoriteRepository.deleteByUserIdAndSpotName(userId, body.get("spotName"));
        return Result.ok();
    }

    /**
     * 获取收藏列表
     * GET /api/favorites
     */
    @GetMapping
    public Result<List<Favorite>> listFavorites(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.ok(favoriteRepository.findByUserIdOrderByCreateTimeDesc(userId));
    }

    /**
     * 检查是否已收藏
     * GET /api/favorites/check?spotName=xxx
     */
    @GetMapping("/check")
    public Result<Boolean> isFavorited(HttpServletRequest request, @RequestParam String spotName) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.ok(favoriteRepository.existsByUserIdAndSpotName(userId, spotName));
    }
}
