package com.example.restaurant.mapper;

import com.example.restaurant.entity.WaiterCall;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface WaiterCallMapper {
    List<WaiterCall> findAll(@Param("status") String status);

    int insert(WaiterCall waiterCall);

    int handle(@Param("callId") Long callId, @Param("handledBy") Long handledBy);
}
