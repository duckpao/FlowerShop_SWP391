package com.example.flowershop.service;

import com.example.flowershop.entity.FavoriteShopId;
import com.example.flowershop.entity.enums.*;
import com.example.flowershop.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDateTime;
import java.text.NumberFormat;
import java.util.Locale;

@Service
public class ShopProductMailDelivery {
    private static final Logger LOG = LoggerFactory.getLogger(ShopProductMailDelivery.class);
    private final ShopProductMailRepository mails;
    private final FavoriteShopRepository follows;
    private final ProductRepository products;
    private final UserRepository users;
    private final JavaMailSender sender;
    private final String from, baseUrl;
    public ShopProductMailDelivery(ShopProductMailRepository mails, FavoriteShopRepository follows,
            ProductRepository products, UserRepository users, JavaMailSender sender,
            @Value("${app.mail.from}") String from,
            @Value("${app.frontend-base-url:http://localhost:5173}") String baseUrl) {
        this.mails=mails; this.follows=follows; this.products=products; this.users=users;
        this.sender=sender; this.from=from; this.baseUrl=baseUrl.replaceAll("/+$", "");
    }
    @Transactional
    public void deliver(String id) {
        var mail = mails.findLocked(id).orElse(null);
        if (mail == null || !"PENDING".equals(mail.getStatus())
                || mail.getNextAttemptAt().isAfter(LocalDateTime.now())) return;
        var product = products.findById(mail.getProductId()).orElse(null);
        var user = users.findById(mail.getUserId()).orElse(null);
        if (product == null || user == null || product.getStatus() != ProductStatus.ACTIVE
                || product.isAdminHidden() || product.getShop().getStatus() != ShopStatus.ACTIVE
                || user.getRole() != UserRole.CUSTOMER || user.getStatus() != UserStatus.ACTIVE
                || !Boolean.TRUE.equals(user.getIsEmailVerified())
                || !follows.existsById(new FavoriteShopId(mail.getUserId(), mail.getShopId()))) {
            mail.setStatus("CANCELLED"); return;
        }
        var message = new SimpleMailMessage();
        message.setFrom(from); message.setTo(user.getEmail());
        message.setSubject("FlowerShop - " + product.getShop().getName().replaceAll("[\\r\\n]", " ") + " có sản phẩm mới");
        message.setText("Xin chào " + (user.getFullName() == null ? "bạn" : user.getFullName())
                + ",\n\nCửa hàng bạn đang theo dõi vừa đăng sản phẩm mới."
                + "\nCửa hàng: " + product.getShop().getName()
                + "\nSản phẩm: " + product.getName()
                + "\nGiá: " + NumberFormat.getCurrencyInstance(Locale.forLanguageTag("vi-VN")).format(product.getPrice())
                + "\nMô tả: " + product.getDescription()
                + "\n\nXem sản phẩm: " + baseUrl + "/products/" + product.getId()
                + "\nXem cửa hàng: " + baseUrl + "/shops/" + mail.getShopId()
                + "\n\nBạn nhận thư vì đang theo dõi cửa hàng này. Bỏ theo dõi tại: " + baseUrl + "/following-shops");
        mail.setAttempts(mail.getAttempts() + 1);
        try {
            sender.send(message); mail.setStatus("SENT"); mail.setSentAt(LocalDateTime.now());
        } catch (MailException ex) {
            if (mail.getAttempts() >= 5) mail.setStatus("FAILED");
            else mail.setNextAttemptAt(LocalDateTime.now().plusMinutes(1L << mail.getAttempts()));
            LOG.warn("Product notification {} failed on attempt {} ({})", id, mail.getAttempts(), ex.getClass().getSimpleName());
        }
    }
}
