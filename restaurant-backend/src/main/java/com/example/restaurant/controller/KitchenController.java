package com.example.restaurant.controller;

import com.example.restaurant.common.ApiResponse;
import com.example.restaurant.dto.KitchenStatusUpdateRequest;
import com.example.restaurant.service.KitchenService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/kitchen")
@PreAuthorize("hasAnyRole('管理员','厨师')")
public class KitchenController {
    private final KitchenService kitchenService;

    public KitchenController(KitchenService kitchenService) {
        this.kitchenService = kitchenService;
    }

    @GetMapping("/queue")
    public ApiResponse<List<Map<String, Object>>> queue() {
        return ApiResponse.ok(kitchenService.queue());
    }

    @PutMapping("/{orderId}/{dishId}/status")
    public ApiResponse<Void> updateStatus(@PathVariable Long orderId, @PathVariable Long dishId,
                                          @Valid @RequestBody KitchenStatusUpdateRequest request) {
        kitchenService.updateStatus(orderId, dishId, request.getStatus());
        return ApiResponse.ok();
    }
}
