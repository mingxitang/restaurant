package com.example.restaurant.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class Review {
    private Long reviewId;
    private Integer rating;
    private String comment;
    private LocalDateTime reviewTime;
    private Long orderId;
    private String username;
    private BigDecimal totalAmount;
}
