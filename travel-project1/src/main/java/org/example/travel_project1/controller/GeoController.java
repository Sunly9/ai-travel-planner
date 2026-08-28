package org.example.travel_project1.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.example.travel_project1.vo.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 高德地理编码代理接口
 * 后端持有 key，前端不暴露
 */
@RestController
@RequestMapping("/api/geo")
public class GeoController {

    @Value("${amap.key}")
    private String amapKey;

    // 高德地理编码：地名 → 经纬度（address 和 city 分开传，city 用来限定搜索范围）
    private static final String GEO_URL = "https://restapi.amap.com/v3/geocode/geo?key=%s&address=%s&city=%s";

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 搜索地名获取经纬度
     * GET /api/geo/search?address=故宫&city=北京
     */
    @GetMapping("/search")
    public Result<Map<String, Object>> search(@RequestParam("address") String address,
                                              @RequestParam(value = "city", required = false) String city) {
        if (address == null || address.trim().isEmpty()) {
            return Result.fail(400, "地址不能为空");
        }

        String encodedAddress = URLEncoder.encode(address.trim(), StandardCharsets.UTF_8);
        String encodedCity = city != null && !city.trim().isEmpty()
                ? URLEncoder.encode(city.trim(), StandardCharsets.UTF_8)
                : "";
        String url = String.format(GEO_URL, amapKey, encodedAddress, encodedCity);
        System.out.println("=== 高德地理编码 URL: " + url);

        String resp;
        try {
            resp = httpGet(url);
        } catch (IOException e) {
            return Result.fail(500, "高德地理编码请求失败: " + e.getMessage());
        }

        try {
            JsonNode root = objectMapper.readTree(resp);
            int status = root.path("status").asInt(-1);
            if (status != 1) {
                String info = root.path("info").asText("unknown");
                System.err.println("=== 高德返回失败: status=" + status + ", info=" + info);
                return Result.fail(404, "高德未找到该地点: " + address);
            }
            JsonNode geocodes = root.path("geocodes");
            if (geocodes.isMissingNode() || !geocodes.isArray() || geocodes.isEmpty()) {
                System.err.println("=== 高德 geocodes 为空, address=" + address + ", city=" + city + ", resp=" + resp);
                return Result.fail(404, "未找到地点: " + address);
            }
            JsonNode first = geocodes.get(0);
            // 高德返回的 location 是 "经度,纬度" 字符串（注意顺序：lng,lat）
            String location = first.path("location").asText("");
            String formattedAddr = first.path("formatted_address").asText("");
            System.out.println("=== 高德地理编码结果: " + address + " → " + location + " (" + formattedAddr + ")");

            String[] parts = location.split(",");
            if (parts.length != 2) {
                return Result.fail(500, "坐标解析失败: " + location);
            }
            double lng = Double.parseDouble(parts[0]);
            double lat = Double.parseDouble(parts[1]);

            Map<String, Object> data = new HashMap<>();
            data.put("latitude", lat);
            data.put("longitude", lng);
            data.put("formattedAddress", formattedAddr);
            data.put("province", first.path("province").asText(""));
            return Result.ok(data);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.fail(500, "解析高德响应失败: " + e.getMessage());
        }
    }

    private String httpGet(String url) throws IOException {
        Request request = new Request.Builder().url(url).get().build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("HTTP " + response.code() + ": " + response.message());
            }
            return response.body() != null ? response.body().string() : "";
        }
    }
}
