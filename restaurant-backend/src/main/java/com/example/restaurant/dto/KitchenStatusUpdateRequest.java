package com.example.restaurant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class KitchenStatusUpdateRequest {
    @NotBlank(message = "菜品状态不能为空")
    @Pattern(regexp = "PENDING|PREPARING|READY|SERVED", message = "菜品状态不正确")
    private String status;
}
