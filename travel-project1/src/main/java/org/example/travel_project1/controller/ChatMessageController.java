package org.example.travel_project1.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.example.travel_project1.entity.ChatMessage;
import org.example.travel_project1.repository.ChatMessageRepository;
import org.example.travel_project1.vo.Result;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat-messages")
@RequiredArgsConstructor
public class ChatMessageController {

    private final ChatMessageRepository chatMessageRepository;

    /**
     * 获取聊天历史
     * GET /api/chat-messages
     */
    @GetMapping
    public Result<List<ChatMessage>> getHistory(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        List<ChatMessage> messages = chatMessageRepository.findByUserIdOrderByCreateTimeAsc(userId);
        return Result.ok(messages);
    }

    /**
     * 保存一条消息
     * POST /api/chat-messages
     */
    @PostMapping
    public Result<ChatMessage> saveMessage(HttpServletRequest request, @RequestBody Map<String, String> body) {
        Long userId = (Long) request.getAttribute("userId");
        ChatMessage msg = new ChatMessage();
        msg.setUserId(userId);
        msg.setRole(body.get("role"));
        msg.setContent(body.get("content"));
        msg.setCreateTime(LocalDateTime.now());
        return Result.ok(chatMessageRepository.save(msg));
    }

    /**
     * 清空聊天历史
     * DELETE /api/chat-messages
     */
    @DeleteMapping
    public Result<Void> clearHistory(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        chatMessageRepository.deleteByUserId(userId);
        return Result.ok();
    }
}
