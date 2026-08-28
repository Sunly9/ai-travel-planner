package org.example.travel_project1.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.example.travel_project1.entity.User;
import org.example.travel_project1.repository.ChatMessageRepository;
import org.example.travel_project1.repository.FavoriteRepository;
import org.example.travel_project1.repository.TravelPlanRepository;
import org.example.travel_project1.service.AuthService;
import org.example.travel_project1.vo.Result;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final AuthService authService;
    private final TravelPlanRepository travelPlanRepository;
    private final FavoriteRepository favoriteRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    /**
     * 获取个人信息
     * GET /api/user/profile
     */
    @GetMapping("/profile")
    public Result<Map<String, Object>> getProfile(HttpServletRequest request) {
        // 从请求属性中取出 userId（JwtAuthFilter 存进去的）
        Long userId = (Long) request.getAttribute("userId");
        User user = authService.getUserById(userId);

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("nickname", user.getNickname());
        userInfo.put("phone", user.getPhone());
        userInfo.put("avatar", user.getAvatar());

        return Result.ok(userInfo);
    }

    /**
     * 更新个人信息
     * PUT /api/user/profile
     */
    @PutMapping("/profile")
    public Result<Map<String, Object>> updateProfile(HttpServletRequest request,
                                                      @RequestBody Map<String, String> body) {
        Long userId = (Long) request.getAttribute("userId");
        User user = authService.getUserById(userId);

        // 只更新传了的字段
        if (body.containsKey("nickname")) {
            user.setNickname(body.get("nickname"));
        }
        if (body.containsKey("phone")) {
            user.setPhone(body.get("phone"));
        }
        if (body.containsKey("avatar")) {
            user.setAvatar(body.get("avatar"));
        }
        user.setUpdateTime(java.time.LocalDateTime.now());
        authService.updateUser(user);

        // 返回更新后的信息
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("nickname", user.getNickname());
        userInfo.put("phone", user.getPhone());
        userInfo.put("avatar", user.getAvatar());

        return Result.ok(userInfo);
    }

    /**
     * 修改密码
     * PUT /api/user/password
     */
    @PutMapping("/password")
    public Result<?> changePassword(HttpServletRequest request,
                                    @RequestBody Map<String, String> body) {
        Long userId = (Long) request.getAttribute("userId");
        User user = authService.getUserById(userId);

        String oldPassword = body.get("oldPassword");
        String newPassword = body.get("newPassword");

        if (oldPassword == null || newPassword == null) {
            return Result.fail(400, "旧密码和新密码不能为空");
        }

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            return Result.fail(400, "旧密码不正确");
        }

        if (newPassword.length() < 6) {
            return Result.fail(400, "新密码至少6位");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdateTime(java.time.LocalDateTime.now());
        authService.updateUser(user);

        return Result.ok(null);
    }

    /**
     * 数据统计
     * GET /api/user/stats
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> getStats(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");

        Map<String, Object> stats = new HashMap<>();
        stats.put("planCount", travelPlanRepository.countByUserId(userId));
        stats.put("favoriteCount", favoriteRepository.countByUserId(userId));
        stats.put("chatCount", chatMessageRepository.countByUserId(userId));

        return Result.ok(stats);
    }
}