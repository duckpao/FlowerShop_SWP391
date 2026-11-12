package com.example.flowershop.scheduler;

import com.example.flowershop.entity.enums.CouponStatus;
import com.example.flowershop.repository.CartItemRepository;
import com.example.flowershop.repository.CouponRepository;
import com.example.flowershop.repository.OrderRepository;
import com.example.flowershop.service.DeliverySync;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** Ba job định kỳ theo SRS 5.3: hết hạn mã giảm giá, tự hoàn tất đơn đã giao, dọn giỏ hàng bỏ quên. */
@Component
public class MaintenanceJob {
    private static final Logger LOG = LoggerFactory.getLogger(MaintenanceJob.class);
    private static final String ZONE = "Asia/Ho_Chi_Minh";
    static final String ACTOR = "system";
    static final int AUTO_COMPLETE_DAYS = 3;
    static final int CART_IDLE_DAYS = 30;

    private final CouponRepository coupons;
    private final OrderRepository orders;
    private final CartItemRepository carts;
    private final DeliverySync deliverySync;

    public MaintenanceJob(CouponRepository coupons, OrderRepository orders, CartItemRepository carts, DeliverySync deliverySync) {
        this.coupons = coupons;
        this.orders = orders;
        this.carts = carts;
        this.deliverySync = deliverySync;
    }

    /** 00:00 hằng ngày: mã ACTIVE đã quá ngày kết thúc chuyển sang EXPIRED. */
    @Scheduled(cron = "0 0 0 * * *", zone = ZONE)
    @Transactional
    public void expireCoupons() {
        var ended = coupons.activeEndedBefore(LocalDateTime.now());
        ended.forEach(c -> { c.setStatus(CouponStatus.EXPIRED); c.setLastModifyBy(ACTOR); });
        LOG.info("Đã chuyển {} mã giảm giá sang EXPIRED.", ended.size());
    }

    /**
     * 02:00 hằng ngày: đơn DELIVERING mà vận đơn đã giao từ 3 ngày trước và không có hoàn tiền thì COMPLETED.
     * Hiện DeliverySync đã hoàn tất đơn ngay khi GHN báo delivered, nên job này chỉ vớt đơn bị sót.
     */
    @Scheduled(cron = "0 0 2 * * *", zone = ZONE)
    @Transactional
    public void autoCompleteDeliveredOrders() {
        var due = orders.deliveredBeforeWithoutRefund(LocalDateTime.now().minusDays(AUTO_COMPLETE_DAYS));
        due.forEach(o -> deliverySync.complete(o, ACTOR));
        LOG.info("Đã tự hoàn tất {} đơn đã giao.", due.size());
    }

    /** 03:00 Chủ nhật hằng tuần: xóa sản phẩm trong giỏ không đổi quá 30 ngày. */
    @Scheduled(cron = "0 0 3 * * SUN", zone = ZONE)
    @Transactional
    public void cleanUpAbandonedCartItems() {
        LOG.info("Đã xóa {} sản phẩm bỏ quên trong giỏ hàng.", carts.deleteNotModifiedSince(LocalDateTime.now().minusDays(CART_IDLE_DAYS)));
    }
}
