package org.example.travel_project1.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.example.travel_project1.entity.GeoCache;
import org.example.travel_project1.repository.GeoCacheRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * 高德地理编码 + 本地缓存
 *
 * 命中规则：同一个 (address, city) 在 cache-ttl-days 天内直接读库，不再请求高德。
 * 这样重复打开同一份行程时，景点坐标是零外网调用的。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GeoService {

    private static final String GEO_URL =
            "https://restapi.amap.com/v3/geocode/geo?key=%s&address=%s&city=%s";

    private final GeoCacheRepository geoCacheRepository;

    @Value("${amap.key}")
    private String amapKey;

    @Value("${geo.cache-ttl-days:30}")
    private long cacheTtlDays;

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 坐标结果（fromCache 供日志/排查用） */
    public record GeoPoint(double latitude, double longitude,
                           String formattedAddress, String province, boolean fromCache) {
    }

    /**
     * 解析地名坐标，优先读缓存。
     *
     * @return null 表示高德明确找不到该地点；IOException 表示网络/接口异常
     */
    public GeoPoint lookup(String rawAddress, String rawCity) throws IOException {
        String address = rawAddress == null ? "" : rawAddress.trim();
        String city = rawCity == null ? "" : rawCity.trim();
        if (address.isEmpty()) {
            return null;
        }

        GeoCache cached = geoCacheRepository.findByAddressAndCity(address, city).orElse(null);
        if (cached != null && cached.getLatitude() != null && cached.getLongitude() != null
                && cached.getUpdatedAt() != null
                && cached.getUpdatedAt().isAfter(LocalDateTime.now().minusDays(cacheTtlDays))) {
            log.debug("地理编码命中缓存: {} ({})", address, city);
            return new GeoPoint(cached.getLatitude(), cached.getLongitude(),
                    cached.getFormattedAddress(), cached.getProvince(), true);
        }

        GeoPoint fresh = fetchFromAmap(address, city);
        if (fresh == null) {
            return null;
        }

        // 有旧记录就更新，没有就新建（避免唯一键冲突）
        GeoCache row = cached != null ? cached : new GeoCache();
        row.setAddress(address);
        row.setCity(city);
        row.setLatitude(fresh.latitude());
        row.setLongitude(fresh.longitude());
        row.setFormattedAddress(fresh.formattedAddress());
        row.setProvince(fresh.province());
        row.setUpdatedAt(LocalDateTime.now());
        try {
            geoCacheRepository.save(row);
        } catch (Exception e) {
            // 并发下可能撞唯一键，缓存写失败不影响本次返回
            log.warn("地理编码缓存写入失败: {}", e.getMessage());
        }
        return fresh;
    }

    private GeoPoint fetchFromAmap(String address, String city) throws IOException {
        String encodedAddress = URLEncoder.encode(address, StandardCharsets.UTF_8);
        String encodedCity = city.isEmpty() ? "" : URLEncoder.encode(city, StandardCharsets.UTF_8);
        String url = String.format(GEO_URL, amapKey, encodedAddress, encodedCity);
        log.debug("高德地理编码请求: {} ({})", address, city);

        String resp = httpGet(url);

        JsonNode root = objectMapper.readTree(resp);
        int status = root.path("status").asInt(-1);
        if (status != 1) {
            log.warn("高德返回失败: status={}, info={}", status, root.path("info").asText("unknown"));
            return null;
        }
        JsonNode geocodes = root.path("geocodes");
        if (!geocodes.isArray() || geocodes.isEmpty()) {
            log.info("高德未找到地点: {}", address);
            return null;
        }
        JsonNode first = geocodes.get(0);
        // 高德返回 "经度,纬度"
        String location = first.path("location").asText("");
        String[] parts = location.split(",");
        if (parts.length != 2) {
            log.warn("坐标格式异常: {}", location);
            return null;
        }
        double lng = Double.parseDouble(parts[0]);
        double lat = Double.parseDouble(parts[1]);
        return new GeoPoint(lat, lng,
                first.path("formatted_address").asText(""),
                first.path("province").asText(""),
                false);
    }

    /** 带一次重试的 GET：网络抖动/瞬时 5xx 时不至于直接失败 */
    private String httpGet(String url) throws IOException {
        IOException last = null;
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                Request request = new Request.Builder().url(url).get().build();
                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        throw new IOException("高德 HTTP " + response.code() + ": " + response.message());
                    }
                    return response.body() != null ? response.body().string() : "";
                }
            } catch (IOException e) {
                last = e;
                log.warn("高德请求失败（第 {} 次）: {}", attempt, e.getMessage());
                if (attempt < 2) {
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        throw last;
    }
}
