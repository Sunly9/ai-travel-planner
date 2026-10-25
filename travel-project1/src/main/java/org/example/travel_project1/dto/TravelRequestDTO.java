package org.example.travel_project1.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * 行程生成请求参数（原类名 TravelResquestDTO 拼写有误，已修正）
 */
@Data
public class TravelRequestDTO {
    @NotNull(message = "城市不能为空")
    private String city;
    @NotNull(message = "天数不能为空")
    @Min(value = 1, message = "天数不能小于1")
    @Max(value = 30, message = "天数不能大于30")
    private Integer days;
    @NotNull(message = "预算不能为空")
    @Min(value = 100, message = "预算不能小于100")
    private Double budget;
}
