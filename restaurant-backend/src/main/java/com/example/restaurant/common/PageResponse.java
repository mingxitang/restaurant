package com.example.restaurant.common;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class PageResponse<T> {
    private List<T> records;
    private long total;
    private int page;
    private int size;
    private int pages;
}
