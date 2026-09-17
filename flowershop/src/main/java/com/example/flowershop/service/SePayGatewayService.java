package com.example.flowershop.service;

import com.example.flowershop.dto.payment.CreatePaymentRequest;
import com.example.flowershop.dto.payment.CreatePaymentResponse;
import com.example.flowershop.dto.payment.PaymentStatusResponse;
import com.example.flowershop.dto.payment.SePayCheckoutFormResponse;
import com.example.flowershop.dto.payment.SePayIpnPayload;
import com.example.flowershop.entity.Order;
import com.example.flowershop.entity.Payment;
import com.example.flowershop.entity.enums.OrderStatus;
import com.example.flowershop.entity.enums.PaymentMethod;
import com.example.flowershop.entity.enums.PaymentStatus;
import com.example.flowershop.entity.enums.PaymentType;
import com.example.flowershop.exception.ApiException;
import com.example.flowershop.repository.OrderRepository;
import com.example.flowershop.repository.PaymentRepository;
import com.example.flowershop.util.SePaySignatureUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class SePayGatewayService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;

    @Value("${sepay.merchant-id:SP-TEST-001}")
    private String merchantId;

    @Value("${sepay.secret-key:sepay_secret_test_key}")
    private String secretKey;

    @Value("${sepay.checkout-url:https://pay-sandbox.sepay.vn/v1/checkout/init}")
    private String checkoutUrl;

    @Value("${sepay.success-url:http://localhost:5173/payment/success}")
    private String successUrl;

    @Value("${sepay.error-url:http://localhost:5173/payment/error}")
    private String errorUrl;

    @Value("${sepay.cancel-url:http://localhost:5173/payment/cancel}")
    private String cancelUrl;

    @Value("${sepay.ipn-secret:}")
    private String ipnSecret;

    private String getShortOrderCode(String orderId) {
        if (orderId == null) return "DH000000";
        String clean = orderId.replace("-", "").toUpperCase();
        return clean.length() >= 8 ? clean.substring(0, 8) : clean;
    }

    /**
     * PHASE 5: Khởi tạo thanh toán SePay với tính toán số tiền bảo mật từ Server
     */
    @Transactional
    public CreatePaymentResponse createPayment(CreatePaymentRequest request, String authenticatedUserId) {
        String orderId = request.getOrderId();
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy đơn hàng với id: " + orderId));

        // PHASE 12: Xác thực quyền sở hữu đơn hàng
        if (authenticatedUserId != null && !authenticatedUserId.isBlank()
                && order.getCustomer() != null
                && !order.getCustomer().getId().equals(authenticatedUserId)) {
            throw ApiException.forbidden("Bạn không có quyền thanh toán cho đơn hàng này.");
        }

        // Kiểm tra trạng thái đơn hàng
        if (order.getStatus() == OrderStatus.COMPLETED) {
            throw ApiException.badRequest("Đơn hàng này đã hoàn thành, không thể thanh toán thêm.");
        }
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw ApiException.badRequest("Đơn hàng này đã bị hủy, không thể thanh toán.");
        }

        // PHASE 2 & 3: Xác định PaymentType (FULL, DEPOSIT, FINAL)
        PaymentType paymentType = request.getPaymentType() != null ? request.getPaymentType() : PaymentType.FULL;

        // PHASE 13: Server-side tính toán số tiền thanh toán (TUYỆT ĐỐI không tin amount từ React)
        BigDecimal payableAmount;
        if (paymentType == PaymentType.DEPOSIT) {
            payableAmount = (order.getDepositAmount() != null && order.getDepositAmount().compareTo(BigDecimal.ZERO) > 0)
                    ? order.getDepositAmount()
                    : order.getTotalAmount().multiply(new BigDecimal("0.5")); // Mặc định cọc 50% nếu chưa đặt
        } else if (paymentType == PaymentType.FINAL) {
            List<Payment> orderPayments = paymentRepository.findByOrderId(orderId);
            BigDecimal totalPaid = orderPayments.stream()
                    .filter(p -> p.getStatus() == PaymentStatus.SUCCESS)
                    .map(Payment::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            payableAmount = order.getTotalAmount().subtract(totalPaid);
            if (payableAmount.compareTo(BigDecimal.ZERO) <= 0) {
                throw ApiException.badRequest("Đơn hàng này đã được thanh toán đầy đủ.");
            }
        } else {
            // FULL payment
            payableAmount = order.getTotalAmount();
        }

        // Tạo mã invoice duy nhất cho mỗi lần thanh toán (PHASE 2 & PHASE 9)
        String invoiceNumber = "INV-" + getShortOrderCode(orderId) + "-" + paymentType + "-" + System.currentTimeMillis() % 1000000;

        // Tạo bản ghi Payment với trạng thái PENDING
        Payment payment = Payment.builder()
                .id(UUID.randomUUID().toString())
                .order(order)
                .paymentType(paymentType)
                .paymentMethod(PaymentMethod.SEPAY)
                .amount(payableAmount)
                .invoiceNumber(invoiceNumber)
                .status(PaymentStatus.PENDING)
                .createdBy(order.getCustomer() != null ? order.getCustomer().getId() : "SYSTEM")
                .build();

        paymentRepository.save(payment);

        // PHASE 6: Xây dựng các fields thanh toán và chữ ký HMAC-SHA256 theo chuẩn tài liệu SePay
        long amountLong = payableAmount.longValue();
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("merchant", merchantId);
        fields.put("currency", "VND");
        fields.put("order_amount", String.valueOf(amountLong));
        fields.put("operation", "PURCHASE");
        fields.put("order_description", "Thanh toan don hoa #" + getShortOrderCode(orderId) + " (" + paymentType + ")");
        fields.put("order_invoice_number", invoiceNumber);
        if (order.getCustomer() != null) {
            fields.put("customer_id", order.getCustomer().getId());
        }
        fields.put("success_url", successUrl + "?invoice_number=" + invoiceNumber);
        fields.put("error_url", errorUrl + "?invoice_number=" + invoiceNumber);
        fields.put("cancel_url", cancelUrl + "?invoice_number=" + invoiceNumber);

        if (request.getPaymentMethod() != null && !request.getPaymentMethod().isBlank()) {
            fields.put("payment_method", request.getPaymentMethod());
        }

        // Tạo chữ ký HMAC-SHA256
        String signature = SePaySignatureUtil.generateSignature(fields, secretKey);
        fields.put("signature", signature);

        // PHASE 18: Ghi log an toàn (KHÔNG log secretKey)
        log.info("Tạo yêu cầu thanh toán SePay thành công: invoiceNumber={}, orderId={}, amount={}đ, type={}",
                invoiceNumber, orderId, amountLong, paymentType);

        return CreatePaymentResponse.builder()
                .checkoutUrl(checkoutUrl)
                .paymentId(payment.getId())
                .invoiceNumber(invoiceNumber)
                .orderId(orderId)
                .amount(payableAmount)
                .fields(fields)
                .build();
    }

    /**
     * Backward-compatible: Khởi tạo cho controller cũ
     */
    @Transactional
    public SePayCheckoutFormResponse initiateCheckout(String orderId) {
        CreatePaymentResponse res = createPayment(
                CreatePaymentRequest.builder().orderId(orderId).paymentType(PaymentType.FULL).build(),
                null
        );
        return SePayCheckoutFormResponse.builder()
                .checkoutUrl(res.getCheckoutUrl())
                .fields(res.getFields())
                .build();
    }

    /**
     * PHASE 8 & 9: Xử lý IPN (Instant Payment Notification) từ SePay Gateway đảm bảo Idempotent & Atomic
     */
    @Transactional
    public Map<String, Object> handleIpn(SePayIpnPayload payload, String xSecretKeyHeader) {
        log.info("Nhận IPN callback từ SePay Gateway: {}", payload);

        // Kiểm tra bí mật xác thực nếu có cấu hình
        if (ipnSecret != null && !ipnSecret.isBlank()) {
            if (!ipnSecret.equals(xSecretKeyHeader) && !secretKey.equals(xSecretKeyHeader)) {
                log.warn("SePay IPN: Sai bí mật xác thực X-Secret-Key!");
                throw ApiException.forbidden("Invalid IPN secret key");
            }
        }

        if (payload == null || !"ORDER_PAID".equalsIgnoreCase(payload.getNotificationType())) {
            log.info("Bỏ qua IPN sự kiện không phải ORDER_PAID: {}", payload != null ? payload.getNotificationType() : "null");
            return Map.of("success", true, "message", "Ignored non-ORDER_PAID event");
        }

        SePayIpnPayload.IpnOrder ipnOrder = payload.getOrder();
        if (ipnOrder == null || ipnOrder.getOrderInvoiceNumber() == null) {
            log.warn("IPN: Thiếu order_invoice_number trong payload");
            return Map.of("success", false, "message", "Missing order_invoice_number");
        }

        String invoiceNumber = ipnOrder.getOrderInvoiceNumber();

        // Tìm kiếm Payment theo invoiceNumber
        Payment payment = paymentRepository.findByInvoiceNumber(invoiceNumber)
                .orElse(null);

        // Fallback: Thử tìm theo order ID nếu là flow cũ
        if (payment == null) {
            payment = paymentRepository.findTopByOrderIdOrderByCreatedDateDesc(invoiceNumber)
                    .orElse(null);
        }

        if (payment == null) {
            log.warn("IPN: Không tìm thấy Payment tương ứng với invoiceNumber: {}", invoiceNumber);
            return Map.of("success", false, "message", "Payment not found for invoice " + invoiceNumber);
        }

        // PHASE 9: Chống trùng lặp (Idempotency)
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            log.info("Giao dịch cho invoice {} đã được cập nhật thành công trước đó (Idempotent bypass).", invoiceNumber);
            return Map.of("success", true, "message", "Payment already processed");
        }

        // Lấy thông tin giao dịch từ SePay
        SePayIpnPayload.IpnTransaction transaction = payload.getTransaction();
        String transactionId = transaction != null && transaction.getTransactionId() != null
                ? transaction.getTransactionId()
                : UUID.randomUUID().toString();

        BigDecimal paidAmount = transaction != null && transaction.getTransactionAmount() != null
                ? transaction.getTransactionAmount()
                : (ipnOrder.getOrderAmount() != null ? ipnOrder.getOrderAmount() : BigDecimal.ZERO);

        // PHASE 13: Kiểm tra khớp số tiền (Amount Validation)
        if (payment.getAmount().compareTo(paidAmount) != 0) {
            log.error("PHÁT HIỆN LỆCH TIỀN: invoice={}, Yêu cầu={}đ, SePay báo={}đ",
                    invoiceNumber, payment.getAmount(), paidAmount);
            payment.setStatus(PaymentStatus.FAILED);
            payment.setGatewayResponse(String.format(
                    "{\"error\":\"AMOUNT_MISMATCH\",\"expected\":%s,\"received\":%s}",
                    payment.getAmount(), paidAmount));
            paymentRepository.save(payment);
            return Map.of("success", false, "message", "Amount mismatch error");
        }

        // Cập nhật trạng thái Payment -> SUCCESS
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setGatewayTransactionNo(transactionId);
        payment.setGatewayResponse(String.format(
                "{\"notification_type\":\"ORDER_PAID\",\"transactionId\":\"%s\",\"amount\":%s,\"invoiceNumber\":\"%s\"}",
                transactionId, paidAmount, invoiceNumber));
        payment.setLastModifyBy("SEPAY_IPN");
        paymentRepository.save(payment);

        // Cập nhật trạng thái Order tương ứng
        Order order = payment.getOrder();
        if (payment.getPaymentType() == PaymentType.FULL) {
            order.setStatus(OrderStatus.PROCESSING);
            order.setDepositAmount(paidAmount);
        } else if (payment.getPaymentType() == PaymentType.DEPOSIT) {
            order.setStatus(OrderStatus.DEPOSIT_PAID);
            order.setDepositAmount(paidAmount);
        } else if (payment.getPaymentType() == PaymentType.FINAL) {
            order.setStatus(OrderStatus.PROCESSING);
        }
        orderRepository.save(order);

        log.info("Xác nhận thanh toán thành công cho đơn hàng: {}, invoice: {}, số tiền: {}đ",
                order.getId(), invoiceNumber, paidAmount);

        return Map.of("success", true, "message", "Payment confirmed successfully");
    }

    /**
     * Tự động chủ động đối soát trực tiếp với Cổng SePay qua REST API
     * (Khắc phục hạn chế khi chạy localhost không nhận được Webhook từ Internet)
     */
    @Transactional
    public boolean reconcileWithSePayGateway(Payment payment) {
        if (payment == null || payment.getInvoiceNumber() == null) return false;
        if (payment.getStatus() == PaymentStatus.SUCCESS) return true;

        try {
            String invoiceNumber = payment.getInvoiceNumber();
            String auth = Base64.getEncoder().encodeToString(
                    (merchantId + ":" + secretKey).getBytes(StandardCharsets.UTF_8)
            );

            // API Endpoint kiểm tra đơn hàng theo chuẩn tài liệu SePay Gateway
            String pgApiUrl = "https://pgapi.sepay.vn/v1/order/detail/" + invoiceNumber;

            java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(pgApiUrl))
                    .header("Authorization", "Basic " + auth)
                    .header("Accept", "application/json")
                    .timeout(java.time.Duration.ofSeconds(6))
                    .GET()
                    .build();

            java.net.http.HttpResponse<String> resp = java.net.http.HttpClient.newHttpClient()
                    .send(req, java.net.http.HttpResponse.BodyHandlers.ofString());

            if (resp.statusCode() == 200 && resp.body() != null) {
                String body = resp.body();
                log.info("Kết quả đối soát từ SePay pgapi cho invoice {}: {}", invoiceNumber, body);

                Matcher stMatcher = Pattern.compile("\"order_status\"\\s*:\\s*\"([^\"]+)\"").matcher(body);
                if (stMatcher.find()) {
                    String sepayStatus = stMatcher.group(1);
                    if ("CAPTURED".equalsIgnoreCase(sepayStatus)) {
                        String transactionNo = null;
                        Matcher txMatcher = Pattern.compile("\"order_id\"\\s*:\\s*\"([^\"]+)\"").matcher(body);
                        if (txMatcher.find()) {
                            transactionNo = txMatcher.group(1);
                        }

                        BigDecimal paidAmount = payment.getAmount();
                        Matcher amtMatcher = Pattern.compile("\"order_amount\"\\s*:\\s*\"([^\"]+)\"").matcher(body);
                        if (amtMatcher.find()) {
                            try {
                                paidAmount = new BigDecimal(amtMatcher.group(1));
                            } catch (Exception ignored) {}
                        }

                        // Cập nhật Payment -> SUCCESS
                        payment.setStatus(PaymentStatus.SUCCESS);
                        payment.setGatewayTransactionNo(transactionNo != null ? transactionNo : "SEPAY-CAPTURED");
                        payment.setGatewayResponse(body);
                        payment.setLastModifyBy("SEPAY_AUTO_RECONCILE");
                        paymentRepository.save(payment);

                        // Cập nhật Order tương ứng
                        Order order = payment.getOrder();
                        if (order != null) {
                            if (payment.getPaymentType() == PaymentType.FULL) {
                                order.setStatus(OrderStatus.PROCESSING);
                                order.setDepositAmount(paidAmount);
                            } else if (payment.getPaymentType() == PaymentType.DEPOSIT) {
                                order.setStatus(OrderStatus.DEPOSIT_PAID);
                                order.setDepositAmount(paidAmount);
                            } else if (payment.getPaymentType() == PaymentType.FINAL) {
                                order.setStatus(OrderStatus.PROCESSING);
                            }
                            orderRepository.save(order);
                        }

                        log.info("Đối soát thành công! Đơn hàng {} đã được xác nhận thanh toán qua SePay.", order != null ? order.getId() : "null");
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Không thể kết nối đối soát trực tiếp với SePay pgapi: {}", e.getMessage());
        }
        return false;
    }

    /**
     * PHASE 11: Truy vấn trạng thái thanh toán theo invoiceNumber (kèm đối soát chủ động thời gian thực)
     */
    @Transactional
    public PaymentStatusResponse getStatusByInvoice(String invoiceNumber, String authenticatedUserId) {
        Payment payment = paymentRepository.findByInvoiceNumber(invoiceNumber)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy thông tin thanh toán với mã hóa đơn: " + invoiceNumber));

        // Kiểm tra quyền truy cập nếu có authenticatedUserId
        if (authenticatedUserId != null && !authenticatedUserId.isBlank()
                && payment.getOrder() != null
                && payment.getOrder().getCustomer() != null
                && !payment.getOrder().getCustomer().getId().equals(authenticatedUserId)) {
            throw ApiException.forbidden("Bạn không có quyền xem thông tin giao dịch này.");
        }

        // Tự động đối soát trực tiếp với SePay pgapi nếu trạng thái chưa là SUCCESS
        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            reconcileWithSePayGateway(payment);
        }

        boolean isPaid = payment.getStatus() == PaymentStatus.SUCCESS;
        return PaymentStatusResponse.builder()
                .paymentId(payment.getId())
                .orderId(payment.getOrder().getId())
                .invoiceNumber(payment.getInvoiceNumber())
                .orderStatus(payment.getOrder().getStatus().name())
                .paymentStatus(payment.getStatus().name())
                .isPaid(isPaid)
                .amount(payment.getAmount())
                .paidAmount(isPaid ? payment.getAmount() : BigDecimal.ZERO)
                .paymentType(payment.getPaymentType() != null ? payment.getPaymentType().name() : "FULL")
                .transactionNo(payment.getGatewayTransactionNo())
                .message(isPaid ? "Thanh toán thành công qua SePay Gateway!" : "Đang chờ thanh toán...")
                .paidAt(payment.getLastModifyDate())
                .build();
    }

    /**
     * Truy vấn trạng thái theo paymentId (kèm đối soát chủ động thời gian thực)
     */
    @Transactional
    public PaymentStatusResponse getStatusById(String paymentId, String authenticatedUserId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy thông tin thanh toán với id: " + paymentId));

        if (authenticatedUserId != null && !authenticatedUserId.isBlank()
                && payment.getOrder() != null
                && payment.getOrder().getCustomer() != null
                && !payment.getOrder().getCustomer().getId().equals(authenticatedUserId)) {
            throw ApiException.forbidden("Bạn không có quyền xem thông tin giao dịch này.");
        }

        // Tự động đối soát trực tiếp với SePay pgapi nếu trạng thái chưa là SUCCESS
        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            reconcileWithSePayGateway(payment);
        }

        boolean isPaid = payment.getStatus() == PaymentStatus.SUCCESS;
        return PaymentStatusResponse.builder()
                .paymentId(payment.getId())
                .orderId(payment.getOrder().getId())
                .invoiceNumber(payment.getInvoiceNumber())
                .orderStatus(payment.getOrder().getStatus().name())
                .paymentStatus(payment.getStatus().name())
                .isPaid(isPaid)
                .amount(payment.getAmount())
                .paidAmount(isPaid ? payment.getAmount() : BigDecimal.ZERO)
                .paymentType(payment.getPaymentType() != null ? payment.getPaymentType().name() : "FULL")
                .transactionNo(payment.getGatewayTransactionNo())
                .message(isPaid ? "Thanh toán thành công qua SePay Gateway!" : "Đang chờ thanh toán...")
                .paidAt(payment.getLastModifyDate())
                .build();
    }
}

