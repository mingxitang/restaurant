package com.example.restaurant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class OrderStatusUpdateRequest {
    @NotBlank(message = "订单状态不能为空")
    @Pattern(regexp = "PENDING|PAID|COMPLETED|CANCELLED", message = "订单状态不正确")
    private String status;
}
