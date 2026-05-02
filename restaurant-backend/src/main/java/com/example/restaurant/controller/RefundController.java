package com.example.restaurant.controller;

import com.example.restaurant.common.ApiResponse;
import com.example.restaurant.dto.RefundRequest;
import com.example.restaurant.entity.RefundRecord;
import com.example.restaurant.service.RefundService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/refunds")
@PreAuthorize("hasAnyRole('管理员','服务员')")
public class RefundController {
    private final RefundService refundService;

    public RefundController(RefundService refundService) {
        this.refundService = refundService;
    }

    @GetMapping
    public ApiResponse<List<RefundRecord>> list() {
        return ApiResponse.ok(refundService.list());
    }

    @PostMapping
    public ApiResponse<Void> create(@RequestBody RefundRequest request) {
        refundService.create(request);
        return ApiResponse.ok();
    }
}
