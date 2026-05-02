package com.example.restaurant.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class RefundRecord {
    private Long refundId;
    private String refundReason;
    private Integer quantity;
    private BigDecimal refundAmount;
    private Integer stockAction;
    private LocalDateTime refundTime;
    private Long orderId;
    private Long dishId;
    private String dishName;
}
