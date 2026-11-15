package com.example.flowershop.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.*;
@Entity @Table(name="Subscription_Reminders",uniqueConstraints=@UniqueConstraint(name="uq_subscription_reminder",columnNames={"subscription_id","expected_end","reminder_date","hour_slot"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SubscriptionReminder {
    @Id @Column(length=36) private String id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="subscription_id",nullable=false) private ShopSubscription subscription;
    @Column(name="expected_end",nullable=false) private LocalDateTime expectedEnd;
    @Column(name="reminder_date",nullable=false) private LocalDate reminderDate;
    @Column(name="hour_slot",nullable=false) private int hourSlot;
    @Column(name="mail_status",nullable=false,length=20) private String mailStatus;
    @Column(nullable=false) private int attempts;
    @Column(name="next_attempt_at",nullable=false) private LocalDateTime nextAttemptAt;
}
