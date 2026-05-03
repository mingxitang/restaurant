package com.example.restaurant.dto;

import java.util.List;

public class CustomerOrderRequest {
    private Integer tableId;
    private Long userId;
    private List<OrderCreateRequest.OrderItemRequest> items;

    public Integer getTableId() { return tableId; }
    public void setTableId(Integer tableId) { this.tableId = tableId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public List<OrderCreateRequest.OrderItemRequest> getItems() { return items; }
    public void setItems(List<OrderCreateRequest.OrderItemRequest> items) { this.items = items; }
}
