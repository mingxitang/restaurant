package com.example.restaurant.controller;

import com.example.restaurant.common.ApiResponse;
import com.example.restaurant.dto.ReviewRequest;
import com.example.restaurant.entity.Review;
import com.example.restaurant.service.ReviewService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {
    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('管理员','服务员')")
    public ApiResponse<List<Review>> list() {
        return ApiResponse.ok(reviewService.list());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('管理员','服务员','顾客')")
    public ApiResponse<Void> create(@RequestBody ReviewRequest request) {
        reviewService.create(request);
        return ApiResponse.ok();
    }
}
