package com.example.restaurant.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class WaiterCallRequest {
    @NotNull(message = "桌台不能为空")
    private Integer tableId;

    private Long userId;

    @Size(max = 255, message = "呼叫备注不能超过255个字符")
    private String remark;
}
