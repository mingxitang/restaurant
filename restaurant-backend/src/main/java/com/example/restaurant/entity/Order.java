package com.example.restaurant.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class Order {
    private Long orderId;
    private LocalDateTime orderTime;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal discountAmount;
    private String status;
    private String payNo;
    private String payMethod;
    private Integer tableId;
    private String tableNumber;
    private Long userId;
    private String username;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private List<OrderDetail> details;
}
