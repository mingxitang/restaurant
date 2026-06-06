package com.example.restaurant.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class OrderCreateRequest {
    @NotNull(message = "桌台不能为空")
    private Integer tableId;

    @NotNull(message = "用户不能为空")
    private Long userId;

    @Valid
    @NotEmpty(message = "订单明细不能为空")
    private List<OrderItemRequest> items;

    @Data
    public static class OrderItemRequest {
        @NotNull(message = "菜品不能为空")
        private Long dishId;

        @NotNull(message = "菜品数量不能为空")
        @Min(value = 1, message = "菜品数量必须大于0")
        private Integer quantity;

        @Size(max = 255, message = "备注不能超过255个字符")
        private String remark;
    }
}
