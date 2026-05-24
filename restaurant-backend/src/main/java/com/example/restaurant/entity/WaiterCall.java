package com.example.restaurant.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class WaiterCall {
    private Long callId;
    private Integer tableId;
    private String tableNumber;
    private Long userId;
    private String username;
    private String status;
    private String remark;
    private LocalDateTime callTime;
    private LocalDateTime handledTime;
    private Long handledBy;
    private String handlerName;
}
