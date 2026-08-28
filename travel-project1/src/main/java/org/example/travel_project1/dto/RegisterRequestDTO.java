package org.example.travel_project1.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequestDTO {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20,message = "用户名长度需在3-20个字符之间")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 30, message = "密码长度需在6-30个字符之间")
    private String password;

    @NotBlank(message = "昵称不能为空")
    @Size(max = 30, message = "昵称长度最大为30个字符")
    private String nickname;

}
