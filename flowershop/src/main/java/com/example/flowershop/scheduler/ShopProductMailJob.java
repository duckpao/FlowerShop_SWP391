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
    @Scheduled(fixedDelayString="${app.shop-follow-mail.interval-ms:30000}", initialDelayString="${app.shop-follow-mail.initial-delay-ms:30000}")
    public void sendPending() {
        for (var id : mails.due(LocalDateTime.now(), PageRequest.of(0,20))) {
            try { delivery.deliver(id); }
            catch (RuntimeException ex) { LOG.warn("Notification {} could not be processed ({})",id,ex.getClass().getSimpleName()); }
        }
    }
}
