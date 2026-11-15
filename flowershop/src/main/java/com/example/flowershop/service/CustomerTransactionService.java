package com.example.flowershop.service;

import com.example.flowershop.entity.Payment;
import com.example.flowershop.entity.Refund;
import com.example.flowershop.entity.enums.OrderStatus;
import com.example.flowershop.entity.enums.PaymentStatus;
import com.example.flowershop.repository.PaymentRepository;
import com.example.flowershop.repository.RefundRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/** Lịch sử giao dịch của khách (SRS UC22 mục 19.2): các lần thanh toán và các khoản hoàn tiền, chỉ của chính khách. */
@Service
@Transactional(readOnly = true)
public class CustomerTransactionService {
    static final int PAGE_SIZE = 20;

    /** invoiceAvailable: đơn chưa hủy và các khoản đã thanh toán thành công đủ tổng tiền. */
    public record TransactionLine(String id, String orderId, String shopName, String paymentType, String paymentMethod,
                                  BigDecimal amount, String status, String invoiceNumber, String gatewayTransactionNo,
                                  LocalDateTime createdDate, LocalDateTime paidAt, boolean invoiceAvailable) {}

    public record RefundTransactionLine(String id, String orderId, String shopName, BigDecimal amount, String status,
                                        String reason, LocalDateTime createdDate, LocalDateTime updatedDate) {}

    /** totalPaid: tổng thanh toán thành công; totalRefunded: tổng hoàn tiền đã chuyển; cả hai theo khoảng ngày, không theo bộ lọc trạng thái.
     *  refunds chỉ có khi không lọc trạng thái thanh toán. */
    public record Results(List<TransactionLine> content, int page, int totalPages, long totalElements,
                          BigDecimal totalPaid, BigDecimal totalRefunded, List<RefundTransactionLine> refunds) {}

    private final PaymentRepository payments;
    private final RefundRepository refunds;

    public CustomerTransactionService(PaymentRepository payments, RefundRepository refunds) {
        this.payments = payments;
        this.refunds = refunds;
    }

    public Results list(String customerId, int page, String status, LocalDate from, LocalDate to) {
        if (from != null && to != null && to.isBefore(from)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ngày kết thúc phải sau hoặc bằng ngày bắt đầu.");
        }
        List<PaymentStatus> statuses = List.of(PaymentStatus.values());
        if (status != null && !status.isBlank()) {
            try {
                statuses = List.of(PaymentStatus.valueOf(status.trim().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Trạng thái giao dịch không hợp lệ.");
            }
        }
        boolean allStatuses = statuses.size() > 1;
        LocalDateTime start = from != null ? from.atStartOfDay() : LocalDateTime.of(1970, 1, 1, 0, 0);
        LocalDateTime end = to != null ? to.plusDays(1).atStartOfDay() : LocalDateTime.of(9999, 1, 1, 0, 0);

        Page<Payment> result = payments.findForCustomer(customerId, statuses, start, end,
                PageRequest.of(Math.max(page, 0), PAGE_SIZE, Sort.by(Sort.Order.desc("createdDate"), Sort.Order.asc("id"))));

        Set<String> orderIds = new HashSet<>();
        result.getContent().forEach(p -> orderIds.add(p.getOrder().getId()));
        Map<String, BigDecimal> paidByOrder = new HashMap<>();
        if (!orderIds.isEmpty()) {
            for (Object[] row : payments.sumSuccessByOrderIds(orderIds)) paidByOrder.put((String) row[0], (BigDecimal) row[1]);
        }
        var content = result.getContent().stream().map(p -> {
            var o = p.getOrder();
            BigDecimal paid = paidByOrder.getOrDefault(o.getId(), BigDecimal.ZERO);
            boolean invoice = o.getStatus() != OrderStatus.CANCELLED && o.getTotalAmount() != null
                    && paid.signum() > 0 && paid.compareTo(o.getTotalAmount()) >= 0;
            return new TransactionLine(p.getId(), o.getId(), o.getShop().getName(), p.getPaymentType().name(),
                    p.getPaymentMethod().name(), p.getAmount(), p.getStatus().name(), p.getInvoiceNumber(),
                    p.getGatewayTransactionNo(), p.getCreatedDate(),
                    p.getStatus() == PaymentStatus.SUCCESS ? p.getLastModifyDate() : null, invoice);
        }).toList();

        List<RefundTransactionLine> refundLines = allStatuses
                ? refunds.findForCustomer(customerId, start, end).stream().map(this::refundLine).toList()
                : List.of();
        return new Results(content, result.getNumber(), result.getTotalPages(), result.getTotalElements(),
                payments.sumSuccessForCustomer(customerId, start, end),
                refunds.sumProcessedForCustomer(customerId, start, end), refundLines);
    }

    private RefundTransactionLine refundLine(Refund r) {
        return new RefundTransactionLine(r.getId(), r.getOrder().getId(), r.getOrder().getShop().getName(), r.getAmount(),
                r.getStatus().name(), r.getReason(), r.getCreatedDate(), r.getLastModifyDate());
    }
}
