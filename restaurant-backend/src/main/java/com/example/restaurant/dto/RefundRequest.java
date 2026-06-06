package com.example.restaurant.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RefundRequest {
    @NotNull(message = "订单不能为空")
    private Long orderId;

    @NotNull(message = "菜品不能为空")
    private Long dishId;

    @NotNull(message = "退款数量不能为空")
    @Min(value = 1, message = "退款数量必须大于0")
    private Integer quantity;

    @NotBlank(message = "退款原因不能为空")
    @Size(max = 255, message = "退款原因不能超过255个字符")
    private String refundReason;

    @NotNull(message = "退款金额不能为空")
    @PositiveOrZero(message = "退款金额不能为负数")
    private BigDecimal refundAmount;

    @NotNull(message = "库存处理方式不能为空")
    @Min(value = 0, message = "库存处理方式不正确")
    @Max(value = 1, message = "库存处理方式不正确")
    private Integer stockAction;
}
