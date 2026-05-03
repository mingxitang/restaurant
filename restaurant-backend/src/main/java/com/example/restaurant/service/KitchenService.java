package com.example.restaurant.service;

import com.example.restaurant.common.BusinessException;
import com.example.restaurant.mapper.OrderMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class KitchenService {
    private final OrderMapper orderMapper;

    public KitchenService(OrderMapper orderMapper) {
        this.orderMapper = orderMapper;
    }

    public List<Map<String, Object>> queue() {
        return orderMapper.kitchenQueue();
    }

    @Transactional
    public void updateStatus(Long orderId, Long dishId, String status) {
        if (!List.of("PENDING", "PREPARING", "READY", "SERVED").contains(status)) {
            throw new BusinessException("无效的菜品状态: " + status);
        }
        orderMapper.updateDetailStatus(orderId, dishId, status);
    }
}
