package com.example.flowershop.controller;

import com.example.flowershop.dto.payment.*;
import com.example.flowershop.service.PaymentService;
import com.example.flowershop.service.SePayGatewayService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.example.flowershop.dto.auth.CurrentUser;

import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PaymentController {

    private final SePayGatewayService sePayGatewayService;
    private final PaymentService paymentService;

    /**
     * PHASE 5: Khởi tạo phiên thanh toán SePay Gateway (an toàn, tính tiền server-side).
     * Trả checkoutUrl + các field đã ký HMAC để FE POST form sang cổng SePay. Chỉ chủ đơn mới tạo được.
     * POST /api/payments/sepay/create
     */
    @PostMapping("/payments/sepay/create")
    public ResponseEntity<CreatePaymentResponse> createPayment(
            @Valid @RequestBody CreatePaymentRequest request,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        CreatePaymentResponse response = sePayGatewayService.createPayment(request, currentUser.id());
        return ResponseEntity.ok(response);
    }

    /**
     * Thanh toán gộp nhiều đơn (mỗi shop một đơn) trong một giao dịch SePay
     * POST /api/payments/sepay/create-group
     */
    @PostMapping("/payments/sepay/create-group")
    public ResponseEntity<CreatePaymentResponse> createGroupPayment(
            @Valid @RequestBody com.example.flowershop.dto.payment.CreateGroupPaymentRequest request,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return ResponseEntity.ok(sePayGatewayService.createGroupPayment(request, currentUser.id()));
    }

    /**
     * Khách đóng/hủy cổng SePay: hủy giao dịch + đơn tương ứng.
     * POST /api/payments/sepay/cancel/{invoiceNumber} — trả {"paid": true} nếu thực ra đã thanh toán (không hủy được).
     */
    @PostMapping("/payments/sepay/cancel/{invoiceNumber}")
    public Map<String, Boolean> cancelCheckout(@PathVariable String invoiceNumber,
                                               @AuthenticationPrincipal CurrentUser currentUser) {
        return Map.of("paid", sePayGatewayService.cancelCheckout(invoiceNumber, currentUser.id()));
    }

    /**
     * Khởi tạo form thanh toán theo orderId (endpoint tương thích với bản cũ)
     * POST /api/sepay/checkout/{orderId}
     */
    @PostMapping("/sepay/checkout/{orderId}")
    public ResponseEntity<SePayCheckoutFormResponse> initiateCheckout(@PathVariable String orderId, @AuthenticationPrincipal CurrentUser currentUser) {
        return ResponseEntity.ok(sePayGatewayService.initiateCheckout(orderId, currentUser.id()));
    }

    /**
     * PHASE 11: Truy vấn trạng thái thanh toán theo paymentId
     * GET /api/payments/{paymentId}
     */
    @GetMapping("/payments/{paymentId}")
    public ResponseEntity<PaymentStatusResponse> getPaymentById(
            @PathVariable String paymentId,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return ResponseEntity.ok(sePayGatewayService.getStatusById(paymentId, currentUser.id()));
    }

    /**
     * PHASE 10 & 11: Truy vấn trạng thái thanh toán theo invoiceNumber (cho Polling kết quả)
     * GET /api/payments/status-by-invoice/{invoiceNumber}
     */
    @GetMapping("/payments/status-by-invoice/{invoiceNumber}")
    public ResponseEntity<PaymentStatusResponse> getPaymentByInvoice(
            @PathVariable String invoiceNumber,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return ResponseEntity.ok(sePayGatewayService.getStatusByInvoice(invoiceNumber, currentUser.id()));
    }

    /**
     * Truy vấn trạng thái thanh toán theo orderId (có đối soát chủ động với SePay trước khi trả kết quả)
     * GET /api/payments/status-by-order/{orderId} và GET /api/payment/status/{orderId}
     */
    @GetMapping({"/payments/status-by-order/{orderId}", "/payment/status/{orderId}"})
    public ResponseEntity<PaymentStatusResponse> getPaymentByOrder(@PathVariable String orderId, @AuthenticationPrincipal CurrentUser currentUser) {
        return ResponseEntity.ok(paymentService.checkPaymentStatus(orderId, currentUser.id()));
    }

    /**
     * Sinh thông tin VietQR trực tiếp
     * POST /api/payments/create-qr/{orderId} và POST /api/payment/create-qr/{orderId}
     */
    @PostMapping({"/payments/create-qr/{orderId}", "/payment/create-qr/{orderId}"})
    public ResponseEntity<PaymentQrResponse> createQrPayment(@PathVariable String orderId, @AuthenticationPrincipal CurrentUser currentUser) {
        return ResponseEntity.ok(paymentService.generatePaymentQr(orderId, currentUser.id()));
    }

    /**
     * Nhận Webhook giao dịch tài khoản ngân hàng trực tiếp từ SePay (không dùng JWT, xác thực bằng API key ở header Authorization)
     * POST /api/webhook/sepay
     */
    @PostMapping("/webhook/sepay")
    public ResponseEntity<Map<String, Object>> receiveBankWebhook(
            @RequestBody SePayWebhookDto webhookDto,
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        Map<String, Object> result = paymentService.processSePayWebhook(webhookDto, authHeader);
        return ResponseEntity.ok(result);
    }
}
