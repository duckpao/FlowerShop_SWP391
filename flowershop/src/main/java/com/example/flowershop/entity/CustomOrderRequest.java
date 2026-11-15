package com.example.flowershop.entity;

import com.example.flowershop.entity.enums.CustomOrderStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "Custom_Order_Requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomOrderRequest {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", nullable = false)
    private Shop shop;

    @Column(name = "budget", nullable = false, precision = 12, scale = 2)
    private BigDecimal budget;

    @Column(name = "desired_components", columnDefinition = "json")
    private String desiredComponents;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @Builder.Default
    private CustomOrderStatus status = CustomOrderStatus.REQUESTED;

    /** Báo giá hoa của shop (chưa gồm phí giao hàng); null khi chưa báo giá. */
    @Column(name = "quoted_price", precision = 12, scale = 2)
    private BigDecimal quotedPrice;

    /** Phần trăm cọc shop yêu cầu (10 đến 100). */
    @Column(name = "deposit_percent")
    private Integer depositPercent;

    @Column(name = "quote_valid_until")
    private LocalDateTime quoteValidUntil;

    @Column(name = "quoted_date")
    private LocalDateTime quotedDate;

    /** Lý do từ chối (shop hoặc khách). */
    @Column(name = "reject_reason", length = 2000)
    private String rejectReason;

    /** SHOP hoặc CUSTOMER: bên đã từ chối yêu cầu. */
    @Column(name = "rejected_by", length = 20)
    private String rejectedBy;

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
