package com.example.restaurant.dto;

import lombok.Data;

@Data
public class ReviewRequest {
    private Long orderId;
    private Integer rating;
    private String comment;
}
