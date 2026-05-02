package com.example.restaurant.mapper;

import com.example.restaurant.entity.RefundRecord;

import java.util.List;
import java.util.Map;

public interface RefundMapper {
    int insert(RefundRecord record);

    List<RefundRecord> findAll();

    List<Map<String, Object>> refundReasons();
}
