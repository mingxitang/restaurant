package com.example.restaurant.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TransferTableRequest {
    @NotNull(message = "原桌台不能为空")
    private Integer sourceTableId;

    @NotNull(message = "目标桌台不能为空")
    private Integer targetTableId;
}
