package com.example.flowershop.dto.order;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class OrderResponse {

    private String id;
    private String status;
    private String orderType;
    private BigDecimal subTotal;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    private String deliveryAddressId;
    private String recipientName;
    private String recipientPhone;
    private List<OrderItemResponse> items;
    private LocalDateTime createdDate;

    @Data
    @Builder
    public static class OrderItemResponse {
        private String id;
        private String productId;
        private String productName;
        private BigDecimal price;
        private Integer quantity;
        private BigDecimal itemTotal;
    }
}

