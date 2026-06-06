package com.example.restaurant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class WxLoginRequest {
    @NotBlank(message = "微信登录 code 不能为空")
    private String code;

    @Size(max = 50, message = "昵称不能超过50个字符")
    private String nickName;
}
