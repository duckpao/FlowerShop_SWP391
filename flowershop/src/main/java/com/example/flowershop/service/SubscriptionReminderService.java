package com.example.flowershop.service;
import com.example.flowershop.entity.*;
import com.example.flowershop.entity.enums.*;
import com.example.flowershop.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import static com.example.flowershop.service.HtmlMailSender.esc;

@Service @Transactional(readOnly=true)
public class SubscriptionReminderService {
    private final ShopSubscriptionRepository subscriptions;
    private final SubscriptionReminderRepository reminders;
    private final ShopRepository shops;
    private final NotificationService notifications;
    private final HtmlMailSender mail;
    private final ShopPlanClock clock;
    @jakarta.persistence.PersistenceContext jakarta.persistence.EntityManager entityManager;
    public SubscriptionReminderService(ShopSubscriptionRepository subscriptions,SubscriptionReminderRepository reminders,
            ShopRepository shops,NotificationService notifications,HtmlMailSender mail,ShopPlanClock clock) {
        this.subscriptions=subscriptions;this.reminders=reminders;this.shops=shops;this.notifications=notifications;this.mail=mail;this.clock=clock;
    }
    public List<String> dueSubscriptions() { return subscriptions.dueIds(clock.now()); }
    public List<String> dueMail() { return reminders.mailDue(clock.now()); }
    @Transactional
    public void queue(String id) {
        var s=subscriptions.findById(id).orElse(null); if(s==null)return;
        shops.findForUpdate(s.getShop().getId()).orElseThrow(); entityManager.refresh(s);
        var now=clock.now();
        if(!s.getEndsAt().isAfter(now) || s.getShop().getStatus()!=ShopStatus.ACTIVE) return;
        long days=ChronoUnit.DAYS.between(now.toLocalDate(),s.getEndsAt().minusNanos(1).toLocalDate());
        if(days<0 || days>2) return;
        int slot=now.getHour()>=19?19:now.getHour()>=9?9:-1;
        if(slot<0 || reminders.sentSlot(id,s.getEndsAt(),now.toLocalDate(),slot)) return;
        var owner=s.getShop().getOwner();
        if(owner.getStatus()!=UserStatus.ACTIVE)return;
        var reminder=SubscriptionReminder.builder().id(UUID.randomUUID().toString()).subscription(s).expectedEnd(s.getEndsAt())
                .reminderDate(now.toLocalDate()).hourSlot(slot).mailStatus("PENDING").attempts(0).nextAttemptAt(now).build();
        reminders.save(reminder);
        notifications.notifyUser(owner,"Gói Pro sắp hết hạn",message(s),"/shop-admin/subscription");
    }
    private String message(ShopSubscription s) {
        return "Gói Pro của "+s.getShop().getName()+" hết hạn lúc "+s.getEndsAt().format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy"))
                +". Gia hạn để tiếp tục đăng không giới hạn và dùng 10 vị trí sản phẩm nổi bật.";
    }
    @Transactional
    public void deliver(String id) {
        var r=reminders.lock(id).orElse(null); if(r==null || !"PENDING".equals(r.getMailStatus()))return;
        var s=r.getSubscription();
        shops.findForUpdate(s.getShop().getId()).orElseThrow(); entityManager.refresh(s);
        var owner=s.getShop().getOwner(); var now=clock.now();
        if(!r.getExpectedEnd().equals(s.getEndsAt()) || !s.getEndsAt().isAfter(now)
                || s.getShop().getStatus()!=ShopStatus.ACTIVE || owner.getStatus()!=UserStatus.ACTIVE) {
            r.setMailStatus("CANCELLED");return;
        }
        if(r.getNextAttemptAt().isAfter(now))return;
        r.setAttempts(r.getAttempts()+1);
        var html=HtmlMailSender.layout("Gói Pro sắp hết hạn","<p>Xin chào "+esc(owner.getFullName())+",</p><p>"+esc(message(s))+"</p>",
                "Gia hạn Pro",mail.url("/shop-admin/subscription"));
        if(mail.send(owner.getEmail(),"FlowerShop - Gói Pro sắp hết hạn",html))r.setMailStatus("SENT");
        else r.setNextAttemptAt(now.plusMinutes(Math.min(60,5L*r.getAttempts())));
    }
}
