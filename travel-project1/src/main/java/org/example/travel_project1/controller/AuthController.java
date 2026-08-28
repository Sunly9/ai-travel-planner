package org.example.travel_project1.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.travel_project1.common.JwtUtils;
import org.example.travel_project1.dto.LoginRequestDTO;
import org.example.travel_project1.dto.RegisterRequestDTO;
import org.example.travel_project1.service.AuthService;
import org.example.travel_project1.vo.Result;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtUtils jwtUtils;

    /**
     * 注册接口
     * POST /api/auth/register
     */
    @PostMapping("/register")
    public Result<Map<String, Object>> register(@Valid @RequestBody RegisterRequestDTO dto) {
        Map<String, Object> data = authService.register(dto);
        return Result.ok(data);
    }

    /**
     * 登录接口
     * POST /api/auth/login
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginRequestDTO dto) {
        Map<String, Object> data = authService.login(dto);
        return Result.ok(data);
    }

    /**
     * 刷新 token
     * POST /api/auth/refresh
     */
    @PostMapping("/refresh")
    public Result<Map<String, Object>> refresh(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String newToken = jwtUtils.generateToken(userId);
        return Result.ok(Map.of("token", newToken));
    }
}