package com.example.restaurant.controller;

import com.example.restaurant.common.ApiResponse;
import com.example.restaurant.common.PageUtils;
import com.example.restaurant.dto.OrderCreateRequest;
import com.example.restaurant.dto.OrderStatusUpdateRequest;
import com.example.restaurant.dto.PayOrderRequest;
import com.example.restaurant.dto.TransferTableRequest;
import com.example.restaurant.entity.Order;
import com.example.restaurant.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
@PreAuthorize("hasAnyRole('管理员','服务员','厨师','顾客')")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public ApiResponse<?> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        List<Order> orders = orderService.list(status, startDate, endDate);
        return ApiResponse.ok(PageUtils.requested(page, size) ? PageUtils.page(orders, page, size) : orders);
    }

    @GetMapping("/{id}")
    public ApiResponse<Order> detail(@PathVariable Long id) {
        return ApiResponse.ok(orderService.detail(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('管理员','服务员','顾客')")
    public ApiResponse<Order> create(@Valid @RequestBody OrderCreateRequest request) {
        return ApiResponse.ok(orderService.create(request));
    }

    @PutMapping("/{id}/pay")
    @PreAuthorize("hasAnyRole('管理员','服务员')")
    public ApiResponse<Void> pay(@PathVariable Long id, @Valid @RequestBody PayOrderRequest request) {
        orderService.pay(id, request);
        return ApiResponse.ok();
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('管理员','服务员')")
    public ApiResponse<Void> cancel(@PathVariable Long id) {
        orderService.cancel(id);
        return ApiResponse.ok();
    }

    @PutMapping("/{id}/unpay")
    @PreAuthorize("hasAnyRole('管理员','服务员')")
    public ApiResponse<Void> unpay(@PathVariable Long id) {
        orderService.unpay(id);
        return ApiResponse.ok();
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('管理员','服务员')")
    public ApiResponse<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody OrderStatusUpdateRequest request) {
        orderService.updateStatus(id, request.getStatus());
        return ApiResponse.ok();
    }

    @PutMapping("/transfer-table")
    @PreAuthorize("hasAnyRole('管理员','服务员')")
    public ApiResponse<Void> transferTable(@Valid @RequestBody TransferTableRequest request) {
        orderService.transferTable(request.getSourceTableId(), request.getTargetTableId());
        return ApiResponse.ok();
    }
}
