package com.example.restaurant.mapper;

import com.example.restaurant.entity.RefundRecord;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface RefundMapper {
    int insert(RefundRecord record);

    List<RefundRecord> findAll();

    List<RefundRecord> findPage(@Param("offset") int offset, @Param("size") int size);

    long countAll();

    List<Map<String, Object>> refundReasons();
}
