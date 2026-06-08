package com.example.restaurant.service;

import com.example.restaurant.common.BusinessException;
import com.example.restaurant.common.PageResponse;
import com.example.restaurant.common.PageUtils;
import com.example.restaurant.dto.RefundRequest;
import com.example.restaurant.entity.RefundRecord;
import com.example.restaurant.entity.StockChangeLog;
import com.example.restaurant.mapper.DishMapper;
import com.example.restaurant.mapper.OrderMapper;
import com.example.restaurant.mapper.RefundMapper;
import com.example.restaurant.mapper.StockChangeLogMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class RefundService {
    private final RefundMapper refundMapper;
    private final DishMapper dishMapper;
    private final OrderMapper orderMapper;
    private final StockChangeLogMapper stockChangeLogMapper;

    public RefundService(RefundMapper refundMapper, DishMapper dishMapper, OrderMapper orderMapper,
                         StockChangeLogMapper stockChangeLogMapper) {
        this.refundMapper = refundMapper;
        this.dishMapper = dishMapper;
        this.orderMapper = orderMapper;
        this.stockChangeLogMapper = stockChangeLogMapper;
    }

    public List<RefundRecord> list() {
        return refundMapper.findAll();
    }

    public PageResponse<RefundRecord> page(Integer page, Integer size) {
        PageUtils.PageParams params = PageUtils.normalize(page, size);
        long total = refundMapper.countAll();
        List<RefundRecord> records = total == 0 ? List.of() : refundMapper.findPage(params.getOffset(), params.getSize());
        return PageUtils.response(records, total, params);
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
        int updated = orderMapper.decreaseDetailQuantity(request.getOrderId(), request.getDishId(), request.getQuantity());
        if (updated == 0) {
            throw new BusinessException("退款数量超过订单明细数量");
        }
        orderMapper.deleteRefundedDetail(request.getOrderId(), request.getDishId(), request.getQuantity());
        if (request.getRefundAmount() != null) {
            orderMapper.decreaseTotal(request.getOrderId(), request.getRefundAmount());
        }
        if (Integer.valueOf(1).equals(request.getStockAction())) {
            dishMapper.increaseStock(request.getDishId(), request.getQuantity());
            recordStockChange(request.getDishId(), request.getOrderId(), request.getQuantity(), "IN", "REFUND_RETURN");
        }
    }

    public List<Map<String, Object>> reasons() {
        return refundMapper.refundReasons();
    }

    private void recordStockChange(Long dishId, Long orderId, Integer quantity, String changeType, String reason) {
        StockChangeLog log = new StockChangeLog();
        log.setDishId(dishId);
        log.setOrderId(orderId);
        log.setQuantity(quantity);
        log.setChangeType(changeType);
        log.setReason(reason);
        stockChangeLogMapper.insert(log);
    }
}
