package org.example.travel_project1.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SavePlanDTO {

    @NotBlank(message = "城市不能为空")
    private String city;

    @NotNull(message = "天数不能为空")
    private Integer days;

    private Double budget;

    @NotBlank(message = "行程数据不能为空")
    private String planData;
}
