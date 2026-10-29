package com.example.flowershop.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "Customer_Occasions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerOccasion {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "occasion_date", nullable = false)
    private LocalDate occasionDate;

    @Builder.Default
    @Column(name = "reminder_sent")
    private Boolean reminderSent = false;

    /** Dịp lặp lại mỗi năm (sinh nhật, kỷ niệm) hay chỉ một lần. */
    @Builder.Default
    @Column(name = "recurring_yearly", nullable = false)
    private Boolean recurringYearly = false;

    /** Nhắc trước ngày dịp bao nhiêu ngày (mặc định 7) và vào giờ nào (mặc định 08:00). */
    @Builder.Default
    @Column(name = "notify_days_before", nullable = false)
    private Integer notifyDaysBefore = 7;

    @Builder.Default
    @Column(name = "notify_time", nullable = false)
    private LocalTime notifyTime = LocalTime.of(8, 0);

    /** Ngày của lần diễn ra dịp đã được nhắc gần nhất, để mỗi lần diễn ra chỉ nhắc một lần. */
    @Column(name = "last_reminded_for")
    private LocalDate lastRemindedFor;

    @CreationTimestamp
    @Column(name = "created_date", updatable = false)
    private LocalDateTime createdDate;

    @Column(name = "created_by", length = 36)
    private String createdBy;

    @UpdateTimestamp
    @Column(name = "last_modify_date")
    private LocalDateTime lastModifyDate;

    @Column(name = "last_modify_by", length = 36)
    private String lastModifyBy;
}
