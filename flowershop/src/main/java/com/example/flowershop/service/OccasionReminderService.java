package com.example.flowershop.service;

import com.example.flowershop.entity.CustomerOccasion;
import com.example.flowershop.entity.User;
import com.example.flowershop.repository.CustomerOccasionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static com.example.flowershop.service.HtmlMailSender.esc;

/** Quét các dịp đặc biệt đã đến lúc nhắc, gửi email và thông báo chuông cho khách. */
@Service
public class OccasionReminderService {

    private static final Logger log = LoggerFactory.getLogger(OccasionReminderService.class);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final CustomerOccasionRepository occasions;
    private final HtmlMailSender mail;
    private final NotificationService notifications;

    public OccasionReminderService(CustomerOccasionRepository occasions, HtmlMailSender mail, NotificationService notifications) {
        this.occasions = occasions;
        this.mail = mail;
        this.notifications = notifications;
    }

    /** @return số dịp đã được nhắc trong lần quét này. */
    @Transactional
    public int sendDueReminders(LocalDateTime now) {
        int sent = 0;
        List<CustomerOccasion> candidates = occasions.findReminderCandidates(now.toLocalDate());
        for (CustomerOccasion o : candidates) {
            if (!OccasionSchedule.isDue(o, now)) continue;
            try {
                if (remind(o, now.toLocalDate())) sent++;
            } catch (RuntimeException e) {
                log.warn("Occasion {} reminder failed: {}", o.getId(), e.toString());
            }
        }
        return sent;
    }

    private boolean remind(CustomerOccasion o, LocalDate today) {
        LocalDate occurrence = OccasionSchedule.nextOccurrence(o, today);
        User customer = o.getCustomer();
        long days = ChronoUnit.DAYS.between(today, occurrence);
        String when = days == 0 ? "hôm nay" : days == 1 ? "ngày mai" : "sau " + days + " ngày nữa (" + occurrence.format(DATE) + ")";

        // Chưa gửi được mail thì để lần quét sau thử lại, không đánh dấu đã nhắc
        boolean canMail = customer.getEmail() != null && !customer.getEmail().isBlank()
                && Boolean.TRUE.equals(customer.getIsEmailVerified());
        if (canMail && !mail.send(customer.getEmail(), "FlowerShop - Sắp đến " + o.getTitle(), html(o, customer, when))) return false;

        notifications.notifyUser(customer, "Sắp đến dịp: " + o.getTitle(),
                "Dịp \"" + o.getTitle() + "\" diễn ra " + when + ". Đặt hoa ngay để kịp tặng người thân!", "/shops");
        o.setLastRemindedFor(occurrence);
        if (!Boolean.TRUE.equals(o.getRecurringYearly())) o.setReminderSent(true);
        occasions.save(o);
        return true;
    }

    private String html(CustomerOccasion o, User customer, String when) {
        String body = "<p>Xin chào " + esc(customer.getFullName()) + ",</p>"
                + "<p>Dịp <strong>" + esc(o.getTitle()) + "</strong> của bạn diễn ra <strong>" + esc(when) + "</strong>.</p>"
                + "<p>Hãy chọn một bó hoa thật đẹp để gửi tặng người thân của bạn nhé.</p>"
                + "<p style=\"font-size:14px\"><a href=\"" + esc(mail.url("/#products")) + "\">Xem sản phẩm hoa</a> · "
                + "<a href=\"" + esc(mail.url("/occasions")) + "\">Quản lý dịp đặc biệt</a></p>";
        return HtmlMailSender.layout("Sắp đến dịp đặc biệt của bạn", body, "Chọn hoa ngay", mail.url("/shops"));
    }
}
