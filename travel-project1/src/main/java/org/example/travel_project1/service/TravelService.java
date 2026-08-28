package org.example.travel_project1.service;


import io.micrometer.common.util.internal.logging.InternalLogger;
import jakarta.annotation.PostConstruct;
import org.example.travel_project1.utils.LLMutils;
import org.example.travel_project1.vo.StreamDoneVO;
import org.example.travel_project1.vo.StreamErrorVO;
import org.example.travel_project1.vo.streamChunkVO;
import org.springframework.beans.factory.annotation.Value;
import org.example.travel_project1.vo.TravelRecommendVO;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.function.Consumer;

@Service
public class TravelService {
    @Value("${llm.api-key}")
    private String apiKey;
    @Value("${llm.base-url}")
    private String baseUrl;
    @Value("${llm.model}")
    private String model;

    private LLMutils llmUtils;
    private ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void init() {
        this.llmUtils = new LLMutils(apiKey, baseUrl, model);
    }

    public TravelRecommendVO recommend (String city, Double budget,Integer days) {
        TravelRecommendVO result = new TravelRecommendVO();

        String prompt = buildTravelPrompt(city, budget, days);
        try{
            String response = llmUtils.chat(null,prompt);
            return parseTravelResponse(response);
        }catch (Exception e){
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

    private String buildTravelPrompt(String city, Double budget,Integer days) {
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
        new Thread(() -> {
            try {
                String prompt = buildTravelPrompt(city, budget, days);
                StringBuilder fullContent = new StringBuilder();

                Consumer<String> callback = content -> {
                    try {
                        fullContent.append(content);
                        String json = objectMapper.writeValueAsString(streamChunkVO.of(content));
                        emitter.send(SseEmitter.event().data(json));
                    } catch (Exception e) {
                        System.out.println("发送推荐chunk失败: " + e);
                    }
                };

                System.out.println("=== 开始调用 LLM 流式生成 ===");
                llmUtils.chatStream(null, prompt, callback);
                System.out.println("=== LLM 流式生成结束，总长度: " + fullContent.length() + " ===");

                // 流结束后，extractJson 清洗掉可能的 markdown 包裹，发 final 事件
                String cleanJson = extractJson(fullContent.toString());
                String finalContent = (cleanJson != null) ? cleanJson : fullContent.toString();
                System.out.println("=== final JSON 长度: " + finalContent.length() + " ===");
                String finalJson = objectMapper.writeValueAsString(
                    java.util.Map.of("type", "final", "content", finalContent)
                );
                emitter.send(SseEmitter.event().data(finalJson));

                String doneJson = objectMapper.writeValueAsString(StreamDoneVO.of());
                emitter.send(SseEmitter.event().data(doneJson));
                emitter.complete();
            } catch (Exception e) {
                System.out.println("=== recommendStream 异常 ===");
                e.printStackTrace();
                try {
                    String errorJson = objectMapper.writeValueAsString(StreamErrorVO.of(e.getMessage()));
                    emitter.send(SseEmitter.event().data(errorJson));
                } catch (Exception e1) {
                    System.out.println("发送错误失败: " + e1);
                }
                emitter.completeWithError(e);
            }
        }).start();
        return emitter;
    }

    public SseEmitter chat(String  message) {
        SseEmitter emitter = new SseEmitter(180000L);
        //发送处理逻辑
        new Thread(() -> {
            try{
                String systemPrompt = "你是一个有好的旅游助手，请用中文回答用户的问题。";
                Consumer<String> callback = content -> {
                    try{
                        String json = objectMapper.writeValueAsString(streamChunkVO.of(content));
                        emitter.send(SseEmitter.event().data(json));
                    }catch (Exception e) {
                        System.out.println("发送消息失败" + e);
                    }
                };
                llmUtils.chatStream(systemPrompt, message, callback);
                //发送完成
                String doneJson = objectMapper.writeValueAsString(StreamDoneVO.of());
                emitter.send(SseEmitter.event().data(doneJson));
                emitter.complete();
            }catch (Exception e) {
                try{
                    String errorJson = objectMapper.writeValueAsString(StreamErrorVO.of(e.getMessage()));
                    emitter.send(SseEmitter.event().data(errorJson));
                }catch (Exception e1) {
                    System.out.println("发送消息失败" + e1);
                }
                emitter.completeWithError(e);
            }

        }).start();
        return emitter;
    }
}
