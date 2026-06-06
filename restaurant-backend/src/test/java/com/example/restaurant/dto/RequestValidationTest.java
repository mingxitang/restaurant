package com.example.restaurant.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class RequestValidationTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void loginRequestRequiresValidPhoneAndPassword() {
        LoginRequest request = new LoginRequest();
        request.setPhone("bad-phone");
        request.setPassword("");

        assertThat(messages(request)).contains("手机号格式不正确", "密码不能为空");
    }

    @Test
    void orderCreateRequestRequiresItemsAndPositiveQuantity() {
        OrderCreateRequest.OrderItemRequest item = new OrderCreateRequest.OrderItemRequest();
        item.setDishId(1L);
        item.setQuantity(0);

        OrderCreateRequest request = new OrderCreateRequest();
        request.setTableId(1);
        request.setUserId(2L);
        request.setItems(List.of(item));

        assertThat(messages(request)).contains("菜品数量必须大于0");
    }

    @Test
    void refundRequestRequiresAmountAndStockAction() {
        RefundRequest request = new RefundRequest();
        request.setOrderId(1L);
        request.setDishId(2L);
        request.setQuantity(1);
        request.setRefundReason("退菜");
        request.setRefundAmount(new BigDecimal("-1.00"));
        request.setStockAction(2);

        assertThat(messages(request)).contains("退款金额不能为负数", "库存处理方式不正确");
    }

    @Test
    void customerReviewRequestRequiresRatingRange() {
        CustomerReviewRequest request = new CustomerReviewRequest();
        request.setRating(6);

        assertThat(messages(request)).contains("评分不能超过5星");
    }

    private Set<String> messages(Object request) {
        return validator.validate(request).stream()
                .map(violation -> violation.getMessage())
                .collect(Collectors.toSet());
    }
}
