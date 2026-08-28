package org.example.travel_project1.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.example.travel_project1.vo.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 天气查询接口 — Open-Meteo 免费 API（无需 Key）
 */
@RestController
@RequestMapping("/api")
public class WeatherController {

    private static final String GEO_URL = "https://geocoding-api.open-meteo.com/v1/search?name=%s&count=1&language=zh&format=json";
    private static final String WEATHER_URL = "https://api.open-meteo.com/v1/forecast?latitude=%s&longitude=%s&daily=weathercode,temperature_2m_max,temperature_2m_min,precipitation_probability_max&timezone=auto&forecast_days=3";

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping("/weather")
    public Result<Map<String, Object>> weather(@RequestParam("city") String city) {
        if (city == null || city.trim().isEmpty()) {
            return Result.fail(400, "城市名不能为空");
        }

        // ===== 1. 根据城市名查经纬度 =====
        String encodedCity = URLEncoder.encode(city.trim(), StandardCharsets.UTF_8);
        String geoUrl = String.format(GEO_URL, encodedCity);
        System.out.println("=== 天气地理编码 URL: " + geoUrl);

        String geoResponse;
        try {
            geoResponse = httpGet(geoUrl);
        } catch (IOException e) {
            return Result.fail(500, "地理编码查询失败: " + e.getMessage());
        }

        double latitude;
        double longitude;
        String cityName;
        String country;
        try {
            JsonNode geoRoot = objectMapper.readTree(geoResponse);
            JsonNode results = geoRoot.path("results");
            if (results.isMissingNode() || !results.isArray() || results.isEmpty()) {
                return Result.fail(404, "未找到城市: " + city);
            }
            JsonNode first = results.get(0);
            latitude = first.path("latitude").asDouble();
            longitude = first.path("longitude").asDouble();
            cityName = first.path("name").asText(city);
            country = first.path("country").asText("");
        } catch (Exception e) {
            return Result.fail(500, "解析地理编码响应失败: " + e.getMessage());
        }

        // ===== 2. 根据经纬度查 3 天天气预报 =====
        String weatherUrl = String.format(WEATHER_URL, latitude, longitude);
        String weatherResponse;
        try {
            weatherResponse = httpGet(weatherUrl);
        } catch (IOException e) {
            return Result.fail(500, "天气查询失败: " + e.getMessage());
        }

        List<Map<String, Object>> forecastList = new ArrayList<>();
        try {
            JsonNode weatherRoot = objectMapper.readTree(weatherResponse);
            JsonNode daily = weatherRoot.path("daily");
            JsonNode times = daily.path("time");
            JsonNode weathercodes = daily.path("weathercode");
            JsonNode tempMax = daily.path("temperature_2m_max");
            JsonNode tempMin = daily.path("temperature_2m_min");
            JsonNode precipProb = daily.path("precipitation_probability_max");

            int days = times.size();
            for (int i = 0; i < days; i++) {
                Map<String, Object> day = new HashMap<>();
                day.put("date", times.get(i).asText());
                int code = weathercodes.get(i).asInt();
                day.put("weatherCode", code);
                day.put("weatherDescription", weatherCodeToZh(code));
                day.put("maxTemperature", tempMax.get(i).asDouble());
                day.put("minTemperature", tempMin.get(i).asDouble());
                JsonNode pp = precipProb.get(i);
                day.put("precipitationProbability", pp.isMissingNode() || pp.isNull() ? null : pp.asInt());
                forecastList.add(day);
            }
        } catch (Exception e) {
            return Result.fail(500, "解析天气数据失败: " + e.getMessage());
        }

        // ===== 3. 组装返回结果 =====
        Map<String, Object> data = new HashMap<>();
        data.put("city", cityName);
        data.put("country", country);
        data.put("latitude", latitude);
        data.put("longitude", longitude);
        data.put("forecast", forecastList);

        return Result.ok(data);
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

    private String weatherCodeToZh(int code) {
        switch (code) {
            case 0: return "晴朗";
            case 1: return "主要晴朗";
            case 2: return "部分多云";
            case 3: return "阴天";
            case 45: return "雾";
            case 48: return "雾凇";
            case 51: return "轻度毛毛雨";
            case 53: return "中度毛毛雨";
            case 55: return "重度毛毛雨";
            case 56: return "轻度冻毛毛雨";
            case 57: return "重度冻毛毛雨";
            case 61: return "小雨";
            case 63: return "中雨";
            case 65: return "大雨";
            case 66: return "轻度冻雨";
            case 67: return "重度冻雨";
            case 71: return "小雪";
            case 73: return "中雪";
            case 75: return "大雪";
            case 77: return "雪粒";
            case 80: return "小阵雨";
            case 81: return "中阵雨";
            case 82: return "大阵雨";
            case 85: return "小阵雪";
            case 86: return "大阵雪";
            case 95: return "雷暴";
            case 96: return "雷暴伴轻度冰雹";
            case 99: return "雷暴伴重度冰雹";
            default: return "未知";
        }
    }
}
