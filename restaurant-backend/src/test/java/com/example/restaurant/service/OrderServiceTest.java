package com.example.restaurant.service;

import com.example.restaurant.common.BusinessException;
import com.example.restaurant.dto.OrderCreateRequest;
import com.example.restaurant.dto.PayOrderRequest;
import com.example.restaurant.entity.Dish;
import com.example.restaurant.entity.Order;
import com.example.restaurant.entity.OrderDetail;
import com.example.restaurant.entity.StockChangeLog;
import com.example.restaurant.entity.TableInfo;
import com.example.restaurant.mapper.DishMapper;
import com.example.restaurant.mapper.OrderMapper;
import com.example.restaurant.mapper.StockChangeLogMapper;
import com.example.restaurant.mapper.TableInfoMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
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

    @Mock
    private StockChangeLogMapper stockChangeLogMapper;

    @InjectMocks
    private OrderService orderService;

    @Test
    void payOnlyMovesPendingDetailsToPreparing() {
        Order order = new Order();
        order.setOrderId(10L);
        order.setTableId(3);
        order.setStatus("PENDING");
        order.setTotalAmount(new BigDecimal("58.00"));
        PayOrderRequest request = new PayOrderRequest();
        request.setPayMethod("现金");
        request.setDiscountAmount(BigDecimal.ZERO);
        when(orderMapper.findById(10L)).thenReturn(order);

        orderService.pay(10L, request);

        verify(orderMapper).pay(org.mockito.ArgumentMatchers.eq(10L), org.mockito.ArgumentMatchers.eq(new BigDecimal("58.00")),
                org.mockito.ArgumentMatchers.eq(BigDecimal.ZERO), org.mockito.ArgumentMatchers.eq("现金"), org.mockito.ArgumentMatchers.anyString());
        verify(orderMapper).updateStatus(10L, "PAID");
        verify(tableInfoMapper).updateStatus(3, "FREE");
        verify(orderMapper).updatePendingDetailStatus(10L, "PREPARING");
        verify(orderMapper, never()).findDetails(10L);
    }

    @Test
    void payRejectsNonPendingOrder() {
        Order order = new Order();
        order.setOrderId(10L);
        order.setStatus("PAID");
        order.setTotalAmount(new BigDecimal("58.00"));
        PayOrderRequest request = new PayOrderRequest();
        request.setPayMethod("现金");
        when(orderMapper.findById(10L)).thenReturn(order);

        assertThatThrownBy(() -> orderService.pay(10L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("只有待支付订单可以支付");

        verify(orderMapper, never()).pay(any(), any(), any(), any(), any());
    }

    @Test
    void payRejectsDiscountGreaterThanOrderTotal() {
        Order order = new Order();
        order.setOrderId(10L);
        order.setStatus("PENDING");
        order.setTotalAmount(new BigDecimal("58.00"));
        PayOrderRequest request = new PayOrderRequest();
        request.setPayMethod("现金");
        request.setDiscountAmount(new BigDecimal("59.00"));
        when(orderMapper.findById(10L)).thenReturn(order);

        assertThatThrownBy(() -> orderService.pay(10L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("优惠金额不能超过订单金额");

        verify(orderMapper, never()).pay(any(), any(), any(), any(), any());
    }

    @Test
    void createForCustomerRejectsOrderOwnedByAnotherCustomer() {
        Order existing = new Order();
        existing.setOrderId(10L);
        existing.setTableId(3);
        existing.setUserId(99L);
        when(tableInfoMapper.lockById(3)).thenReturn(new TableInfo());
        when(orderMapper.findActiveByTableId(3)).thenReturn(existing);

        OrderCreateRequest request = new OrderCreateRequest();
        request.setTableId(3);
        request.setUserId(2L);
        request.setItems(List.of(orderItem()));

        assertThatThrownBy(() -> orderService.createForCustomer(request, 2L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("当前桌台已有其他顾客订单");

        verify(dishMapper, never()).findById(5L);
    }

    @Test
    void createForCustomerAllowsAppendingToOwnActiveOrder() {
        Order existing = new Order();
        existing.setOrderId(10L);
        existing.setTableId(3);
        existing.setUserId(2L);
        existing.setTotalAmount(new BigDecimal("20.00"));
        Dish dish = new Dish();
        dish.setDishId(5L);
        dish.setDishName("番茄炒蛋");
        dish.setAvailable(true);
        dish.setPrice(new BigDecimal("18.00"));

        OrderCreateRequest request = new OrderCreateRequest();
        request.setTableId(3);
        request.setUserId(2L);
        request.setItems(List.of(orderItem()));

        when(tableInfoMapper.lockById(3)).thenReturn(new TableInfo());
        when(orderMapper.findActiveByTableId(3)).thenReturn(existing);
        when(dishMapper.findById(5L)).thenReturn(dish);
        when(dishMapper.decreaseStock(5L, 1)).thenReturn(1);
        when(orderMapper.addDetailQuantity(any(OrderDetail.class))).thenReturn(1);
        when(orderMapper.findById(10L)).thenReturn(existing);
        when(orderMapper.findDetails(10L)).thenReturn(List.of());

        orderService.createForCustomer(request, 2L);

        verify(orderMapper).increaseTotal(10L, new BigDecimal("18.00"));
        verify(stockChangeLogMapper).insert(any(StockChangeLog.class));
        verify(tableInfoMapper).updateStatus(3, "OCCUPIED");
    }

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

    private OrderCreateRequest.OrderItemRequest orderItem() {
        OrderCreateRequest.OrderItemRequest item = new OrderCreateRequest.OrderItemRequest();
        item.setDishId(5L);
        item.setQuantity(1);
        return item;
    }
}
