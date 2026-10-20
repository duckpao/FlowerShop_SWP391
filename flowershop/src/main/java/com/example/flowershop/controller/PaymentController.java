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
}
