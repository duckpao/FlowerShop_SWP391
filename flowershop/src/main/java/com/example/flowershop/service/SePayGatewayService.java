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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SePayGatewayService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final OrderService orderService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${sepay.merchant-id:SP-TEST-001}")
    private String merchantId;

    @Value("${sepay.secret-key:}")
    private String secretKey;

    @Value("${sepay.checkout-url:https://pay-sandbox.sepay.vn/v1/checkout/init}")
    private String checkoutUrl;

    @Value("${sepay.api-base-url:https://pgapi.sepay.vn}")
    private String apiBaseUrl;

    @Value("${sepay.success-url:http://localhost:8080/payment/success}")
    private String successUrl;

    @Value("${sepay.error-url:http://localhost:8080/payment/error}")
    private String errorUrl;

    @Value("${sepay.cancel-url:http://localhost:8080/payment/cancel}")
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
        if (secretKey == null || secretKey.isBlank() || merchantId == null || merchantId.isBlank()) {
            throw ApiException.badRequest("Cổng thanh toán chưa được cấu hình.");
        }
        String orderId = request.getOrderId();
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy đơn hàng với id: " + orderId));

        // PHASE 12: Xác thực quyền sở hữu đơn hàng
        if (authenticatedUserId == null || order.getCustomer() == null
                || !order.getCustomer().getId().equals(authenticatedUserId)) {
            throw ApiException.forbidden("Bạn không có quyền thanh toán cho đơn hàng này.");
        }

        // Kiểm tra trạng thái đơn hàng
        if (order.getStatus() == OrderStatus.COMPLETED) {
            throw ApiException.badRequest("Đơn hàng này đã hoàn thành, không thể thanh toán thêm.");
        }
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw ApiException.badRequest("Đơn hàng này đã bị hủy, không thể thanh toán.");
        }
        if (order.getStatus() == OrderStatus.PENDING && order.getCreatedDate() != null
                && order.getCreatedDate().plusMinutes(10).isBefore(java.time.LocalDateTime.now())) {
            orderService.cancelExpiredOrder(order.getId(), "Quá hạn 10 phút không quét mã thanh toán");
            throw ApiException.badRequest("Đơn hàng đã hết hạn thanh toán (quá 10 phút) và đã tự động hủy.");
        }
        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.DEPOSIT_PAID
                && order.getStatus() != OrderStatus.AWAITING_DEPOSIT) {
            throw ApiException.badRequest("Đơn hàng không còn chờ thanh toán.");
        }
        if (paymentRepository.findByOrderId(orderId).stream().anyMatch(p -> p.getPaymentMethod() == PaymentMethod.COD)) {
            throw ApiException.badRequest("Đơn COD không thể thanh toán trực tuyến.");
        }

        // PHASE 2 & 3: Xác định PaymentType (FULL, DEPOSIT, FINAL)
        PaymentType paymentType = request.getPaymentType() != null ? request.getPaymentType() : PaymentType.FULL;
        if (order.getOrderType() == com.example.flowershop.entity.enums.OrderType.STANDARD
                && paymentType != PaymentType.FULL) {
            throw ApiException.badRequest("Đơn hàng thường chỉ hỗ trợ thanh toán toàn bộ.");
        }
        if (order.getStatus() == OrderStatus.DEPOSIT_PAID && paymentType != PaymentType.FINAL) {
            throw ApiException.badRequest("Đơn đã trả cọc chỉ được thanh toán phần còn lại.");
        }
        if (paymentType == PaymentType.FINAL && order.getStatus() != OrderStatus.DEPOSIT_PAID) {
            throw ApiException.badRequest("Đơn hàng chưa thanh toán tiền cọc.");
        }
        if (paymentType == PaymentType.DEPOSIT && order.getStatus() == OrderStatus.DEPOSIT_PAID) {
            throw ApiException.badRequest("Tiền cọc đã được thanh toán.");
        }

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

        Payment payment = paymentRepository.findByOrderId(orderId).stream()
                .filter(p -> p.getPaymentMethod() == PaymentMethod.ONLINE
                        && p.getPaymentType() == paymentType && p.getStatus() == PaymentStatus.PENDING)
                .findFirst().orElse(null);
        if (payment == null) {
            payment = Payment.builder()
                    .id(UUID.randomUUID().toString())
                    .order(order)
                    .paymentType(paymentType)
                    .paymentMethod(PaymentMethod.ONLINE)
                    .amount(payableAmount)
                    .invoiceNumber("INV-" + getShortOrderCode(orderId) + "-" + paymentType + "-" + UUID.randomUUID().toString().substring(0, 8))
                    .status(PaymentStatus.PENDING)
                    .createdBy(order.getCustomer().getId())
                    .build();
            paymentRepository.save(payment);
        }
        String invoiceNumber = payment.getInvoiceNumber();

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
    public SePayCheckoutFormResponse initiateCheckout(String orderId, String authenticatedUserId) {
        CreatePaymentResponse res = createPayment(
                CreatePaymentRequest.builder().orderId(orderId).paymentType(PaymentType.FULL).build(),
                authenticatedUserId
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
        if (ipnSecret == null || ipnSecret.isBlank()) throw ApiException.forbidden("IPN chưa được cấu hình.");
        if (!ipnSecret.equals(xSecretKeyHeader)) {
            log.warn("SePay IPN: Sai bí mật xác thực X-Secret-Key!");
            throw ApiException.forbidden("Invalid IPN secret key");
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
        Payment payment = paymentRepository.findByInvoiceNumberForUpdate(invoiceNumber)
                .orElse(null);

        if (payment == null) {
            log.warn("IPN: Không tìm thấy Payment tương ứng với invoiceNumber: {}", invoiceNumber);
            return Map.of("success", false, "message", "Payment not found for invoice " + invoiceNumber);
        }

        // PHASE 9: Chống trùng lặp (Idempotency)
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            log.info("Giao dịch cho invoice {} đã được cập nhật thành công trước đó (Idempotent bypass).", invoiceNumber);
            return Map.of("success", true, "message", "Payment already processed");
        }
        if (payment.getOrder().getStatus() == OrderStatus.CANCELLED) {
            return Map.of("success", false, "message", "Order cancelled");
        }
        if (payment.getOrder().getStatus() == OrderStatus.PENDING && payment.getOrder().getCreatedDate() != null
                && payment.getOrder().getCreatedDate().plusMinutes(10).isBefore(java.time.LocalDateTime.now())) {
            orderService.cancelExpiredOrder(payment.getOrder().getId(), "Quá hạn 10 phút không quét mã thanh toán");
            return Map.of("success", false, "message", "Order expired and cancelled");
        }

        // Lấy thông tin giao dịch từ SePay
        SePayIpnPayload.IpnTransaction transaction = payload.getTransaction();
        if (transaction == null || !"APPROVED".equalsIgnoreCase(transaction.getTransactionStatus())) {
            return Map.of("success", false, "message", "Transaction not approved");
        }
        String transactionId = transaction != null && transaction.getTransactionId() != null
                ? transaction.getTransactionId()
                : UUID.randomUUID().toString();

        BigDecimal paidAmount = transaction != null && transaction.getTransactionAmount() != null
                ? transaction.getTransactionAmount()
                : (ipnOrder.getOrderAmount() != null ? ipnOrder.getOrderAmount() : BigDecimal.ZERO);

        // PHASE 13: Kiểm tra khớp số tiền (Amount Validation)
        if (payment.getAmount().compareTo(paidAmount) != 0
                || (ipnOrder.getOrderAmount() != null && payment.getAmount().compareTo(ipnOrder.getOrderAmount()) != 0)) {
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
        if (payment == null || payment.getPaymentMethod() != PaymentMethod.ONLINE || payment.getInvoiceNumber() == null) return false;
        if (payment.getStatus() == PaymentStatus.SUCCESS) return true;
        if (payment.getOrder() != null && payment.getOrder().getStatus() == OrderStatus.CANCELLED) return false;
        if (payment.getOrder() != null && payment.getOrder().getStatus() == OrderStatus.PENDING
                && payment.getOrder().getCreatedDate() != null
                && payment.getOrder().getCreatedDate().plusMinutes(10).isBefore(java.time.LocalDateTime.now())) {
            orderService.cancelExpiredOrder(payment.getOrder().getId(), "Quá hạn 10 phút không quét mã thanh toán");
            return false;
        }

        try {
            String invoiceNumber = payment.getInvoiceNumber();
            String auth = Base64.getEncoder().encodeToString(
                    (merchantId + ":" + secretKey).getBytes(StandardCharsets.UTF_8)
            );

            // API Endpoint kiểm tra đơn hàng theo chuẩn tài liệu SePay Gateway
            String pgApiUrl = apiBaseUrl.replaceAll("/$", "") + "/v1/order/detail/" + invoiceNumber;

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

                JsonNode root = objectMapper.readTree(body);
                JsonNode details = root.has("data") && root.get("data").isObject() ? root.get("data") : root;
                String sepayStatus = details.path("order_status").asText();
                if (!sepayStatus.isBlank()) {
                    if ("CAPTURED".equalsIgnoreCase(sepayStatus)) {
                        String transactionNo = details.path("order_id").asText(null);
                        String amountText = details.path("order_amount").asText(null);
                        String returnedInvoice = details.path("order_invoice_number").asText(null);
                        if (amountText == null || (returnedInvoice != null && !invoiceNumber.equals(returnedInvoice))) return false;
                        BigDecimal paidAmount = new BigDecimal(amountText);
                        if (payment.getAmount().compareTo(paidAmount) != 0 || payment.getOrder().getStatus() == OrderStatus.CANCELLED) {
                            log.warn("SePay amount, invoice or order status mismatch for invoice {}", invoiceNumber);
                            return false;
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
        Payment payment = paymentRepository.findByInvoiceNumberForUpdate(invoiceNumber)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy thông tin thanh toán với mã hóa đơn: " + invoiceNumber));

        // Kiểm tra quyền truy cập nếu có authenticatedUserId
        if (authenticatedUserId == null || payment.getOrder() == null
                || payment.getOrder().getCustomer() == null
                || !payment.getOrder().getCustomer().getId().equals(authenticatedUserId)) {
            throw ApiException.forbidden("Bạn không có quyền xem thông tin giao dịch này.");
        }

        // Tự động hủy nếu đã quá hạn 10 phút
        if (payment.getOrder() != null && payment.getOrder().getStatus() == OrderStatus.PENDING
                && payment.getOrder().getCreatedDate() != null
                && payment.getOrder().getCreatedDate().plusMinutes(10).isBefore(java.time.LocalDateTime.now())) {
            orderService.cancelExpiredOrder(payment.getOrder().getId(), "Quá hạn 10 phút không quét mã thanh toán");
            payment = paymentRepository.findByInvoiceNumberForUpdate(invoiceNumber).orElse(payment);
        }

        // Tự động đối soát trực tiếp với SePay pgapi nếu trạng thái chưa là SUCCESS và đơn chưa bị hủy
        if (payment.getStatus() != PaymentStatus.SUCCESS && payment.getOrder().getStatus() != OrderStatus.CANCELLED) {
            reconcileWithSePayGateway(payment);
        }

        boolean isPaid = payment.getStatus() == PaymentStatus.SUCCESS;
        String message;
        if (isPaid) {
            message = "Thanh toán thành công qua SePay Gateway!";
        } else if (payment.getOrder() != null && payment.getOrder().getStatus() == OrderStatus.CANCELLED) {
            message = "Đơn hàng đã bị hủy do quá thời gian thanh toán (10 phút).";
        } else {
            message = "Đang chờ thanh toán...";
        }

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
                .message(message)
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

        if (authenticatedUserId == null || payment.getOrder() == null
                || payment.getOrder().getCustomer() == null
                || !payment.getOrder().getCustomer().getId().equals(authenticatedUserId)) {
            throw ApiException.forbidden("Bạn không có quyền xem thông tin giao dịch này.");
        }

        // Tự động hủy nếu đã quá hạn 10 phút
        if (payment.getOrder() != null && payment.getOrder().getStatus() == OrderStatus.PENDING
                && payment.getOrder().getCreatedDate() != null
                && payment.getOrder().getCreatedDate().plusMinutes(10).isBefore(java.time.LocalDateTime.now())) {
            orderService.cancelExpiredOrder(payment.getOrder().getId(), "Quá hạn 10 phút không quét mã thanh toán");
            payment = paymentRepository.findById(paymentId).orElse(payment);
        }

        // Tự động đối soát trực tiếp với SePay pgapi nếu trạng thái chưa là SUCCESS và đơn chưa bị hủy
        if (payment.getStatus() != PaymentStatus.SUCCESS && payment.getOrder().getStatus() != OrderStatus.CANCELLED) {
            reconcileWithSePayGateway(payment);
        }

        boolean isPaid = payment.getStatus() == PaymentStatus.SUCCESS;
        String message;
        if (isPaid) {
            message = "Thanh toán thành công qua SePay Gateway!";
        } else if (payment.getOrder() != null && payment.getOrder().getStatus() == OrderStatus.CANCELLED) {
            message = "Đơn hàng đã bị hủy do quá thời gian thanh toán (10 phút).";
        } else {
            message = "Đang chờ thanh toán...";
        }

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
                .message(message)
                .paidAt(payment.getLastModifyDate())
                .build();
    }
}
