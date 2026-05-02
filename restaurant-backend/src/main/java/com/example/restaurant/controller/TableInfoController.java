package com.example.restaurant.controller;

import com.example.restaurant.common.ApiResponse;
import com.example.restaurant.entity.TableInfo;
import com.example.restaurant.service.TableInfoService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tables")
public class TableInfoController {
    private final TableInfoService tableInfoService;

    public TableInfoController(TableInfoService tableInfoService) {
        this.tableInfoService = tableInfoService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('管理员','服务员','顾客')")
    public ApiResponse<List<TableInfo>> list(@RequestParam(required = false) String status) {
        return ApiResponse.ok(tableInfoService.list(status));
    }

    @PostMapping
    @PreAuthorize("hasRole('管理员')")
    public ApiResponse<TableInfo> create(@RequestBody TableInfo tableInfo) {
        return ApiResponse.ok(tableInfoService.create(tableInfo));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('管理员')")
    public ApiResponse<Void> update(@PathVariable Integer id, @RequestBody TableInfo tableInfo) {
        tableInfo.setTableId(id);
        tableInfoService.update(tableInfo);
        return ApiResponse.ok();
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('管理员','服务员')")
    public ApiResponse<Void> updateStatus(@PathVariable Integer id, @RequestBody Map<String, String> body) {
        tableInfoService.updateStatus(id, body.get("status"));
        return ApiResponse.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('管理员')")
    public ApiResponse<Void> delete(@PathVariable Integer id) {
        tableInfoService.delete(id);
        return ApiResponse.ok();
    }
}
