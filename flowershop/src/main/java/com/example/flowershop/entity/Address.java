package com.example.flowershop.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "Addresses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Address {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id")
    private Shop shop;

    @Column(name = "address_line", nullable = false, length = 255)
    private String addressLine;

    @Column(name = "ward", length = 100)
    private String ward;

    @Column(name = "district", length = 100)
    private String district;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "ghn_ward_code", length = 20)
    private String ghnWardCode;

    @Column(name = "ghn_district_id")
    private Integer ghnDistrictId;

    @Builder.Default
    @Column(name = "is_default")
    private Boolean isDefault = false;

    @Column(name = "recipient_name", length = 100) private String recipientName;
    @Column(name = "recipient_phone", length = 10) private String recipientPhone;
    @Builder.Default @Column(name = "is_pickup") private Boolean isPickup = false;
    @Builder.Default @Column(name = "is_return") private Boolean isReturn = false;
    @Builder.Default @Column(name = "address_type", length = 10) private String addressType = "HOME";

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
