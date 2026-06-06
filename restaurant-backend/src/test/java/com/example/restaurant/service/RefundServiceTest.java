package com.example.restaurant.service;

import com.example.restaurant.common.BusinessException;
import com.example.restaurant.dto.RefundRequest;
import com.example.restaurant.entity.RefundRecord;
import com.example.restaurant.mapper.DishMapper;
import com.example.restaurant.mapper.OrderMapper;
import com.example.restaurant.mapper.RefundMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefundServiceTest {
    @Mock
    private RefundMapper refundMapper;

    @Mock
    private DishMapper dishMapper;

    @Mock
    private OrderMapper orderMapper;

    @InjectMocks
    private RefundService refundService;

    @Test
    void createDecreasesDetailBeforeDeletingZeroQuantityRows() {
        RefundRequest request = refundRequest();
        when(orderMapper.decreaseDetailQuantity(10L, 5L, 2)).thenReturn(1);

        refundService.create(request);

        InOrder inOrder = inOrder(refundMapper, orderMapper);
        inOrder.verify(refundMapper).insert(any(RefundRecord.class));
        inOrder.verify(orderMapper).decreaseDetailQuantity(10L, 5L, 2);
        inOrder.verify(orderMapper).deleteRefundedDetail(10L, 5L, 2);
        inOrder.verify(orderMapper).decreaseTotal(10L, new BigDecimal("38.00"));
        verify(dishMapper).increaseStock(5L, 2);
    }

    @Test
    void createRejectsRefundQuantityWhenNoDetailWasReduced() {
        RefundRequest request = refundRequest();
        when(orderMapper.decreaseDetailQuantity(10L, 5L, 2)).thenReturn(0);

        assertThatThrownBy(() -> refundService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("退款数量超过订单明细数量");

        verify(orderMapper, never()).deleteRefundedDetail(10L, 5L, 2);
        verify(orderMapper, never()).decreaseTotal(10L, new BigDecimal("38.00"));
        verify(dishMapper, never()).increaseStock(5L, 2);
    }

    private RefundRequest refundRequest() {
        RefundRequest request = new RefundRequest();
        request.setOrderId(10L);
        request.setDishId(5L);
        request.setQuantity(2);
        request.setRefundAmount(new BigDecimal("38.00"));
        request.setRefundReason("菜品退单");
        request.setStockAction(1);
        return request;
    }
}
