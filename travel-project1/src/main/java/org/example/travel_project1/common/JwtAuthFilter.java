package org.example.travel_project1.common;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;


@Component
@RequiredArgsConstructor
public class JwtAuthFilter implements Filter {

    private final JwtUtils jwtUtils;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;


        // 放行 OPTIONS 预检请求（CORS）
        if ("OPTIONS".equalsIgnoreCase(req.getMethod())) {
            chain.doFilter(req, res);
            return;
        }

        // 放行不需要登录的接口
        String path = req.getRequestURI();
        if (path.equals("/api/auth/login") || path.equals("/api/auth/register")
                || path.startsWith("/api/travel/") || path.startsWith("/api/weather")
                || path.startsWith("/api/geo/")) {
            chain.doFilter(req, res);
            return;
        }

        //从请求头获取token
        String authHeader = req.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            res.setStatus(401);
            res.setContentType("application/json;charset=utf-8");
            res.getWriter().write("{\"success\":false,\"code\":401,\"message\":\"没登录\"}");
            return;
        }

        String token = authHeader.substring(7);
        Long userId = jwtUtils.getUserIdFromToken(token);

        if (userId == null) {
            res.setStatus(401);
            res.setContentType("application/json;charset=utf-8");
            res.getWriter().write("{\"success\":false,\"code\":401,\"message\":\"token无效或已过期\"}");
            return;
        }

        req.setAttribute("userId", userId);
        chain.doFilter(request, response);

    }
}
