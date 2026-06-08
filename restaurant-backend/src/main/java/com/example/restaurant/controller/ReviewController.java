package com.example.restaurant.controller;

import com.example.restaurant.common.ApiResponse;
import com.example.restaurant.common.PageUtils;
import com.example.restaurant.dto.ReviewRequest;
import com.example.restaurant.entity.User;
import com.example.restaurant.service.CurrentUserService;
import com.example.restaurant.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {
    private final ReviewService reviewService;
    private final CurrentUserService currentUserService;

    public ReviewController(ReviewService reviewService, CurrentUserService currentUserService) {
        this.reviewService = reviewService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('管理员','服务员')")
    public ApiResponse<?> list(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ApiResponse.ok(PageUtils.requested(page, size)
                ? reviewService.page(page, size)
                : reviewService.list());
    }

    @PostMapping
    @PreAuthorize("hasRole('顾客')")
    public ApiResponse<Void> create(@Valid @RequestBody ReviewRequest request) {
        User currentUser = currentUserService.requireCurrentUser();
        reviewService.createForCustomer(request, currentUser.getUserId());
        return ApiResponse.ok();
    }
}
