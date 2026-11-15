package com.example.flowershop.repository;
import com.example.flowershop.entity.ShopPlanPayment;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.*;
@Repository public class ShopPlanPaymentRepository extends HibernateRepository<ShopPlanPayment> {
    public ShopPlanPaymentRepository() { super(ShopPlanPayment.class); }
    public void refresh(ShopPlanPayment payment) { session().refresh(payment); }
    public Optional<ShopPlanPayment> invoice(String invoice) { return one("e.invoiceNumber = :invoice",Map.of("invoice",invoice)); }
    public List<ShopPlanPayment> pendingForShop(String shopId) {
        return selection("e.shop.id = :shopId and e.status in ('PENDING','REVIEW')",Map.of("shopId",shopId),org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Order.desc("createdAt"))).setMaxResults(50).getResultList();
    }
    public List<String> pendingInvoices(LocalDateTime now) {
        return session().createSelectionQuery("select e.invoiceNumber from ShopPlanPayment e where e.status = 'PENDING' and e.createdAt >= :since and (e.lastCheckedAt is null or e.lastCheckedAt < :retry) order by e.lastCheckedAt asc nulls first, e.createdAt",String.class)
            .setParameter("since",now.minusDays(30)).setParameter("retry",now.minusMinutes(2)).setMaxResults(25).getResultList();
    }
}
