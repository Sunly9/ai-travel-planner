package org.example.travel_project1.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChatRequestDTO {
    @NotBlank(message = "消息不能为空")
    private String message;

}
