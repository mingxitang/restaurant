package com.example.restaurant.controller;

import com.example.restaurant.common.ApiResponse;
import com.example.restaurant.dto.*;
import com.example.restaurant.entity.Order;
import com.example.restaurant.service.CategoryService;
import com.example.restaurant.service.CurrentUserService;
import com.example.restaurant.service.DishService;
import com.example.restaurant.service.OrderService;
import com.example.restaurant.service.ReviewService;
import com.example.restaurant.service.WaiterCallService;
import com.example.restaurant.entity.User;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customer")
@PreAuthorize("hasRole('顾客')")
public class CustomerController {
    private final DishService dishService;
    private final CategoryService categoryService;
    private final OrderService orderService;
    private final ReviewService reviewService;
    private final WaiterCallService waiterCallService;
    private final CurrentUserService currentUserService;

    public CustomerController(DishService dishService, CategoryService categoryService,
                              OrderService orderService, ReviewService reviewService,
                              WaiterCallService waiterCallService, CurrentUserService currentUserService) {
        this.dishService = dishService;
        this.categoryService = categoryService;
        this.orderService = orderService;
        this.reviewService = reviewService;
        this.waiterCallService = waiterCallService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/menu")
    public ApiResponse<MenuResponse> menu() {
        return ApiResponse.ok(new MenuResponse(categoryService.list(), dishService.list(null, null)));
    }

    @PostMapping("/orders")
    public ApiResponse<Order> placeOrder(@Valid @RequestBody CustomerOrderRequest request) {
        User currentUser = currentUserService.requireCurrentUser();
        OrderCreateRequest createRequest = new OrderCreateRequest();
        createRequest.setTableId(request.getTableId());
        createRequest.setUserId(currentUser.getUserId());
        createRequest.setItems(request.getItems());
        return ApiResponse.ok(orderService.createForCustomer(createRequest, currentUser.getUserId()));
    }

    @GetMapping("/orders/{id}")
    public ApiResponse<Order> getOrder(@PathVariable Long id) {
        User currentUser = currentUserService.requireCurrentUser();
        return ApiResponse.ok(orderService.detailForCustomer(id, currentUser.getUserId()));
    }

    @GetMapping("/tables/{tableId}/active-order")
    public ApiResponse<Order> getActiveOrderByTable(@PathVariable Integer tableId) {
        User currentUser = currentUserService.requireCurrentUser();
        return ApiResponse.ok(orderService.activeByTableIdForCustomer(tableId, currentUser.getUserId()));
    }

    @PostMapping("/orders/{id}/pay")
    public ApiResponse<Void> pay(@PathVariable Long id, @Valid @RequestBody PayOrderRequest request) {
        User currentUser = currentUserService.requireCurrentUser();
        orderService.payForCustomer(id, request, currentUser.getUserId());
        return ApiResponse.ok();
    }

    @PostMapping("/orders/{id}/remind")
    public ApiResponse<Void> remind(@PathVariable Long id) {
        User currentUser = currentUserService.requireCurrentUser();
        orderService.remindForCustomer(id, currentUser.getUserId());
        return ApiResponse.ok();
    }

    @PostMapping("/orders/{id}/review")
    public ApiResponse<Void> review(@PathVariable Long id, @Valid @RequestBody CustomerReviewRequest request) {
        User currentUser = currentUserService.requireCurrentUser();
        ReviewRequest reviewRequest = new ReviewRequest();
        reviewRequest.setRating(request.getRating());
        reviewRequest.setComment(request.getComment());
        reviewRequest.setOrderId(id);
        reviewService.createForCustomer(reviewRequest, currentUser.getUserId());
        return ApiResponse.ok();
    }

    @PostMapping("/call-waiter")
    public ApiResponse<Void> callWaiter(@Valid @RequestBody WaiterCallRequest request) {
        User currentUser = currentUserService.requireCurrentUser();
        String remark = request.getRemark() == null || request.getRemark().isBlank()
                ? "顾客呼叫服务员"
                : request.getRemark();
        waiterCallService.create(request.getTableId(), currentUser.getUserId(), remark);
        return ApiResponse.ok();
    }
}
