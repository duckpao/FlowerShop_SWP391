package com.example.flowershop.service;

import com.example.flowershop.dto.payment.PaymentStatusResponse;
import com.example.flowershop.entity.Order;
import com.example.flowershop.entity.Payment;
import com.example.flowershop.entity.enums.OrderStatus;
import com.example.flowershop.entity.enums.PaymentMethod;
import com.example.flowershop.entity.enums.PaymentStatus;
import com.example.flowershop.exception.ApiException;
import com.example.flowershop.repository.OrderRepository;
import com.example.flowershop.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final SePayGatewayService sePayGatewayService;

    /**
     * 3. Kiểm tra trạng thái đơn hàng cho Frontend Polling (kèm đối soát chủ động thời gian thực).
     * Nếu Payment ONLINE gần nhất chưa SUCCESS và đơn chưa hủy thì hỏi lại SePay (không có webhook, đây là cách duy nhất biết tiền đã về),
     * rồi trả isPaid, số tiền đã trả, mã giao dịch, thông báo và tiến trình đơn.
     * Không @Transactional: đối soát gọi HTTP sang SePay, không giữ khóa DB. Việc hủy đơn quá hạn do scheduler làm.
     */
    public PaymentStatusResponse checkPaymentStatus(String orderId, String userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy đơn hàng với id: " + orderId));
        requireOwner(order, userId);

        // Nếu có bản ghi payment chưa thành công và đơn chưa bị hủy -> chủ động đối soát với SePay
        if (order.getStatus() != OrderStatus.CANCELLED) {
            Optional<Payment> latestPaymentOpt = paymentRepository.findTopByOrderIdOrderByCreatedDateDesc(orderId);
            if (latestPaymentOpt.isPresent()) {
                Payment payment = latestPaymentOpt.get();
                if (payment.getPaymentMethod() == PaymentMethod.ONLINE && payment.getStatus() != PaymentStatus.SUCCESS) {
                    sePayGatewayService.reconcileWithSePayGateway(payment.getInvoiceNumber());
                }
            }
        }

        Optional<Payment> latestPayment = paymentRepository.findTopByOrderIdOrderByCreatedDateDesc(orderId);
        boolean isPaid = order.getStatus() != OrderStatus.CANCELLED
                && latestPayment.map(p -> p.getStatus() == PaymentStatus.SUCCESS).orElse(false);

        String message;
        if (isPaid) {
            message = "Đơn hàng đã được thanh toán thành công!";
        } else if (order.getStatus() == OrderStatus.CANCELLED) {
            message = "Đơn hàng đã bị hủy do hết thời gian chờ thanh toán ("
                    + SePayGatewayService.expiryText(order) + ").";
        } else {
            message = "Đang chờ thanh toán...";
        }

        PaymentStatusResponse.PaymentStatusResponseBuilder status = PaymentStatusResponse.builder()
                .orderId(order.getId())
                .orderStatus(order.getStatus().name())
                .isPaid(isPaid)
                .paidAmount(latestPayment.filter(p -> p.getStatus() == PaymentStatus.SUCCESS).map(Payment::getAmount).orElse(BigDecimal.ZERO))
                .transactionNo(latestPayment.map(Payment::getGatewayTransactionNo).orElse(null))
                .message(message);
        SePayGatewayService.addOrderProgress(status, order, paymentRepository.findByOrderId(orderId));
        return status.build();
    }

    /** Chặn xem/thanh toán đơn của người khác: userId phải là khách đặt đơn, nếu không trả 403. */
    private static void requireOwner(Order order, String userId) {
        if (userId == null || order.getCustomer() == null || !userId.equals(order.getCustomer().getId())) {
            throw ApiException.forbidden("Không có quyền truy cập đơn hàng này.");
        }
    }
}
