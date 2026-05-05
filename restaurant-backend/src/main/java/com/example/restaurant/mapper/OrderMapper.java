package com.example.restaurant.mapper;

import com.example.restaurant.entity.Order;
import com.example.restaurant.entity.OrderDetail;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface OrderMapper {
    List<Order> findAll(@Param("status") String status, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    Order findById(@Param("orderId") Long orderId);

    Order findActiveByTableId(@Param("tableId") Integer tableId);

    List<OrderDetail> findDetails(@Param("orderId") Long orderId);

    int insert(Order order);

    int insertDetail(OrderDetail detail);

    int addDetailQuantity(OrderDetail detail);

    int updateTotal(@Param("orderId") Long orderId, @Param("totalAmount") BigDecimal totalAmount);

    int increaseTotal(@Param("orderId") Long orderId, @Param("amount") BigDecimal amount);

    int decreaseDetailQuantity(@Param("orderId") Long orderId, @Param("dishId") Long dishId, @Param("quantity") Integer quantity);

    int deleteRefundedDetail(@Param("orderId") Long orderId, @Param("dishId") Long dishId, @Param("quantity") Integer quantity);

    int decreaseTotal(@Param("orderId") Long orderId, @Param("amount") BigDecimal amount);

    int updateStatus(@Param("orderId") Long orderId, @Param("status") String status);

    int updateTable(@Param("orderId") Long orderId, @Param("tableId") Integer tableId);

    int pay(@Param("orderId") Long orderId, @Param("paidAmount") BigDecimal paidAmount, @Param("discountAmount") BigDecimal discountAmount, @Param("payMethod") String payMethod, @Param("payNo") String payNo);

    int unpay(@Param("orderId") Long orderId);

    int remind(@Param("orderId") Long orderId);

    Map<String, Object> dashboard();

    List<Map<String, Object>> monthlyRevenue();

    List<Map<String, Object>> hotDishes(@Param("limit") Integer limit, @Param("startDate") LocalDate startDate);

    List<Map<String, Object>> kitchenQueue();

    int updateDetailStatus(@Param("orderId") Long orderId, @Param("dishId") Long dishId, @Param("status") String status);

    int completePaidOrderIfFullyServed(@Param("orderId") Long orderId);
}
