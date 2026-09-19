package com.example.flowershop.entity;

import com.example.flowershop.entity.enums.InteractionType;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class BlogInteractionId implements Serializable {
    private String userId;
    private String blogId;
    @Enumerated(EnumType.STRING)
    @Column(name = "interaction_type")
    private InteractionType interactionType;
}
