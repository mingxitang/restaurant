package com.example.restaurant.service;

import com.example.restaurant.common.BusinessException;
import com.example.restaurant.dto.ReviewRequest;
import com.example.restaurant.entity.Order;
import com.example.restaurant.entity.Review;
import com.example.restaurant.mapper.OrderMapper;
import com.example.restaurant.mapper.ReviewMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {
    @Mock
    private ReviewMapper reviewMapper;

    @Mock
    private OrderMapper orderMapper;

    @InjectMocks
    private ReviewService reviewService;

    @Test
    void createForCustomerRejectsOrderOwnedByAnotherCustomer() {
        Order order = paidOrder();
        order.setUserId(99L);
        when(orderMapper.findById(10L)).thenReturn(order);

        assertThatThrownBy(() -> reviewService.createForCustomer(reviewRequest(), 2L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("无权评价该订单");

        verify(reviewMapper, never()).insert(any(Review.class));
    }

    @Test
    void createRejectsDuplicateReviewForSameOrder() {
        Order order = paidOrder();
        when(orderMapper.findById(10L)).thenReturn(order);
        when(reviewMapper.countByOrderId(10L)).thenReturn(1);

        assertThatThrownBy(() -> reviewService.createForCustomer(reviewRequest(), 2L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("该订单已评价");

        verify(reviewMapper, never()).insert(any(Review.class));
    }

    @Test
    void createForCustomerInsertsReviewForOwnedPaidOrder() {
        Order order = paidOrder();
        when(orderMapper.findById(10L)).thenReturn(order);
        when(reviewMapper.countByOrderId(10L)).thenReturn(0);

        reviewService.createForCustomer(reviewRequest(), 2L);

        verify(reviewMapper).insert(any(Review.class));
    }

    private Order paidOrder() {
        Order order = new Order();
        order.setOrderId(10L);
        order.setUserId(2L);
        order.setStatus("PAID");
        return order;
    }

    private ReviewRequest reviewRequest() {
        ReviewRequest request = new ReviewRequest();
        request.setOrderId(10L);
        request.setRating(5);
        request.setComment("味道很好");
        return request;
    }
}
