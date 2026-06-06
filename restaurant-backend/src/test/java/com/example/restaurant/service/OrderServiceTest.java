package com.example.restaurant.service;

import com.example.restaurant.entity.Order;
import com.example.restaurant.entity.OrderDetail;
import com.example.restaurant.mapper.DishMapper;
import com.example.restaurant.mapper.OrderMapper;
import com.example.restaurant.mapper.TableInfoMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
    @Mock
    private OrderMapper orderMapper;

    @Mock
    private DishMapper dishMapper;

    @Mock
    private TableInfoMapper tableInfoMapper;

    @InjectMocks
    private OrderService orderService;

    @Test
    void unpayRestoresTableToOccupied() {
        Order order = new Order();
        order.setOrderId(10L);
        order.setTableId(3);
        order.setStatus("PAID");
        when(orderMapper.findById(10L)).thenReturn(order);

        orderService.unpay(10L);

        verify(orderMapper).unpay(10L);
        verify(tableInfoMapper).updateStatus(3, "OCCUPIED");
    }

    @Test
    void transferTableDeletesSourceDetailsWhenMergingOrders() {
        Order sourceOrder = new Order();
        sourceOrder.setOrderId(10L);
        sourceOrder.setTotalAmount(new BigDecimal("38.00"));
        Order targetOrder = new Order();
        targetOrder.setOrderId(20L);
        OrderDetail detail = new OrderDetail();
        detail.setDishId(5L);
        detail.setQuantity(2);
        detail.setUnitPrice(new BigDecimal("19.00"));

        when(orderMapper.findActiveByTableId(1)).thenReturn(sourceOrder);
        when(orderMapper.findActiveByTableId(2)).thenReturn(targetOrder);
        when(orderMapper.findDetails(10L)).thenReturn(List.of(detail));
        when(orderMapper.addDetailQuantity(org.mockito.ArgumentMatchers.any(OrderDetail.class))).thenReturn(1);

        orderService.transferTable(1, 2);

        InOrder inOrder = inOrder(orderMapper);
        inOrder.verify(orderMapper).increaseTotal(20L, new BigDecimal("38.00"));
        inOrder.verify(orderMapper).deleteDetails(10L);
        inOrder.verify(orderMapper).updateStatus(10L, "CANCELLED");
        verify(tableInfoMapper).updateStatus(1, "FREE");
        verify(tableInfoMapper).updateStatus(2, "OCCUPIED");
    }
}
