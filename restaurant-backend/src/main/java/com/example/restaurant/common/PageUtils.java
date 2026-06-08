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

    public static PageParams normalize(Integer page, Integer size) {
        int safePage = page == null || page < 1 ? DEFAULT_PAGE : page;
        int safeSize = size == null || size < 1 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        int offset = (safePage - 1) * safeSize;
        return new PageParams(safePage, safeSize, offset);
    }

    public static <T> PageResponse<T> response(List<T> records, long total, PageParams params) {
        int pages = total == 0 ? 0 : (int) Math.ceil((double) total / params.getSize());
        return new PageResponse<>(records == null ? List.of() : records, total, params.getPage(), params.getSize(), pages);
    }

    public static final class PageParams {
        private final int page;
        private final int size;
        private final int offset;

        private PageParams(int page, int size, int offset) {
            this.page = page;
            this.size = size;
            this.offset = offset;
        }

        public int getPage() {
            return page;
        }

        public int getSize() {
            return size;
        }

        public int getOffset() {
            return offset;
        }
    }
}
