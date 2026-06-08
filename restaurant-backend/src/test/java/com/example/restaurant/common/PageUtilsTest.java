package com.example.restaurant.common;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PageUtilsTest {
    @Test
    void normalizeReturnsOffsetAndClampedSize() {
        PageUtils.PageParams params = PageUtils.normalize(2, 200);

        assertThat(params.getPage()).isEqualTo(2);
        assertThat(params.getSize()).isEqualTo(100);
        assertThat(params.getOffset()).isEqualTo(100);
    }

    @Test
    void responseBuildsMetadataFromDatabaseRecords() {
        PageUtils.PageParams params = PageUtils.normalize(0, 2);
        PageResponse<Integer> page = PageUtils.response(List.of(1, 2), 5, params);

        assertThat(page.getRecords()).containsExactly(1, 2);
        assertThat(page.getTotal()).isEqualTo(5);
        assertThat(page.getPage()).isEqualTo(1);
        assertThat(page.getSize()).isEqualTo(2);
        assertThat(page.getPages()).isEqualTo(3);
    }
}
