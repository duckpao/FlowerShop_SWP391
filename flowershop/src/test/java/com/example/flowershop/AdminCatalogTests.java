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

class AdminCatalogTests extends ManagerShopTests {
    @Autowired ProductRepository products;
    @Autowired CategoryRepository categories;
    @Autowired ProductImageRepository productImages;
    @Autowired AdminCatalogService service;
    @Autowired CatalogService catalog;
    @Autowired ManagerProductService managerProducts;
    Category category; User admin; Product item;

    @Override @BeforeEach void setup() {
        productImages.deleteAll(); products.deleteAll(); categories.deleteAll(); super.setup();
        category = categories.saveAndFlush(Category.builder().id(UUID.randomUUID().toString())
                .name("Hoa cưới").status(CategoryStatus.ACTIVE).build());
        admin = user(UserRole.ADMIN);
        item = products.saveAndFlush(Product.builder().id(UUID.randomUUID().toString()).shop(shop)
                .category(category).name("Bó hồng").price(new BigDecimal("100000")).stock(5)
                .status(ProductStatus.ACTIVE).build());
    }

    @AfterEach void clean() { productImages.deleteAll(); products.deleteAll(); categories.deleteAll(); }

    @Test void adminCreatesAndDeactivatesCategory() {
        var created = service.createCategory(admin.getId(),
                new AdminCatalogService.CategoryInput("Hoa sinh nhật", "Mô tả", CategoryStatus.ACTIVE));
        assertThat(catalog.categories()).extracting(CatalogService.CategoryCard::name)
                .contains("Hoa sinh nhật");
        service.updateCategory(created.id(), admin.getId(),
                new AdminCatalogService.CategoryInput("Hoa sinh nhật", "Mô tả", CategoryStatus.INACTIVE));
        assertThat(catalog.categories()).extracting(CatalogService.CategoryCard::name)
                .doesNotContain("Hoa sinh nhật");
    }

    @Test void adminHiddenRemovesProductFromPublicAndManagerCannotUndoIt() {
        service.setHidden(item.getId(), true, admin.getId());
        assertThat(catalog.browse("", null, null, "newest", 0, 12).content()).isEmpty();
        managerProducts.save(shop.getId(), item.getId(), owner.getId(),
                new ManagerProductService.Input("Bó hồng", "Mô tả", category.getId(),
                        new BigDecimal("100000"), 5, ProductStatus.ACTIVE, null, null));
        assertThat(products.findById(item.getId()).orElseThrow().isAdminHidden()).isTrue();
        assertThat(catalog.browse("", null, null, "newest", 0, 12).content()).isEmpty();
        service.setHidden(item.getId(), false, admin.getId());
        assertThat(catalog.browse("", null, null, "newest", 0, 12).content()).hasSize(1);
    }

    @Test void adminListDistinguishesAdminHiddenFromShopHidden() {
        var shopHidden = products.saveAndFlush(Product.builder().id(UUID.randomUUID().toString()).shop(shop)
                .category(category).name("Shop tự ẩn").price(new BigDecimal("1000")).stock(1)
                .status(ProductStatus.INACTIVE).build());
        service.setHidden(item.getId(), true, admin.getId());
        var listed = service.products("", null, "", 0).content();
        assertThat(listed).filteredOn(p -> p.id().equals(item.getId()))
                .singleElement().extracting(ProductCardAssembler.ProductCard::adminHidden).isEqualTo(true);
        assertThat(listed).filteredOn(p -> p.id().equals(shopHidden.getId()))
                .singleElement().extracting(ProductCardAssembler.ProductCard::adminHidden).isEqualTo(false);
    }

    @Test void adminProductListFiltersByStatus() {
        var inactive = products.saveAndFlush(Product.builder().id(UUID.randomUUID().toString()).shop(shop)
                .category(category).name("Đã ẩn").price(new BigDecimal("1000")).stock(1)
                .status(ProductStatus.INACTIVE).build());
        assertThat(service.products("", null, "", 0).totalElements()).isEqualTo(2);
        assertThat(service.products("", null, "INACTIVE", 0).content())
                .extracting(ProductCardAssembler.ProductCard::id).containsExactly(inactive.getId());
        assertThat(service.products("", null, "ACTIVE", 0).content())
                .extracting(ProductCardAssembler.ProductCard::id).containsExactly(item.getId());
        assertThatThrownBy(() -> service.products("", null, "KHONG_HOP_LE", 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test void adminEndpointsRejectNonAdmin() throws Exception {
        String managerToken = auth.login(new LoginRequest(owner.getEmail(), "FlowerShop123")).accessToken();
        mvc.perform(get("/api/admin/categories").header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isForbidden());
        String adminToken = auth.login(new LoginRequest(admin.getEmail(), "FlowerShop123")).accessToken();
        mvc.perform(get("/api/admin/categories").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }
}
