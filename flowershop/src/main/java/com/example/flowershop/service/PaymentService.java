package com.example.flowershop.service;

import com.example.flowershop.dto.payment.PaymentQrResponse;
import com.example.flowershop.dto.payment.PaymentStatusResponse;
import com.example.flowershop.dto.payment.SePayWebhookDto;
import com.example.flowershop.entity.Order;
import com.example.flowershop.entity.Payment;
import com.example.flowershop.entity.enums.OrderStatus;
import com.example.flowershop.entity.enums.PaymentMethod;
import com.example.flowershop.entity.enums.PaymentStatus;
import com.example.flowershop.entity.enums.PaymentType;
import com.example.flowershop.exception.ApiException;
import com.example.flowershop.repository.OrderRepository;
import com.example.flowershop.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final SePayGatewayService sePayGatewayService;

    @Value("${sepay.bank-id:MB}")
    private String bankId;

    @Value("${sepay.account-number:0912345678}")
    private String accountNumber;

    @Value("${sepay.account-name:FLOWERSHOP}")
    private String accountName;

    @Value("${sepay.va-prefix:TKPNXT}")
    private String vaPrefix;

    @Value("${sepay.api-key:YOUR_SEPAY_API_KEY}")
    private String apiKey;

    /**
     * Tạo mã chuyển khoản ngắn gọn từ Order ID (dạng DH + 8 ký tự hex viết hoa).
     * Ví dụ: Order ID "18e6c055-895d-4308-930d-b837a89c5eb7" -> "DH18E6C055"
     */
    public String getShortOrderCode(String orderId) {
        if (orderId == null) return "DH";
        String clean = orderId.replace("-", "").toUpperCase();
        return "DH" + (clean.length() >= 8 ? clean.substring(0, 8) : clean);
    }

    /**
     * 1. Sinh thông tin thanh toán VietQR cho đơn hàng
     */
    @Transactional(readOnly = true)
    public PaymentQrResponse generatePaymentQr(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy đơn hàng với id: " + orderId));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw ApiException.badRequest("Đơn hàng này đã được thanh toán hoặc không ở trạng thái chờ.");
        }

        String orderCode = getShortOrderCode(orderId);
        String transferContent = (vaPrefix != null && !vaPrefix.isBlank() ? vaPrefix + " " : "") + orderCode;

        // Tạo link ảnh VietQR động chuẩn NAPAS
        String encodedAccountName = URLEncoder.encode(accountName, StandardCharsets.UTF_8);
        String encodedContent = URLEncoder.encode(transferContent, StandardCharsets.UTF_8);
        long amountLong = order.getTotalAmount().longValue();

        String qrUrl = String.format(
                "https://img.vietqr.io/image/%s-%s-compact2.png?amount=%d&addInfo=%s&accountName=%s",
                bankId,
                accountNumber,
                amountLong,
                encodedContent,
                encodedAccountName
        );

        return PaymentQrResponse.builder()
                .orderId(order.getId())
                .orderCode(orderCode)
                .amount(order.getTotalAmount())
                .bankId(bankId)
                .accountNumber(accountNumber)
                .accountName(accountName)
                .transferContent(transferContent)
                .qrUrl(qrUrl)
                .paymentStatus(order.getStatus().name())
                .build();
    }

    /**
     * 2. Xử lý Webhook gửi từ SePay (khi tiền nổ vào tài khoản ngân hàng)
     */
    @Transactional
    public Map<String, Object> processSePayWebhook(SePayWebhookDto webhookDto, String authHeader) {
        log.info("Nhận Webhook từ SePay: {}", webhookDto);

        // Kiểm tra API Key nếu có cấu hình
        if (apiKey != null && !apiKey.equals("YOUR_SEPAY_API_KEY") && !apiKey.isBlank()) {
            String token = authHeader != null && authHeader.startsWith("Apikey ")
                    ? authHeader.substring(7).trim()
                    : authHeader;
            if (!apiKey.equals(token)) {
                log.warn("SePay Webhook: Sai API Key xác thực!");
                throw ApiException.forbidden("Unauthorized webhook request");
            }
        }

        // Bỏ qua nếu là giao dịch tiền ra
        if ("out".equalsIgnoreCase(webhookDto.getTransferType())) {
            log.info("Bỏ qua giao dịch tiền ra khỏi tài khoản.");
            return Map.of("success", true, "message", "Ignored transfer out");
        }

        String refCode = webhookDto.getReferenceCode() != null && !webhookDto.getReferenceCode().isBlank()
                ? webhookDto.getReferenceCode()
                : (webhookDto.getId() != null ? String.valueOf(webhookDto.getId()) : UUID.randomUUID().toString());

        // Kiểm tra chống trùng lặp (Idempotency)
        if (paymentRepository.existsByGatewayTransactionNo(refCode)) {
            log.warn("Giao dịch {} đã được xử lý trước đó, bỏ qua để tránh trùng lặp.", refCode);
            return Map.of("success", true, "message", "Transaction already processed");
        }

        // Trích xuất mã đơn hàng từ nội dung chuyển khoản
        String content = webhookDto.getContent() != null ? webhookDto.getContent() : "";
        Order matchedOrder = findMatchingOrder(content);

        if (matchedOrder == null) {
            log.warn("Không tìm thấy đơn hàng khớp với nội dung chuyển khoản: '{}'", content);
            return Map.of("success", false, "message", "Order not found from content");
        }

        BigDecimal transferAmount = webhookDto.getTransferAmount() != null
                ? webhookDto.getTransferAmount()
                : BigDecimal.ZERO;

        // Cập nhật trạng thái đơn hàng sang PROCESSING
        matchedOrder.setStatus(OrderStatus.PROCESSING);
        matchedOrder.setDepositAmount(transferAmount);
        orderRepository.save(matchedOrder);

        // Lưu thông tin thanh toán vào bảng Payments
        Payment payment = Payment.builder()
                .id(UUID.randomUUID().toString())
                .order(matchedOrder)
                .paymentType(PaymentType.FULL)
                .paymentMethod(PaymentMethod.BANK_TRANSFER)
                .amount(transferAmount)
                .gatewayTransactionNo(refCode)
                .gatewayResponse(String.format(
                        "{\"gateway\":\"%s\",\"referenceCode\":\"%s\",\"transferAmount\":%s}",
                        webhookDto.getGateway() != null ? webhookDto.getGateway() : "BANK",
                        refCode,
                        transferAmount
                ))
                .status(PaymentStatus.SUCCESS)
                .createdBy("SEPAY_WEBHOOK")
                .lastModifyBy("SEPAY_WEBHOOK")
                .build();

        paymentRepository.save(payment);
        log.info("Thanh toán thành công cho đơn hàng: {} với số tiền: {}đ", matchedOrder.getId(), transferAmount);

        return Map.of(
                "success", true,
                "message", "Thanh toán thành công!",
                "orderId", matchedOrder.getId()
        );
    }

    /**
     * 3. Kiểm tra trạng thái đơn hàng cho Frontend Polling (kèm đối soát chủ động thời gian thực)
     */
    @Transactional
    public PaymentStatusResponse checkPaymentStatus(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy đơn hàng với id: " + orderId));

        // Nếu có bản ghi payment chưa thành công -> chủ động đối soát với SePay
        Optional<Payment> latestPaymentOpt = paymentRepository.findTopByOrderIdOrderByCreatedDateDesc(orderId);
        if (latestPaymentOpt.isPresent()) {
            Payment payment = latestPaymentOpt.get();
            if (payment.getStatus() != PaymentStatus.SUCCESS) {
                sePayGatewayService.reconcileWithSePayGateway(payment);
            }
        }

        boolean isPaid = order.getStatus() == OrderStatus.PROCESSING
                || order.getStatus() == OrderStatus.DELIVERING
                || order.getStatus() == OrderStatus.COMPLETED
                || order.getStatus() == OrderStatus.DEPOSIT_PAID;

        Optional<Payment> latestPayment = paymentRepository.findTopByOrderIdOrderByCreatedDateDesc(orderId);

        return PaymentStatusResponse.builder()
                .orderId(order.getId())
                .orderStatus(order.getStatus().name())
                .isPaid(isPaid)
                .paidAmount(latestPayment.map(Payment::getAmount).orElse(BigDecimal.ZERO))
                .transactionNo(latestPayment.map(Payment::getGatewayTransactionNo).orElse(null))
                .message(isPaid ? "Đơn hàng đã được thanh toán thành công!" : "Đang chờ thanh toán...")
                .build();
    }

    private Order findMatchingOrder(String content) {
        Pattern pattern = Pattern.compile("(?i)DH([a-zA-Z0-9]+)");
        Matcher matcher = pattern.matcher(content);

        if (matcher.find()) {
            String candidateCode = matcher.group(1).toUpperCase();

            List<Order> pendingOrders = orderRepository.findAll();
            for (Order o : pendingOrders) {
                String shortCode = o.getId().replace("-", "").toUpperCase();
                if (shortCode.startsWith(candidateCode) || candidateCode.startsWith(shortCode.substring(0, Math.min(8, shortCode.length())))) {
                    return o;
                }
            }
        }

        List<Order> allOrders = orderRepository.findAll();
        for (Order o : allOrders) {
            if (content.toLowerCase().contains(o.getId().toLowerCase())) {
                return o;
            }
        }

        return null;
    }
}

