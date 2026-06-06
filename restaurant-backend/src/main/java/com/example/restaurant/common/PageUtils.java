package com.example.restaurant.common;

import java.util.List;

public final class PageUtils {
    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private PageUtils() {
    }

    public static boolean requested(Integer page, Integer size) {
        return page != null || size != null;
    }

    public static <T> PageResponse<T> page(List<T> records, Integer page, Integer size) {
        int safePage = page == null || page < 1 ? DEFAULT_PAGE : page;
        int safeSize = size == null || size < 1 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        int total = records == null ? 0 : records.size();
        int from = Math.min((safePage - 1) * safeSize, total);
        int to = Math.min(from + safeSize, total);
        List<T> slice = records == null ? List.of() : records.subList(from, to);
        int pages = total == 0 ? 0 : (int) Math.ceil((double) total / safeSize);
        return new PageResponse<>(slice, total, safePage, safeSize, pages);
    }
}
