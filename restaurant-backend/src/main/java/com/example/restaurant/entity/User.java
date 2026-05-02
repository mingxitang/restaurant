package com.example.restaurant.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class User {
    private Long userId;
    private String username;
    private String phone;
    private Integer points;
    private String password;
    private Integer roleId;
    private String roleName;
    private Boolean enabled;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
