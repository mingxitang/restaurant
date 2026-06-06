package com.example.restaurant.controller;

import com.example.restaurant.common.ApiResponse;
import com.example.restaurant.common.PageUtils;
import com.example.restaurant.dto.ReviewRequest;
import com.example.restaurant.entity.Review;
import com.example.restaurant.service.ReviewService;
import jakarta.validation.Valid;
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
    public ApiResponse<?> list(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        List<Review> reviews = reviewService.list();
        return ApiResponse.ok(PageUtils.requested(page, size) ? PageUtils.page(reviews, page, size) : reviews);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('管理员','服务员','顾客')")
    public ApiResponse<Void> create(@Valid @RequestBody ReviewRequest request) {
        reviewService.create(request);
        return ApiResponse.ok();
    }
}
