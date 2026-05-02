package com.example.restaurant.dto;

import lombok.Data;

import java.util.List;

@Data
public class OrderCreateRequest {
    private Integer tableId;
    private Long userId;
    private List<OrderItemRequest> items;

    @Data
    public static class OrderItemRequest {
        private Long dishId;
        private Integer quantity;
        private String remark;
    }
}
