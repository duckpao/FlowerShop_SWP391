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
     * PHASE 5: Khởi tạo phiên thanh toán SePay Gateway (an toàn, tính tiền server-side)
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
     * Khởi tạo form thanh toán theo orderId (endpoint tương thích)
     * POST /api/sepay/checkout/{orderId}
     */
    @PostMapping("/sepay/checkout/{orderId}")
    public ResponseEntity<SePayCheckoutFormResponse> initiateCheckout(@PathVariable String orderId, @AuthenticationPrincipal CurrentUser currentUser) {
        return ResponseEntity.ok(sePayGatewayService.initiateCheckout(orderId, currentUser.id()));
    }

    /**
     * PHASE 8 & 9: Endpoint IPN webhook nhận callback từ SePay Gateway
     * POST /api/payments/sepay/ipn và POST /api/sepay/ipn
     */
    @PostMapping({"/payments/sepay/ipn", "/sepay/ipn"})
    public ResponseEntity<Map<String, Object>> handleIpn(
            @RequestBody SePayIpnPayload payload,
            @RequestHeader(value = "X-Secret-Key", required = false) String xSecretKey
    ) {
        Map<String, Object> result = sePayGatewayService.handleIpn(payload, xSecretKey);
        return ResponseEntity.ok(result);
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
     * Truy vấn trạng thái thanh toán theo orderId
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
     * Nhận Webhook giao dịch tài khoản ngân hàng trực tiếp từ SePay
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
