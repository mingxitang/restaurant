package com.example.restaurant.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class RefundRequest {
    private Long orderId;
    private Long dishId;
    private Integer quantity;
    private String refundReason;
    private BigDecimal refundAmount;
    private Integer stockAction;
}
