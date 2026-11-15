package com.example.flowershop.repository;
import com.example.flowershop.entity.SubscriptionReminder;
import org.springframework.stereotype.Repository;
import java.time.*;
import java.util.*;
@Repository public class SubscriptionReminderRepository extends HibernateRepository<SubscriptionReminder> {
    public SubscriptionReminderRepository() { super(SubscriptionReminder.class); }
    public boolean sentSlot(String id,LocalDateTime end,LocalDate day,int slot) {
        return countWhere("e.subscription.id = :id and e.expectedEnd = :end and e.reminderDate = :day and e.hourSlot = :slot",Map.of("id",id,"end",end,"day",day,"slot",slot))>0;
    }
    public Optional<SubscriptionReminder> lock(String id) { return locked("e.id = :id",Map.of("id",id)); }
    public List<String> mailDue(LocalDateTime now) {
        return session().createSelectionQuery("select e.id from SubscriptionReminder e where e.mailStatus = 'PENDING' and e.nextAttemptAt <= :now order by e.nextAttemptAt",String.class).setParameter("now",now).setMaxResults(50).getResultList();
    }
}
