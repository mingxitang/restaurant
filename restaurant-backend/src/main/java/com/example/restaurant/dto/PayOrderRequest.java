package com.example.restaurant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PayOrderRequest {
    @NotBlank(message = "支付方式不能为空")
    @Size(max = 32, message = "支付方式不能超过32个字符")
    private String payMethod;

    @PositiveOrZero(message = "优惠金额不能为负数")
    private BigDecimal discountAmount;
}
