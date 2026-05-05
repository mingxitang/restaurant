package com.example.restaurant.controller;

import com.example.restaurant.common.ApiResponse;
import com.example.restaurant.service.OrderService;
import com.example.restaurant.service.RefundService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@PreAuthorize("hasAnyRole('管理员','服务员','厨师')")
public class ReportController {
    private final OrderService orderService;
    private final RefundService refundService;

    public ReportController(OrderService orderService, RefundService refundService) {
        this.orderService = orderService;
        this.refundService = refundService;
    }

    @GetMapping("/dashboard")
    public ApiResponse<Map<String, Object>> dashboard() {
        return ApiResponse.ok(orderService.dashboard());
    }

    @GetMapping("/monthly-revenue")
    public ApiResponse<List<Map<String, Object>>> monthlyRevenue() {
        return ApiResponse.ok(orderService.monthlyRevenue());
    }

    @GetMapping("/hot-dishes")
    public ApiResponse<List<Map<String, Object>>> hotDishes(@RequestParam(required = false) Integer limit,
                                                            @RequestParam(required = false) String period) {
        return ApiResponse.ok(orderService.hotDishes(limit, period));
    }

    @GetMapping("/refund-reasons")
    public ApiResponse<List<Map<String, Object>>> refundReasons() {
        return ApiResponse.ok(refundService.reasons());
    }
}
