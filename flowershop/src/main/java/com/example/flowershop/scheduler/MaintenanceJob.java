package com.example.flowershop.scheduler;

import com.example.flowershop.entity.Delivery;
import com.example.flowershop.entity.enums.CouponStatus;
import com.example.flowershop.entity.enums.DeliveryStatus;
import com.example.flowershop.repository.CartItemRepository;
import com.example.flowershop.repository.CouponRepository;
import com.example.flowershop.repository.DeliveryRepository;
import com.example.flowershop.service.DeliverySync;
import com.example.flowershop.service.GhnService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;

/** Ba job định kỳ theo SRS 5.3: hết hạn voucher, tự hoàn tất đơn đã giao, dọn giỏ hàng bỏ quên. */
@Component
public class MaintenanceJob {
    private static final Logger LOG = LoggerFactory.getLogger(MaintenanceJob.class);
    static final String ZONE = "Asia/Ho_Chi_Minh";
    static final String ACTOR = "system";

    private final CouponRepository coupons;
    private final DeliveryRepository deliveries;
    private final CartItemRepository carts;
    private final DeliverySync deliverySync;
    private final TransactionTemplate tx;
    private final int autoCompleteDays;
    private final int cartIdleDays;

    public MaintenanceJob(CouponRepository coupons, DeliveryRepository deliveries, CartItemRepository carts,
                          DeliverySync deliverySync, TransactionTemplate tx,
                          @Value("${app.scheduler.auto-complete-days:3}") int autoCompleteDays,
                          @Value("${app.scheduler.cart-idle-days:30}") int cartIdleDays) {
        this.coupons = coupons;
        this.deliveries = deliveries;
        this.carts = carts;
        this.deliverySync = deliverySync;
        this.tx = tx;
        this.autoCompleteDays = autoCompleteDays;
        this.cartIdleDays = cartIdleDays;
    }

    /** 00:00 hằng ngày: voucher ACTIVE đã quá ngày kết thúc chuyển sang EXPIRED. */
    @Scheduled(cron = "${app.scheduler.expire-coupons-cron:0 0 0 * * *}", zone = ZONE)
    public void expireCoupons() {
        try {
            int n = tx.execute(s -> {
                var ended = coupons.activeEndedBefore(LocalDateTime.now());
                ended.forEach(c -> { c.setStatus(CouponStatus.EXPIRED); c.setLastModifyBy(ACTOR); });
                return ended.size();
            });
            LOG.info("Đã chuyển {} voucher sang EXPIRED.", n);
        } catch (Exception e) {
            LOG.error("Lỗi khi hết hạn voucher: {}", e.getMessage(), e);
        }
    }

    /**
     * 02:00 hằng ngày: đơn DELIVERING mà vận đơn đã delivered quá N ngày thì COMPLETED.
     * Đi qua DeliverySync nên COD được đánh dấu SUCCESS và listener phát sự kiện gửi mail như khi GHN báo giao xong.
     * Mỗi đơn một transaction để một đơn lỗi không chặn các đơn còn lại.
     */
    @Scheduled(cron = "${app.scheduler.auto-complete-cron:0 0 2 * * *}", zone = ZONE)
    public void autoCompleteDeliveredOrders() {
        int done = 0;
        try {
            var due = deliveries.deliveredBeforeWithoutRefund(LocalDateTime.now().minusDays(autoCompleteDays));
            for (Delivery d : due) {
                try {
                    tx.executeWithoutResult(s -> deliverySync.apply(d.getOrder(), d,
                            new GhnService.Tracking("delivered", DeliveryStatus.DELIVERED), ACTOR));
                    done++;
                } catch (Exception e) {
                    LOG.error("Lỗi khi tự hoàn tất đơn của vận đơn {}: {}", d.getId(), e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            LOG.error("Lỗi khi quét đơn đã giao: {}", e.getMessage(), e);
        }
        LOG.info("Đã tự hoàn tất {} đơn đã giao.", done);
    }

    /** 03:00 Chủ nhật hằng tuần: xóa dòng giỏ không đổi quá 30 ngày. */
    @Scheduled(cron = "${app.scheduler.cart-cleanup-cron:0 0 3 * * SUN}", zone = ZONE)
    public void cleanUpAbandonedCartItems() {
        try {
            Integer n = tx.execute(s -> carts.deleteNotModifiedSince(LocalDateTime.now().minusDays(cartIdleDays)));
            LOG.info("Đã xóa {} dòng giỏ hàng bỏ quên.", n);
        } catch (Exception e) {
            LOG.error("Lỗi khi dọn giỏ hàng: {}", e.getMessage(), e);
        }
    }
}
