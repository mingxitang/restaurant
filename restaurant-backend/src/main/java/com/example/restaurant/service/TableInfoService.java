package com.example.restaurant.service;

import com.example.restaurant.common.PageResponse;
import com.example.restaurant.common.PageUtils;
import com.example.restaurant.entity.TableInfo;
import com.example.restaurant.mapper.TableInfoMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TableInfoService {
    private final TableInfoMapper tableInfoMapper;

    public TableInfoService(TableInfoMapper tableInfoMapper) {
        this.tableInfoMapper = tableInfoMapper;
    }

    public List<TableInfo> list(String status) {
        return tableInfoMapper.findAll(status);
    }

    public PageResponse<TableInfo> page(String status, Integer page, Integer size) {
        PageUtils.PageParams params = PageUtils.normalize(page, size);
        long total = tableInfoMapper.countAll(status);
        List<TableInfo> records = total == 0 ? List.of() : tableInfoMapper.findPage(status, params.getOffset(), params.getSize());
        return PageUtils.response(records, total, params);
    }

    public TableInfo create(TableInfo tableInfo) {
        if (tableInfo.getStatus() == null) {
            tableInfo.setStatus("FREE");
        }
        tableInfoMapper.insert(tableInfo);
        return tableInfo;
    }

    public void update(TableInfo tableInfo) {
        tableInfoMapper.update(tableInfo);
    }

    public void updateStatus(Integer id, String status) {
        tableInfoMapper.updateStatus(id, status);
    }

    public void delete(Integer id) {
        tableInfoMapper.delete(id);
    }
}
