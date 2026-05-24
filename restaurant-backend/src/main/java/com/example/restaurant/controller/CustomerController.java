package com.example.restaurant.controller;

import com.example.restaurant.common.ApiResponse;
import com.example.restaurant.dto.*;
import com.example.restaurant.entity.Order;
import com.example.restaurant.service.CategoryService;
import com.example.restaurant.service.DishService;
import com.example.restaurant.service.OrderService;
import com.example.restaurant.service.ReviewService;
import com.example.restaurant.service.WaiterCallService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customer")
@PreAuthorize("hasAnyRole('管理员','服务员','顾客')")
public class CustomerController {
    private final DishService dishService;
    private final CategoryService categoryService;
    private final OrderService orderService;
    private final ReviewService reviewService;
    private final WaiterCallService waiterCallService;

    public CustomerController(DishService dishService, CategoryService categoryService,
                              OrderService orderService, ReviewService reviewService,
                              WaiterCallService waiterCallService) {
        this.dishService = dishService;
        this.categoryService = categoryService;
        this.orderService = orderService;
        this.reviewService = reviewService;
        this.waiterCallService = waiterCallService;
    }

    @GetMapping("/menu")
    public ApiResponse<MenuResponse> menu() {
        return ApiResponse.ok(new MenuResponse(categoryService.list(), dishService.list(null, null)));
    }

    @PostMapping("/orders")
    public ApiResponse<Order> placeOrder(@RequestBody CustomerOrderRequest request) {
        OrderCreateRequest createRequest = new OrderCreateRequest();
        createRequest.setTableId(request.getTableId());
        createRequest.setUserId(request.getUserId());
        createRequest.setItems(request.getItems());
        return ApiResponse.ok(orderService.create(createRequest));
    }

    @GetMapping("/orders/{id}")
    public ApiResponse<Order> getOrder(@PathVariable Long id) {
        return ApiResponse.ok(orderService.detail(id));
    }

    @GetMapping("/tables/{tableId}/active-order")
    public ApiResponse<Order> getActiveOrderByTable(@PathVariable Integer tableId) {
        return ApiResponse.ok(orderService.activeByTableId(tableId));
    }

    @PostMapping("/orders/{id}/pay")
    public ApiResponse<Void> pay(@PathVariable Long id, @RequestBody PayOrderRequest request) {
        orderService.pay(id, request);
        return ApiResponse.ok();
    }

    @PostMapping("/orders/{id}/remind")
    public ApiResponse<Void> remind(@PathVariable Long id) {
        orderService.remind(id);
        return ApiResponse.ok();
    }

    @PostMapping("/orders/{id}/review")
    public ApiResponse<Void> review(@PathVariable Long id, @RequestBody ReviewRequest request) {
        request.setOrderId(id);
        reviewService.create(request);
        return ApiResponse.ok();
    }

    @PostMapping("/call-waiter")
    public ApiResponse<Void> callWaiter(@RequestBody java.util.Map<String, Object> body) {
        Object tableIdValue = body.get("tableId");
        Integer tableId = tableIdValue == null ? null : Integer.valueOf(String.valueOf(tableIdValue));
        Object userIdValue = body.get("userId");
        Long userId = userIdValue == null ? null : Long.valueOf(String.valueOf(userIdValue));
        String remark = body.get("remark") == null ? "顾客呼叫服务员" : String.valueOf(body.get("remark"));
        waiterCallService.create(tableId, userId, remark);
        return ApiResponse.ok();
    }
}
