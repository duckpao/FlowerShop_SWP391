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

    @Scheduled(fixedDelayString = "${app.occasion-reminder.interval-ms:900000}",
            initialDelayString = "${app.occasion-reminder.initial-delay-ms:60000}")
    public void run() {
        try {
            int sent = reminders.sendDueReminders(LocalDateTime.now(OccasionService.ZONE));
            if (sent > 0) log.info("Sent {} occasion reminder(s)", sent);
        } catch (RuntimeException e) {
            log.warn("Occasion reminder run failed: {}", e.toString());
        }
    }
}
