package com.example.restaurant.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PayOrderRequest {
    private String payMethod;
    private BigDecimal discountAmount;
}
