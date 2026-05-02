package com.example.restaurant.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TableInfo {
    private Integer tableId;
    private String tableNumber;
    private String tableName;
    private String area;
    private Integer capacity;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
