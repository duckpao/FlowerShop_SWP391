package com.example.flowershop.scheduler;

import com.example.flowershop.service.OccasionReminderService;
import com.example.flowershop.service.OccasionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/** Mỗi 15 phút quét các dịp đặc biệt đến hạn nhắc và gửi email + thông báo cho khách. */
@Component
@ConditionalOnProperty(name = "app.occasion-reminder.enabled", havingValue = "true", matchIfMissing = true)
public class OccasionReminderJob {

    private static final Logger log = LoggerFactory.getLogger(OccasionReminderJob.class);
    private final OccasionReminderService reminders;

    public OccasionReminderJob(OccasionReminderService reminders) {
        this.reminders = reminders;
    }

    /**
     * Cronjob chạy định kỳ (mặc định mỗi 15 phút - 900.000 ms, khởi động sau 1 phút - 60.000 ms).
     * 
     * Luồng thực thi:
     * 1. Gọi OccasionReminderService.sendDueReminders() kèm mốc thời gian hiện tại theo múi giờ hệ thống.
     * 2. Quét các dịp kỷ niệm/sự kiện đặc biệt của khách hàng đến hạn nhắc nhở.
     * 3. Gửi email thông báo nhắc chọn hoa kèm link sản phẩm.
     * 4. Tạo thông báo in-app (chuông thông báo) gửi đến tài khoản khách hàng.
     */
    @Scheduled(fixedDelayString = "${app.occasion-reminder.interval-ms:900000}",
            initialDelayString = "${app.occasion-reminder.initial-delay-ms:60000}")
    public void run() {
        try {
            // Quét và gửi thông báo nhắc dịp đặc biệt cho các khách hàng đến hạn
            int sent = reminders.sendDueReminders(LocalDateTime.now(OccasionService.ZONE));
            if (sent > 0) log.info("Sent {} occasion reminder(s)", sent);
        } catch (RuntimeException e) {
            log.warn("Occasion reminder run failed: {}", e.toString());
        }
    }
}
