package org.example.travel_project1.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.travel_project1.utils.LLMutils;
import org.example.travel_project1.vo.StreamDoneVO;
import org.example.travel_project1.vo.StreamErrorVO;
import org.example.travel_project1.vo.TravelRecommendVO;
import org.example.travel_project1.vo.StreamChunkVO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

@Slf4j
@Service
@RequiredArgsConstructor
public class TravelService {

    /** SSE 专用线程池（见 AsyncConfig），替代原来每个请求 new Thread() */
    private final ExecutorService sseExecutor;

    @Value("${llm.api-key}")
    private String apiKey;
    @Value("${llm.base-url}")
    private String baseUrl;
    @Value("${llm.model}")
    private String model;

    private LLMutils llmUtils;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void init() {
        this.llmUtils = new LLMutils(apiKey, baseUrl, model);
    }

    /** 同步推荐（不带流式，保留给外部/调试用） */
    public TravelRecommendVO recommend(String city, Double budget, Integer days) {
        String prompt = buildTravelPrompt(city, budget, days);
        try {
            String response = llmUtils.chat(null, prompt);
            return parseTravelResponse(response);
        } catch (Exception e) {
            log.error("旅游推荐失败: {}", e.getMessage());
            TravelRecommendVO result = new TravelRecommendVO();
            result.setSuccess(false);
            result.setError("旅游推荐失败");
            return result;
        }
    }

    // service旅游推荐返回数据处理
    private TravelRecommendVO parseTravelResponse(String response) {
        TravelRecommendVO result = new TravelRecommendVO();

        try {
            String jsonContent = extractJson(response);
            if (jsonContent != null) {
                result = objectMapper.readValue(jsonContent, TravelRecommendVO.class);
            } else {
                result.setSuccess(false);
                result.setError("未能从响应中提取JSON");
                result.setRawResponse(response);
            }
        } catch (Exception e) {
            result.setSuccess(false);
            result.setError("JSON解析失败");
            result.setRawResponse(response);
        }

        return result;
    }

    private String extractJson(String response) {
        if (response == null || response.isEmpty()) {
            return null;
        }

        String[] patterns = {
                "```json\\n([\\s\\S]*?)\\n```",
                "```\\n([\\s\\S]*?)\\n```"
        };

        for (String pattern : patterns) {
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = p.matcher(response);
            if (m.find()) {
                return m.group(1);
            }
        }

        int start = response.indexOf('{');
        int end = response.lastIndexOf('}');
        if (start != -1 && end != -1 && end > start) {
            return response.substring(start, end + 1);
        }

        return null;
    }

    private String buildTravelPrompt(String city, Double budget, Integer days) {
        return "你是一位资深旅游规划专家，请为用户量身定制一份详细的旅游行程规划。\n\n" +
                "【用户需求】\n" +
                "目的地：" + city + "\n" +
                "总预算：" + budget + "元（含住宿、餐饮、交通、门票等所有费用）\n" +
                "旅游天数：" + days + "天\n\n" +
                "请直接输出纯JSON（不要markdown代码块标记，不要添加任何解释文字），格式如下：\n" +
                "{\"success\":true,\"city\":\"" + city + "\",\"days\":" + days + ",\"totalBudget\":" + budget + "," +
                "\"dailyItinerary\":[{\"day\":1,\"date\":\"第1天\"," +
                "\"morning\":{\"spot\":\"景点名称\",\"duration\":\"2小时\",\"ticket\":\"门票价格\",\"transportation\":\"交通方式\",\"description\":\"景点详细介绍，2-3句话\"}," +
                "\"afternoon\":{\"spot\":\"景点名称\",\"duration\":\"2小时\",\"ticket\":\"门票价格\",\"transportation\":\"交通方式\",\"description\":\"景点详细介绍，2-3句话\"}," +
                "\"evening\":{\"spot\":\"活动/景点\",\"duration\":\"1-2小时\",\"ticket\":\"门票价格\",\"transportation\":\"交通方式\",\"description\":\"活动介绍\"}}]," +
                "\"budgetBreakdown\":{\"accommodation\":住宿费用,\"food\":餐饮费用,\"transportation\":交通费用,\"tickets\":门票费用,\"other\":\"其他费用\"}," +
                "\"tips\":[\"实用提示1\",\"实用提示2\",\"实用提示3\"]," +
                "\"warnings\":[\"注意事项1\",\"注意事项2\"]}\n\n" +
                "【填写要求】\n" +
                "- 每天上午、下午、晚上各安排一个景点或活动\n" +
                "- description 字段要写得详细、有吸引力\n" +
                "- budgetBreakdown 的各项费用总和应接近 totalBudget\n" +
                "- tips 和 warnings 各输出2-4条实用信息\n" +
                "- 确保JSON格式正确，字段完整";
    }

    /**
     * 流式推荐 — SSE 实时返回 AI 生成内容
     */
    public SseEmitter recommendStream(String city, Double budget, Integer days) {
        SseEmitter emitter = new SseEmitter(600000L);
        AtomicBoolean cancelled = newCancelledFlag(emitter);

        submit(emitter, () -> {
            String prompt = buildTravelPrompt(city, budget, days);
            StringBuilder fullContent = new StringBuilder();

            Consumer<String> callback = content -> {
                if (cancelled.get()) {
                    return;
                }
                try {
                    fullContent.append(content);
                    emitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(StreamChunkVO.of(content))));
                } catch (Exception e) {
                    log.debug("发送推荐 chunk 失败（客户端可能已断开）: {}", e.getMessage());
                }
            };

            try {
                llmUtils.chatStream(null, prompt, callback, cancelled::get);
            } catch (Exception e) {
                log.error("生成行程失败", e);
                sendError(emitter, e.getMessage());
                emitter.completeWithError(e);
                return;
            }

            if (cancelled.get()) {
                log.info("客户端已断开，跳过 final 事件");
                return;
            }

            try {
                // 流结束后清洗掉可能的 markdown 包裹
                String cleanJson = extractJson(fullContent.toString());
                String finalContent = (cleanJson != null) ? cleanJson : fullContent.toString();
                log.info("行程 JSON 生成完成, 长度 {}", finalContent.length());
                emitter.send(SseEmitter.event().data(
                        objectMapper.writeValueAsString(Map.of("type", "final", "content", finalContent))));
                emitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(StreamDoneVO.of())));
                emitter.complete();
            } catch (Exception e) {
                log.error("发送 final 事件失败", e);
                sendError(emitter, e.getMessage());
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    /** AI 对话（流式） */
    public SseEmitter chat(String message) {
        SseEmitter emitter = new SseEmitter(180000L);
        AtomicBoolean cancelled = newCancelledFlag(emitter);

        submit(emitter, () -> {
            String systemPrompt = "你是一个友好的旅游助手，请用中文回答用户的问题。";
            Consumer<String> callback = content -> {
                if (cancelled.get()) {
                    return;
                }
                try {
                    emitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(StreamChunkVO.of(content))));
                } catch (Exception e) {
                    log.debug("发送对话 chunk 失败（客户端可能已断开）: {}", e.getMessage());
                }
            };

            try {
                llmUtils.chatStream(systemPrompt, message, callback, cancelled::get);
                if (cancelled.get()) {
                    return;
                }
                emitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(StreamDoneVO.of())));
                emitter.complete();
            } catch (Exception e) {
                log.error("AI 对话失败", e);
                sendError(emitter, e.getMessage());
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    /** 注册客户端断开/超时回调，置位后可中止上游请求 */
    private AtomicBoolean newCancelledFlag(SseEmitter emitter) {
        AtomicBoolean cancelled = new AtomicBoolean(false);
        emitter.onCompletion(() -> cancelled.set(true));
        emitter.onTimeout(() -> {
            cancelled.set(true);
            emitter.complete();
        });
        emitter.onError(e -> cancelled.set(true));
        return cancelled;
    }

    /** 提交到 SSE 线程池；池满时直接回一个错误事件，而不是把请求挂死 */
    private void submit(SseEmitter emitter, Runnable task) {
        try {
            sseExecutor.execute(task);
        } catch (RejectedExecutionException e) {
            log.warn("SSE 线程池已满，拒绝请求");
            sendError(emitter, "服务繁忙，请稍后再试");
            emitter.complete();
        }
    }

    private void sendError(SseEmitter emitter, String message) {
        try {
            emitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(StreamErrorVO.of(message))));
        } catch (Exception ignored) {
            // 客户端已断开，忽略
        }
    }
}
