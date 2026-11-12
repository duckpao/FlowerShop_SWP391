package com.example.flowershop.entity;

import jakarta.persistence.*;
import lombok.*;

/** Database-managed choices; disabling a choice never changes an existing order. */
@Entity
@Table(name = "Custom_Flower_Options", uniqueConstraints = @UniqueConstraint(columnNames = {"option_group", "label"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CustomFlowerOption {
    public enum Group { FLOWER, OCCASION, WRAPPING, ACCESSORY }

    @Id @Column(length = 36)
    private String id;
    @Enumerated(EnumType.STRING)
    @Column(name = "option_group", nullable = false, length = 20)
    private Group optionGroup;
    @Column(nullable = false, length = 100)
    private String label;
    @Column(nullable = false)
    private boolean disabled;
    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
}
