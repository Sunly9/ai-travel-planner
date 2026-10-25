package org.example.travel_project1.common;

import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.example.travel_project1.vo.Result;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 统一异常处理
 *
 * 以前所有异常都返回 HTTP 200 + body 里的 code，监控看不出来真实错误率。
 * 现在按语义返回真实状态码：
 *   参数校验 / 业务规则  → 400
 *   未登录 / token 失效  → 401（由 JwtAuthFilter 处理）
 *   其它未预期异常      → 500
 * 响应体仍然是统一的 Result 结构，前端不需要改解析方式。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(","));
        return ResponseEntity.badRequest().body(Result.fail(400, message));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Result<Void>> handleIllegalArgument(IllegalArgumentException e,
                                                              HttpServletResponse response) {
        if (response.isCommitted()) {
            return null;
        }
        log.warn("参数异常: {}", e.getMessage());
        return ResponseEntity.badRequest().body(Result.fail(400, e.getMessage()));
    }

    /**
     * 业务异常（"用户名或密码错误""无权访问""行程不存在"…）统一按 400 返回
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Result<Void>> handleRuntimeException(RuntimeException e,
                                                               HttpServletResponse response) {
        if (response.isCommitted()) {
            return null;
        }
        log.warn("业务异常: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Result.fail(400, e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleException(Exception e, HttpServletResponse response) {
        if (response.isCommitted()) {
            return null;
        }
        log.error("服务器内部错误", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.fail(500, "服务器内部错误：" + e.getMessage()));
    }
}
