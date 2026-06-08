package com.example.restaurant.service;

import com.example.restaurant.common.BusinessException;
import com.example.restaurant.common.PageResponse;
import com.example.restaurant.common.PageUtils;
import com.example.restaurant.entity.WaiterCall;
import com.example.restaurant.mapper.WaiterCallMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WaiterCallService {
    private final WaiterCallMapper waiterCallMapper;

    public WaiterCallService(WaiterCallMapper waiterCallMapper) {
        this.waiterCallMapper = waiterCallMapper;
    }

    public List<WaiterCall> list(String status) {
        return waiterCallMapper.findAll(status);
    }

    public PageResponse<WaiterCall> page(String status, Integer page, Integer size) {
        PageUtils.PageParams params = PageUtils.normalize(page, size);
        long total = waiterCallMapper.countAll(status);
        List<WaiterCall> records = total == 0 ? List.of() : waiterCallMapper.findPage(status, params.getOffset(), params.getSize());
        return PageUtils.response(records, total, params);
    }

    public WaiterCall create(Integer tableId, Long userId, String remark) {
        if (tableId == null) {
            throw new BusinessException("桌台不能为空");
        }
        WaiterCall waiterCall = new WaiterCall();
        waiterCall.setTableId(tableId);
        waiterCall.setUserId(userId);
        waiterCall.setRemark(remark);
        waiterCall.setStatus("PENDING");
        waiterCallMapper.insert(waiterCall);
        return waiterCall;
    }

    public void handle(Long callId, Long handledBy) {
        if (waiterCallMapper.handle(callId, handledBy) == 0) {
            throw new BusinessException("呼叫记录不存在或已处理");
        }
    }
}
