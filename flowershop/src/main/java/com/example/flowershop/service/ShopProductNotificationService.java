package com.example.flowershop.service;

import com.example.flowershop.entity.*;
import com.example.flowershop.entity.enums.*;
import com.example.flowershop.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.UUID;

/** Stores publication and recipient records in the same transaction as the product. */
@Service
public class ShopProductNotificationService {
    private final ShopProductPublicationRepository publications;
    private final ShopProductMailRepository mails;
    private final FavoriteShopRepository follows;
    public ShopProductNotificationService(ShopProductPublicationRepository publications,
            ShopProductMailRepository mails, FavoriteShopRepository follows) {
        this.publications = publications; this.mails = mails; this.follows = follows;
    }
    @Transactional
    public void published(Product product) {
        if (product.getStatus() != ProductStatus.ACTIVE || product.isAdminHidden()
                || product.getShop().getStatus() != ShopStatus.ACTIVE
                || publications.existsById(product.getId())) return;
        LocalDateTime now = LocalDateTime.now();
        var publication = new ShopProductPublication();
        publication.setProductId(product.getId()); publication.setPublishedAt(now);
        publications.save(publication);
        for (var follow : follows.findByIdShopId(product.getShop().getId())) {
            var user = follow.getUser();
            if (user.getRole() != UserRole.CUSTOMER || user.getStatus() != UserStatus.ACTIVE
                    || !Boolean.TRUE.equals(user.getIsEmailVerified())) continue;
            var mail = new ShopProductMail();
            mail.setId(UUID.randomUUID().toString()); mail.setProductId(product.getId());
            mail.setShopId(product.getShop().getId()); mail.setUserId(user.getId());
            mail.setCreatedAt(now); mail.setNextAttemptAt(now);
            mails.save(mail);
        }
    }
}
