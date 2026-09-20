package com.example.flowershop.entity;
import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name="Shop_Staff",uniqueConstraints=@UniqueConstraint(columnNames={"shop_id","user_id"}))
@Getter @Setter @NoArgsConstructor
public class ShopStaff {
    @Id @Column(length=36) private String id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="shop_id",nullable=false) private Shop shop;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="user_id",nullable=false) private User user;
    @Column(nullable=false) private boolean active;
}
