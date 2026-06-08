package com.example.restaurant.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class CustomerOrderRequest {
    @NotNull(message = "桌台不能为空")
    private Integer tableId;

    private Long userId;

    @Valid
    @NotEmpty(message = "订单明细不能为空")
    private List<OrderCreateRequest.OrderItemRequest> items;

    public Integer getTableId() { return tableId; }
    public void setTableId(Integer tableId) { this.tableId = tableId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public List<OrderCreateRequest.OrderItemRequest> getItems() { return items; }
    public void setItems(List<OrderCreateRequest.OrderItemRequest> items) { this.items = items; }
}
