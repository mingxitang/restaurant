package com.example.restaurant.service;

import com.example.restaurant.common.BusinessException;
import com.example.restaurant.common.PageResponse;
import com.example.restaurant.common.PageUtils;
import com.example.restaurant.dto.ReviewRequest;
import com.example.restaurant.entity.Order;
import com.example.restaurant.entity.Review;
import com.example.restaurant.mapper.OrderMapper;
import com.example.restaurant.mapper.ReviewMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReviewService {
    private final ReviewMapper reviewMapper;
    private final OrderMapper orderMapper;

    public ReviewService(ReviewMapper reviewMapper, OrderMapper orderMapper) {
        this.reviewMapper = reviewMapper;
        this.orderMapper = orderMapper;
    }

    public List<Review> list() {
        return reviewMapper.findAll();
    }

    public PageResponse<Review> page(Integer page, Integer size) {
        PageUtils.PageParams params = PageUtils.normalize(page, size);
        long total = reviewMapper.countAll();
        List<Review> records = total == 0 ? List.of() : reviewMapper.findPage(params.getOffset(), params.getSize());
        return PageUtils.response(records, total, params);
    }

    public void create(ReviewRequest request) {
        Order order = orderMapper.findById(request.getOrderId());
        createInternal(request, order);
    }

    public void createForCustomer(ReviewRequest request, Long customerUserId) {
        Order order = orderMapper.findById(request.getOrderId());
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (customerUserId == null || !customerUserId.equals(order.getUserId())) {
            throw new BusinessException("无权评价该订单");
        }
        createInternal(request, order);
    }

    private void createInternal(ReviewRequest request, Order order) {
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (!List.of("PAID", "COMPLETED").contains(order.getStatus())) {
            throw new BusinessException("只有已支付或已完成订单可以评价");
        }
        if (reviewMapper.countByOrderId(request.getOrderId()) > 0) {
            throw new BusinessException("该订单已评价");
        }
        Review review = new Review();
        review.setOrderId(request.getOrderId());
        review.setRating(request.getRating());
        review.setComment(request.getComment());
        reviewMapper.insert(review);
    }
}
