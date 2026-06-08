package com.example.restaurant.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class StockChangeLog {
    private Long logId;
    private Long dishId;
    private Long orderId;
    private Integer quantity;
    private String changeType;
    private String reason;
    private LocalDateTime createTime;
    private String dishName;
}
