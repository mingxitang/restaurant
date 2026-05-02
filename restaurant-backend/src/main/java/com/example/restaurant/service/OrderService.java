package com.example.restaurant.service;

import com.example.restaurant.common.BusinessException;
import com.example.restaurant.dto.OrderCreateRequest;
import com.example.restaurant.dto.PayOrderRequest;
import com.example.restaurant.entity.Dish;
import com.example.restaurant.entity.Order;
import com.example.restaurant.entity.OrderDetail;
import com.example.restaurant.mapper.DishMapper;
import com.example.restaurant.mapper.OrderMapper;
import com.example.restaurant.mapper.TableInfoMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class OrderService {
    private final OrderMapper orderMapper;
    private final DishMapper dishMapper;
    private final TableInfoMapper tableInfoMapper;

    public OrderService(OrderMapper orderMapper, DishMapper dishMapper, TableInfoMapper tableInfoMapper) {
        this.orderMapper = orderMapper;
        this.dishMapper = dishMapper;
        this.tableInfoMapper = tableInfoMapper;
    }

    public List<Order> list(String status, LocalDate startDate, LocalDate endDate) {
        return orderMapper.findAll(status, startDate, endDate);
    }

    public Order detail(Long id) {
        Order order = orderMapper.findById(id);
        if (order != null) {
            order.setDetails(orderMapper.findDetails(id));
        }
        return order;
    }

    @Transactional
    public Order create(OrderCreateRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BusinessException("订单明细不能为空");
        }
        Order order = orderMapper.findActiveByTableId(request.getTableId());
        boolean newOrder = order == null;
        if (newOrder) {
            order = new Order();
            order.setTableId(request.getTableId());
            order.setUserId(request.getUserId());
            order.setStatus("PENDING");
            order.setTotalAmount(BigDecimal.ZERO);
            orderMapper.insert(order);
        }

        BigDecimal total = BigDecimal.ZERO;
        for (OrderCreateRequest.OrderItemRequest item : request.getItems()) {
            Dish dish = dishMapper.findById(item.getDishId());
            if (dish == null || !Boolean.TRUE.equals(dish.getAvailable())) {
                throw new BusinessException("菜品不存在或已下架");
            }
            int affected = dishMapper.decreaseStock(item.getDishId(), item.getQuantity());
            if (affected == 0) {
                throw new BusinessException(dish.getDishName() + "库存不足");
            }
            OrderDetail detail = new OrderDetail();
            detail.setOrderId(order.getOrderId());
            detail.setDishId(item.getDishId());
            detail.setQuantity(item.getQuantity());
            detail.setUnitPrice(dish.getPrice());
            detail.setRemark(item.getRemark());
            int updated = orderMapper.addDetailQuantity(detail);
            if (updated == 0) {
                orderMapper.insertDetail(detail);
            }
            total = total.add(dish.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }
        if (newOrder) {
            order.setTotalAmount(total);
            orderMapper.updateTotal(order.getOrderId(), total);
        } else {
            orderMapper.increaseTotal(order.getOrderId(), total);
        }
        tableInfoMapper.updateStatus(order.getTableId(), "OCCUPIED");
        return detail(order.getOrderId());
    }

    @Transactional
    public void pay(Long orderId, PayOrderRequest request) {
        Order order = orderMapper.findById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        BigDecimal discount = request.getDiscountAmount() == null ? BigDecimal.ZERO : request.getDiscountAmount();
        BigDecimal paid = order.getTotalAmount().subtract(discount);
        String payNo = "PAY" + DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(java.time.LocalDateTime.now()) + orderId;
        orderMapper.pay(orderId, paid, discount, request.getPayMethod(), payNo);
        orderMapper.updateStatus(orderId, "PAID");
    }

    @Transactional
    public void cancel(Long orderId) {
        Order order = detail(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (!"PENDING".equals(order.getStatus())) {
            throw new BusinessException("只有待支付订单可以取消");
        }
        for (OrderDetail detail : order.getDetails()) {
            dishMapper.increaseStock(detail.getDishId(), detail.getQuantity());
        }
        orderMapper.updateStatus(orderId, "CANCELLED");
        tableInfoMapper.updateStatus(order.getTableId(), "FREE");
    }

    @Transactional
    public void unpay(Long orderId) {
        Order order = orderMapper.findById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (!"PAID".equals(order.getStatus()) && !"COMPLETED".equals(order.getStatus())) {
            throw new BusinessException("只有已支付或已完成的订单可以反结账");
        }
        orderMapper.unpay(orderId);
        tableInfoMapper.updateStatus(order.getTableId(), "OCCUPIED");
    }

    public void updateStatus(Long orderId, String status) {
        orderMapper.updateStatus(orderId, status);
    }

    public Map<String, Object> dashboard() {
        return orderMapper.dashboard();
    }

    public List<Map<String, Object>> monthlyRevenue() {
        return orderMapper.monthlyRevenue();
    }

    public List<Map<String, Object>> hotDishes(Integer limit) {
        return orderMapper.hotDishes(limit == null ? 10 : limit);
    }
}
