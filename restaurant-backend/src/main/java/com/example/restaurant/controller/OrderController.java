package com.example.restaurant.controller;

import com.example.restaurant.common.ApiResponse;
import com.example.restaurant.dto.OrderCreateRequest;
import com.example.restaurant.dto.PayOrderRequest;
import com.example.restaurant.entity.Order;
import com.example.restaurant.service.OrderService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@PreAuthorize("hasAnyRole('管理员','服务员','厨师','顾客')")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public ApiResponse<List<Order>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        return ApiResponse.ok(orderService.list(status, startDate, endDate));
    }

    @GetMapping("/{id}")
    public ApiResponse<Order> detail(@PathVariable Long id) {
        return ApiResponse.ok(orderService.detail(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('管理员','服务员','顾客')")
    public ApiResponse<Order> create(@RequestBody OrderCreateRequest request) {
        return ApiResponse.ok(orderService.create(request));
    }

    @PutMapping("/{id}/pay")
    @PreAuthorize("hasAnyRole('管理员','服务员')")
    public ApiResponse<Void> pay(@PathVariable Long id, @RequestBody PayOrderRequest request) {
        orderService.pay(id, request);
        return ApiResponse.ok();
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('管理员','服务员')")
    public ApiResponse<Void> cancel(@PathVariable Long id) {
        orderService.cancel(id);
        return ApiResponse.ok();
    }

    @PutMapping("/{id}/status")
    public ApiResponse<Void> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        orderService.updateStatus(id, body.get("status"));
        return ApiResponse.ok();
    }
}
