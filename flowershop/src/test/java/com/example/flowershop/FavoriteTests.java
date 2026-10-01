package com.example.flowershop;

import com.example.flowershop.entity.*;
import com.example.flowershop.entity.enums.*;
import com.example.flowershop.repository.*;
import com.example.flowershop.service.*;
import com.example.flowershop.dto.auth.LoginRequest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class FavoriteTests extends ManagerShopTests {
    @Autowired ProductRepository products;
    @Autowired CategoryRepository categories;
    @Autowired FavoriteProductRepository favorites;
    @Autowired FavoriteService service;
    Category category; User customer; Product item;

    @Override @BeforeEach void setup() {
        favorites.deleteAll(); products.deleteAll(); categories.deleteAll(); super.setup();
        category = categories.saveAndFlush(Category.builder().id(UUID.randomUUID().toString())
                .name("Hoa cưới").status(CategoryStatus.ACTIVE).build());
        customer = user(UserRole.CUSTOMER);
        item = products.saveAndFlush(Product.builder().id(UUID.randomUUID().toString()).shop(shop)
                .category(category).name("Bó hồng").price(new BigDecimal("100000")).stock(5)
                .status(ProductStatus.ACTIVE).build());
    }

    @AfterEach void clean() { favorites.deleteAll(); products.deleteAll(); categories.deleteAll(); }

    @Test void addAndRemoveAreIdempotent() {
        service.add(customer.getId(), item.getId());
        service.add(customer.getId(), item.getId());
        assertThat(service.mine(customer.getId(), 0).totalElements()).isEqualTo(1);
        service.remove(customer.getId(), item.getId());
        service.remove(customer.getId(), item.getId());
        assertThat(service.mine(customer.getId(), 0).totalElements()).isZero();
    }

    @Test void hiddenProductCannotBeAddedButStaysListedWhenHiddenLater() {
        service.add(customer.getId(), item.getId());
        item.setAdminHidden(true); products.saveAndFlush(item);
        assertThat(service.mine(customer.getId(), 0).content()).singleElement()
                .extracting(ProductCardAssembler.ProductCard::available).isEqualTo(false);
        var second = products.saveAndFlush(Product.builder().id(UUID.randomUUID().toString()).shop(shop)
                .category(category).name("Ẩn sẵn").price(new BigDecimal("1000")).stock(1)
                .status(ProductStatus.INACTIVE).build());
        assertThatThrownBy(() -> service.add(customer.getId(), second.getId()))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    }

    @Test void favoritesAreCustomerOnly() throws Exception {
        String managerToken = auth.login(new LoginRequest(owner.getEmail(), "FlowerShop123")).accessToken();
        mvc.perform(get("/api/customer/favorites").header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/customer/favorites")).andExpect(status().isUnauthorized());
        String customerToken = auth.login(new LoginRequest(customer.getEmail(), "FlowerShop123")).accessToken();
        mvc.perform(get("/api/customer/favorites").header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk());
    }
}
