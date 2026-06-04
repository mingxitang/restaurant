package com.example.restaurant.dto;

import lombok.Data;

@Data
public class WxLoginRequest {
    private String code;
    private String nickName;
}
