package com.example.flowershop.scheduler;
import com.example.flowershop.repository.ShopProductMailRepository;
import com.example.flowershop.service.ShopProductMailDelivery;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDateTime;
@Component
@ConditionalOnProperty(name="app.shop-follow-mail.enabled", havingValue="true", matchIfMissing=true)
public class ShopProductMailJob {
    private static final Logger LOG=LoggerFactory.getLogger(ShopProductMailJob.class);
    private final ShopProductMailRepository mails;
    private final ShopProductMailDelivery delivery;
    public ShopProductMailJob(ShopProductMailRepository mails, ShopProductMailDelivery delivery) {
        this.mails=mails; this.delivery=delivery;
    }
    /**
     * Cronjob chạy định kỳ mỗi 30 giây (30.000 ms, khởi động sau 30s) để xử lý hàng đợi mail thông báo sản phẩm mới.
     * 
     * Luồng thực thi:
     * 1. Lấy danh sách tối đa 20 bản ghi mail đang đến hạn gửi (mails.due) theo cơ chế batch.
     * 2. Duyệt qua từng bản ghi và gọi ShopProductMailDelivery.deliver(id) để:
     *    - Kiểm tra tính hợp lệ của sản phẩm, shop và người theo dõi (Follower).
     *    - Soạn thảo và gửi email thông báo mẫu hoa mới tới hòm thư người theo dõi.
     *    - Cập nhật trạng thái mail thành SENT hoặc tăng số lần thử lại (retry) nếu lỗi.
     */
    @Scheduled(fixedDelayString="${app.shop-follow-mail.interval-ms:30000}", initialDelayString="${app.shop-follow-mail.initial-delay-ms:30000}")
    public void sendPending() {
        // Quét lấy danh sách tối đa 20 email đến hạn trong hàng đợi
        for (var id : mails.due(LocalDateTime.now(), PageRequest.of(0,20))) {
            try {
                // Tiến hành gửi email thông báo sản phẩm mới tới khách hàng theo dõi shop
                delivery.deliver(id);
            } catch (RuntimeException ex) {
                LOG.warn("Notification {} could not be processed ({})", id, ex.getClass().getSimpleName());
            }
        }
    }
}
