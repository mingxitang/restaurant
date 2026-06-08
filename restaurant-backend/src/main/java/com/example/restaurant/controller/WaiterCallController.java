package com.example.restaurant.controller;

import com.example.restaurant.common.ApiResponse;
import com.example.restaurant.common.PageUtils;
import com.example.restaurant.service.WaiterCallService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/waiter-calls")
@PreAuthorize("hasAnyRole('管理员','服务员')")
public class WaiterCallController {
    private final WaiterCallService waiterCallService;

    public WaiterCallController(WaiterCallService waiterCallService) {
        this.waiterCallService = waiterCallService;
    }

    @GetMapping
    public ApiResponse<?> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ApiResponse.ok(PageUtils.requested(page, size)
                ? waiterCallService.page(status, page, size)
                : waiterCallService.list(status));
    }

    @PutMapping("/{id}/handle")
    public ApiResponse<Void> handle(@PathVariable Long id, @RequestBody(required = false) Map<String, Long> body) {
        Long handledBy = body == null ? null : body.get("handledBy");
        waiterCallService.handle(id, handledBy);
        return ApiResponse.ok();
    }
}
