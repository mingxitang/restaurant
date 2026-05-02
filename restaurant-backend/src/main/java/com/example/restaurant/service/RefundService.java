package com.example.restaurant.service;

import com.example.restaurant.dto.RefundRequest;
import com.example.restaurant.entity.RefundRecord;
import com.example.restaurant.mapper.DishMapper;
import com.example.restaurant.mapper.OrderMapper;
import com.example.restaurant.mapper.RefundMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class RefundService {
    private final RefundMapper refundMapper;
    private final DishMapper dishMapper;
    private final OrderMapper orderMapper;

    public RefundService(RefundMapper refundMapper, DishMapper dishMapper, OrderMapper orderMapper) {
        this.refundMapper = refundMapper;
        this.dishMapper = dishMapper;
        this.orderMapper = orderMapper;
    }

    public List<RefundRecord> list() {
        return refundMapper.findAll();
    }

    @Transactional
    public void create(RefundRequest request) {
        RefundRecord record = new RefundRecord();
        record.setOrderId(request.getOrderId());
        record.setDishId(request.getDishId());
        record.setQuantity(request.getQuantity());
        record.setRefundReason(request.getRefundReason());
        record.setRefundAmount(request.getRefundAmount());
        record.setStockAction(request.getStockAction());
        refundMapper.insert(record);
        orderMapper.deleteRefundedDetail(request.getOrderId(), request.getDishId(), request.getQuantity());
        orderMapper.decreaseDetailQuantity(request.getOrderId(), request.getDishId(), request.getQuantity());
        if (request.getRefundAmount() != null) {
            orderMapper.decreaseTotal(request.getOrderId(), request.getRefundAmount());
        }
        if (Integer.valueOf(1).equals(request.getStockAction())) {
            dishMapper.increaseStock(request.getDishId(), request.getQuantity());
        }
    }

    public List<Map<String, Object>> reasons() {
        return refundMapper.refundReasons();
    }
}
