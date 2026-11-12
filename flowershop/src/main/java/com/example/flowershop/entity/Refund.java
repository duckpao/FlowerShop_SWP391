package com.example.flowershop.entity;

import com.example.flowershop.entity.enums.RefundStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "Refunds")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Refund {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    private String reason;

    /** Mô tả thêm của khách (bắt buộc khi lý do là "Khác"). */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /** Ảnh bằng chứng của khách, mảng URL dạng JSON. */
    @Column(name = "evidence_images", columnDefinition = "TEXT")
    private String evidenceImages;

    /** Phản hồi của shop cho khách, hiển thị khi yêu cầu bị từ chối. */
    @Column(name = "shop_response", columnDefinition = "TEXT")
    private String shopResponse;

    @Column(name = "gateway_refund_id", length = 100)
    private String gatewayRefundId;

    @Column(name = "gateway_response", columnDefinition = "json")
    private String gatewayResponse;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @Builder.Default
    private RefundStatus status = RefundStatus.REQUESTED;

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
