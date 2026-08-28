package org.example.travel_project1.utils;

import okhttp3.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class LLMutils {
    private String apiKey;
    private String baseUrl;
    private String model;
    private OkHttpClient client;
    private ObjectMapper objectMapper = new ObjectMapper();

    public LLMutils(String apiKey, String baseUrl, String model) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.model = model;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(600, TimeUnit.SECONDS)
                .writeTimeout(120, TimeUnit.SECONDS)
                .build();
    }

    public String chat(String systemPrompt, String userPrompt) {

        String requestBody = buildRequstBody(systemPrompt, userPrompt, false);
//        System.out.println(requestBody);
        // 创建一个post请求
        Request request = new Request.Builder()
                .url(baseUrl + "/chat/completions")
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + apiKey)
                .post(RequestBody.create(requestBody, MediaType.parse("application/json;charset=utf-8")))
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("LLM调用异常: " + response.code());
            }
//            System.out.println(response.body().string());
            String responseBody = response.body().string();
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
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
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
        String requestBody = buildRequstBody(systemPrompt, userPrompt, true);
        System.out.println("=== LLM 请求 URL: " + baseUrl + "/chat/completions ===");
        System.out.println("=== LLM 请求体: " + requestBody.substring(0, Math.min(200, requestBody.length())) + "... ===");
        //创建一个请求
        Request request = new Request.Builder()
                .url(baseUrl + "/chat/completions")
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Accept", "text/event-stream")
                .post(RequestBody.create(requestBody, MediaType.parse("application/json;charset=utf-8")))
                .build();
        //记录完整的内容
        StringBuilder fullContent = new StringBuilder();
        try(Response response = client.newCall(request).execute()){
            System.out.println("=== LLM 响应码: " + response.code() + " ===");
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "no body";
                System.out.println("=== LLM 错误响应: " + errorBody + " ===");
                throw new IOException("LLM调用异常: " + response.code());

        }
            try(BufferedReader reader = new BufferedReader(new InputStreamReader(response.body().byteStream(), StandardCharsets.UTF_8))){
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("data: ")) {
                        //截取字符串
                        String data = line.substring(6);
                        if ("[DONE]".equals(data)){
                            break;
                        }
                        String content = parseStreamContent(data);
                        if (content != null && !content.isEmpty()) {
                            fullContent.append(content);
                            if (content!=null) {
                                callback.accept(content);
                            }
                        }

                    }
                }
            }

            }
        return fullContent.toString();

    }

    // 大模型返回对话流数据处理函数
    private String parseStreamContent(String data) {
        try {
            JsonNode root = objectMapper.readTree(data);
            JsonNode choices = root.path("choices");
            if (choices.isArray() && choices.size() > 0) {
                JsonNode delta = choices.get(0).path("delta");
                return delta.path("content").asText("");
            }
        } catch (Exception e) {
            System.out.println("解析流式数据失败: {}" + e.getMessage());
        }
        return null;
    }
}