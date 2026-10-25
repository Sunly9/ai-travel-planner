package org.example.travel_project1.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.travel_project1.service.GeoService;
import org.example.travel_project1.vo.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 高德地理编码代理接口
 * 后端持有 key，前端不暴露；结果落库缓存，重复查询不再打高德
 */
@Slf4j
@RestController
@RequestMapping("/api/geo")
@RequiredArgsConstructor
public class GeoController {

    private final GeoService geoService;

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

        try {
            GeoService.GeoPoint point = geoService.lookup(address, city);
            if (point == null) {
                return Result.fail(404, "未找到地点: " + address);
            }
            Map<String, Object> data = new HashMap<>();
            data.put("latitude", point.latitude());
            data.put("longitude", point.longitude());
            data.put("formattedAddress", point.formattedAddress());
            data.put("province", point.province());
            data.put("fromCache", point.fromCache());
            return Result.ok(data);
        } catch (IOException e) {
            return Result.fail(500, "高德地理编码请求失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("解析高德响应失败", e);
            return Result.fail(500, "解析高德响应失败: " + e.getMessage());
        }
    }
}
