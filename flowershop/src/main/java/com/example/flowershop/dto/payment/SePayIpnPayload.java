package com.example.flowershop.dto.payment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SePayIpnPayload {

    private Long timestamp;

    @JsonProperty("notification_type")
    private String notificationType; // "ORDER_PAID"

    private IpnOrder order;
    private IpnTransaction transaction;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class IpnOrder {
        private String id;

        @JsonProperty("order_id")
        private String orderId;

        @JsonProperty("order_status")
        private String orderStatus;

        @JsonProperty("order_currency")
        private String orderCurrency;

        @JsonProperty("order_amount")
        private BigDecimal orderAmount;

        @JsonProperty("order_invoice_number")
        private String orderInvoiceNumber;

        @JsonProperty("order_description")
        private String orderDescription;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class IpnTransaction {
        private String id;

        @JsonProperty("payment_method")
        private String paymentMethod; // "BANK_TRANSFER", "CREDIT_CARD", "NAPAS"

        @JsonProperty("transaction_id")
        private String transactionId;

        @JsonProperty("transaction_type")
        private String transactionType;

        @JsonProperty("transaction_date")
        private String transactionDate;

        @JsonProperty("transaction_status")
        private String transactionStatus; // "APPROVED"

        @JsonProperty("transaction_amount")
        private BigDecimal transactionAmount;

        @JsonProperty("transaction_currency")
        private String transactionCurrency;
    }
}

