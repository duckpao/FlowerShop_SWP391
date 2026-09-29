package com.example.flowershop;

import com.example.flowershop.entity.*;
import com.example.flowershop.entity.enums.*;
import com.example.flowershop.repository.*;
import com.example.flowershop.service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class ProductReviewTests extends ManagerShopTests {
    @Autowired ProductRepository products;
    @Autowired CategoryRepository categories;
    @Autowired ProductReviewRepository reviews;
    @Autowired ProductReviewService service;
    Category category; User customer, stranger; Product item;

    @Override @BeforeEach void setup() {
        reviews.deleteAll(); products.deleteAll(); categories.deleteAll(); super.setup();
        category = categories.saveAndFlush(Category.builder().id(UUID.randomUUID().toString())
                .name("Hoa cưới").status(CategoryStatus.ACTIVE).build());
        customer = user(UserRole.CUSTOMER); stranger = user(UserRole.CUSTOMER);
        item = products.saveAndFlush(Product.builder().id(UUID.randomUUID().toString()).shop(shop)
                .category(category).name("Bó hồng").price(new BigDecimal("100000")).stock(5)
                .status(ProductStatus.ACTIVE).build());
    }

    @AfterEach void clean() { reviews.deleteAll(); products.deleteAll(); categories.deleteAll(); }

    ProductReviewService.ReviewInput input(int rating) {
        return new ProductReviewService.ReviewInput(rating, "Hoa tươi");
    }

    @Test void oneReviewPerUserPerProduct() {
        service.write(item.getId(), customer.getId(), input(5));
        assertThatThrownBy(() -> service.write(item.getId(), customer.getId(), input(4)))
                .isInstanceOf(ResponseStatusException.class);
        service.update(item.getId(), customer.getId(), input(3));
        assertThat(service.list(item.getId(), 0).content()).singleElement()
                .extracting(ProductReviewService.ReviewResult::rating).isEqualTo(3);
    }

    @Test void reviewNeverExposesEmailAndFallsBackWhenNameEmpty() {
        customer.setFullName(null); users.saveAndFlush(customer);
        service.write(item.getId(), customer.getId(), input(5));
        var result = service.list(item.getId(), 0).content().get(0);
        assertThat(result.reviewerName()).isEqualTo("Khách hàng");
        assertThat(result.toString()).doesNotContain(customer.getEmail());
    }

    @Test void cannotEditOrDeleteSomeoneElsesReview() {
        service.write(item.getId(), customer.getId(), input(5));
        assertThatThrownBy(() -> service.update(item.getId(), stranger.getId(), input(1)))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.delete(item.getId(), stranger.getId()))
                .isInstanceOf(ResponseStatusException.class);
        service.delete(item.getId(), customer.getId());
        assertThat(service.list(item.getId(), 0).content()).isEmpty();
    }

    @Test void cannotReviewHiddenProduct() {
        item.setAdminHidden(true); products.saveAndFlush(item);
        assertThatThrownBy(() -> service.write(item.getId(), customer.getId(), input(5)))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test void publicReviewListRefusesProductsThatAreNotPubliclyVisible() {
        service.write(item.getId(), customer.getId(), input(5));
        assertThat(service.list(item.getId(), 0).totalElements()).isEqualTo(1);

        item.setAdminHidden(true); products.saveAndFlush(item);
        assertThatThrownBy(() -> service.list(item.getId(), 0)).isInstanceOf(ResponseStatusException.class);

        item.setAdminHidden(false); products.saveAndFlush(item);
        shop.setStatus(ShopStatus.BANNED); shops.saveAndFlush(shop);
        assertThatThrownBy(() -> service.list(item.getId(), 0)).isInstanceOf(ResponseStatusException.class);
    }

    @Test void publicReviewListOfUnknownProductIsNotAnExistenceOracle() {
        assertThatThrownBy(() -> service.list("khong-ton-tai", 0)).isInstanceOf(ResponseStatusException.class);
    }

    @Test void databaseRejectsDuplicateReviewWhenTheServiceCheckIsBypassed() {
        service.write(item.getId(), customer.getId(), input(5));
        var duplicate = ProductReview.builder().id(UUID.randomUUID().toString())
                .product(item).user(customer).rating(3).build();
        assertThatThrownBy(() -> reviews.saveAndFlush(duplicate))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    @Test void customerCanReadBackOwnReviewToEditIt() {
        assertThatThrownBy(() -> service.mine(item.getId(), customer.getId()))
                .isInstanceOf(ResponseStatusException.class);
        service.write(item.getId(), customer.getId(), input(4));
        assertThat(service.mine(item.getId(), customer.getId()).rating()).isEqualTo(4);
        assertThatThrownBy(() -> service.mine(item.getId(), stranger.getId()))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test void onlyOwningManagerCanReply() {
        var review = service.write(item.getId(), customer.getId(), input(5));
        assertThatThrownBy(() -> service.reply(shop.getId(), review.id(), other.getId(), "Cảm ơn"))
                .isInstanceOf(ResponseStatusException.class);
        assertThat(service.reply(shop.getId(), review.id(), owner.getId(), "Cảm ơn").shopReply())
                .isEqualTo("Cảm ơn");
    }
}
