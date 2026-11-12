package com.example.flowershop.scheduler;
import com.example.flowershop.service.*;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
@Component
@ConditionalOnProperty(name="app.shop-subscription.enabled",havingValue="true",matchIfMissing=true)
public class ShopSubscriptionJob {
    private static final Logger LOG=LoggerFactory.getLogger(ShopSubscriptionJob.class);
    private final SubscriptionReminderService reminders;
    private final ShopPlanService plans;
    public ShopSubscriptionJob(SubscriptionReminderService reminders,ShopPlanService plans) {this.reminders=reminders;this.plans=plans;}
    @Scheduled(cron="${app.shop-subscription.reminder-cron:0 0 9,19 * * *}",zone="Asia/Ho_Chi_Minh")
    public void remind() {
        for(var id:reminders.dueSubscriptions())try {reminders.queue(id);} catch(RuntimeException e){LOG.warn("Cannot queue subscription reminder {}: {}",id,e.getClass().getSimpleName());}
        sendMail();
    }
    @Scheduled(fixedDelayString="${app.shop-subscription.retry-ms:60000}",initialDelayString="${app.shop-subscription.initial-delay-ms:60000}")
    public void background() {
        for(var invoice:plans.pendingInvoices())try {plans.reconcile(invoice);} catch(RuntimeException e){LOG.warn("Cannot reconcile subscription {}: {}",invoice,e.getClass().getSimpleName());}
        // Recover today's latest due slot after a restart; the persisted slot key prevents duplicates.
        remind();
    }
    private void sendMail() {
        for(var id:reminders.dueMail())try {reminders.deliver(id);} catch(RuntimeException e){LOG.warn("Cannot deliver subscription reminder {}: {}",id,e.getClass().getSimpleName());}
    }
}
