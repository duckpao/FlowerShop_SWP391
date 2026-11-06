package com.example.flowershop.service;

import com.example.flowershop.entity.Order;
import com.example.flowershop.entity.Refund;
import com.example.flowershop.entity.enums.RefundStatus;
import com.example.flowershop.repository.RefundRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Staff xử lý yêu cầu hoàn tiền của đơn đã hủy: xem thông tin đơn + SĐT khách để gọi,
 * rồi đánh dấu tiến độ REQUESTED → APPROVED (đã liên hệ) → PROCESSED (đã hoàn), hoặc REJECTED.
 */
@Service
public class StaffRefundService {

    public record RefundResponse(String id, String orderId, String customerName, String customerPhone,
                                 String recipientName, String recipientPhone, BigDecimal amount,
                                 String reason, String status, LocalDateTime createdDate) {}

    /** Các bước được phép đi tiếp; PROCESSED và REJECTED là trạng thái cuối. */
    private static final Map<RefundStatus, Set<RefundStatus>> NEXT = Map.of(
            RefundStatus.REQUESTED, EnumSet.of(RefundStatus.APPROVED, RefundStatus.REJECTED),
            RefundStatus.APPROVED, EnumSet.of(RefundStatus.PROCESSED, RefundStatus.REJECTED));

    private final RefundRepository refunds;
    private final ShopOperationGuard guard;
    private final NotificationService notifications;

    public StaffRefundService(RefundRepository refunds, ShopOperationGuard guard, NotificationService notifications) {
        this.refunds = refunds;
        this.guard = guard;
        this.notifications = notifications;
    }

    @Transactional
    public List<RefundResponse> list(String shopId, String actor) {
        guard.requireAccess(shopId, actor);
        return refunds.findByOrderShopIdOrderByCreatedDateDesc(shopId).stream().map(StaffRefundService::toResponse).toList();
    }

    @Transactional
    public RefundResponse updateStatus(String shopId, String actor, String refundId, String status) {
        guard.requireAccess(shopId, actor);
        Refund r = refunds.findById(refundId)
                .filter(x -> x.getOrder().getShop().getId().equals(shopId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy yêu cầu hoàn tiền."));
        RefundStatus next;
        try {
            next = RefundStatus.valueOf(status == null ? "" : status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Trạng thái không hợp lệ: " + status);
        }
        if (!NEXT.getOrDefault(r.getStatus(), Set.of()).contains(next))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Không thể chuyển yêu cầu hoàn tiền từ " + r.getStatus() + " sang " + next + ".");
        r.setStatus(next);
        r.setLastModifyBy(actor);
        if (next == RefundStatus.PROCESSED || next == RefundStatus.REJECTED) notifications.notifyRefundResolved(r);
        return toResponse(r);
    }

    private static RefundResponse toResponse(Refund r) {
        Order o = r.getOrder();
        return new RefundResponse(r.getId(), o.getId(), o.getCustomer().getFullName(), o.getCustomer().getPhone(),
                o.getRecipientName(), o.getRecipientPhone(), r.getAmount(), r.getReason(),
                r.getStatus().name(), r.getCreatedDate());
    }
}
