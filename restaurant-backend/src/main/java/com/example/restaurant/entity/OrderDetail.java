package com.example.restaurant.entity;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderDetail {
    private Long orderId;
    private Long dishId;
    private String dishName;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal subtotal;
    private String remark;
    private String status;
}
