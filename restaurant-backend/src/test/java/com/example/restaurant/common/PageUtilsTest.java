package com.example.restaurant.common;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PageUtilsTest {
    @Test
    void pageReturnsRequestedSliceAndMetadata() {
        PageResponse<Integer> page = PageUtils.page(List.of(1, 2, 3, 4, 5), 2, 2);

        assertThat(page.getRecords()).containsExactly(3, 4);
        assertThat(page.getTotal()).isEqualTo(5);
        assertThat(page.getPage()).isEqualTo(2);
        assertThat(page.getSize()).isEqualTo(2);
        assertThat(page.getPages()).isEqualTo(3);
    }

    @Test
    void pageClampsInvalidPageToFirstPage() {
        PageResponse<Integer> page = PageUtils.page(List.of(1, 2, 3), 0, 2);

        assertThat(page.getRecords()).containsExactly(1, 2);
        assertThat(page.getPage()).isEqualTo(1);
    }
}
