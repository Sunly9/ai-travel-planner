package org.example.travel_project1.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * SiliconFlow（OpenAI 兼容协议）调用封装
 * 支持同步对话与 SSE 流式对话；流式支持外部取消（客户端断开时中止上游请求）
 */
@Slf4j
public class LLMutils {

    private final String apiKey;
    private final String baseUrl;
    private final String model;
    private final OkHttpClient client;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public LLMutils(String apiKey, String baseUrl, String model) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.model = model;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(600, TimeUnit.SECONDS)   // 流式输出可能很久
                .writeTimeout(120, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();
    }

    /** 同步对话 */
    public String chat(String systemPrompt, String userPrompt) {
        String requestBody = buildRequstBody(systemPrompt, userPrompt, false);
        Request request = new Request.Builder()
                .url(baseUrl + "/chat/completions")
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + apiKey)
                .post(RequestBody.create(requestBody, MediaType.parse("application/json;charset=utf-8")))
                .build();
        long start = System.currentTimeMillis();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "no body";
                log.warn("LLM 同步调用失败: code={}, body={}", response.code(), errorBody);
                throw new IOException(friendlyMessage(response.code()));
            }
            String responseBody = response.body().string();
            log.info("LLM 同步调用完成, 耗时 {} ms", System.currentTimeMillis() - start);
            return extractContent(responseBody);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String extractContent(String responseBody) throws IOException {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode choices = root.path("choices");
        if (choices.isArray() && choices.size() > 0) {
            return choices.get(0).get("message").get("content").asText();
        }
        return "";
    }

    public String buildRequstBody(String systemPrompt, String userPrompt, Boolean stream) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            com.fasterxml.jackson.databind.node.ObjectNode root = mapper.createObjectNode();
            root.put("model", model);
            root.put("stream", stream);
            root.put("temperature", 0.7);

            com.fasterxml.jackson.databind.node.ArrayNode messages = root.putArray("messages");
            if (systemPrompt != null && !systemPrompt.isEmpty()) {
                com.fasterxml.jackson.databind.node.ObjectNode sysMsg = mapper.createObjectNode();
                sysMsg.put("role", "system");
                sysMsg.put("content", systemPrompt);
                messages.add(sysMsg);
            }
            com.fasterxml.jackson.databind.node.ObjectNode userMsg = mapper.createObjectNode();
            userMsg.put("role", "user");
            userMsg.put("content", userPrompt);
            messages.add(userMsg);

            return mapper.writeValueAsString(root);
        } catch (Exception e) {
            throw new RuntimeException("构建请求体失败", e);
        }
    }

    public String chatStream(String systemPrompt, String userPrompt, Consumer<String> callback) throws IOException {
        return chatStream(systemPrompt, userPrompt, callback, () -> false);
    }

    /**
     * 流式对话
     *
     * @param cancelled 由调用方提供：返回 true 时立即取消上游请求（比如用户关了页面）
     * @return 完整内容（已拼接）
     */
    public String chatStream(String systemPrompt, String userPrompt, Consumer<String> callback,
                             BooleanSupplier cancelled) throws IOException {
        String requestBody = buildRequstBody(systemPrompt, userPrompt, true);
        Request request = new Request.Builder()
                .url(baseUrl + "/chat/completions")
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Accept", "text/event-stream")
                .post(RequestBody.create(requestBody, MediaType.parse("application/json;charset=utf-8")))
                .build();

        StringBuilder fullContent = new StringBuilder();
        long start = System.currentTimeMillis();
        Call call = client.newCall(request);

        try (Response response = call.execute()) {
            log.info("LLM 流式响应码: {}", response.code());
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "no body";
                log.warn("LLM 流式调用失败: code={}, body={}", response.code(), errorBody);
                throw new IOException(friendlyMessage(response.code()));
            }
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(response.body().byteStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (cancelled.getAsBoolean()) {
                        log.info("客户端已断开，取消上游 LLM 请求（已接收 {} 字符）", fullContent.length());
                        call.cancel();
                        break;
                    }
                    if (!line.startsWith("data: ")) {
                        continue;
                    }
                    String data = line.substring(6);
                    if ("[DONE]".equals(data)) {
                        break;
                    }
                    String content = parseStreamContent(data);
                    if (content != null && !content.isEmpty()) {
                        fullContent.append(content);
                        callback.accept(content);
                    }
                }
            }
        }

        log.info("LLM 流式结束, 总长度 {} 字符, 耗时 {} ms",
                fullContent.length(), System.currentTimeMillis() - start);
        return fullContent.toString();
    }

    /** 把供应商的 HTTP 状态码翻译成用户能看懂的一句话 */
    private String friendlyMessage(int code) {
        return switch (code) {
            case 401 -> "AI 服务鉴权失败：API Key 无效或已过期，请检查配置";
            case 402, 403 -> "AI 服务额度不足：请更换有可用额度的 API Key，"
                    + "或改用免费模型（见 README「切换模型供应商」）";
            case 429 -> "AI 服务请求太频繁，请稍后再试";
            default -> "AI 服务暂时不可用（HTTP " + code + "），请稍后再试";
        };
    }

    /** 解析一条 SSE data 里的增量内容 */
    private String parseStreamContent(String data) {
        try {
            JsonNode root = objectMapper.readTree(data);
            JsonNode choices = root.path("choices");
            if (choices.isArray() && choices.size() > 0) {
                JsonNode delta = choices.get(0).path("delta");
                return delta.path("content").asText("");
            }
        } catch (Exception e) {
            log.debug("解析流式数据失败: {}", e.getMessage());
        }
        return null;
    }
}
