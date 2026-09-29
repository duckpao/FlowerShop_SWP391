package com.example.flowershop;

import com.example.flowershop.entity.*;
import com.example.flowershop.entity.enums.*;
import com.example.flowershop.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CatalogTests extends ManagerShopTests {
    @Autowired ProductRepository products;
    @Autowired CategoryRepository categories;
    @Autowired ProductImageRepository images;
    Category category;

    @Override @BeforeEach void setup() {
        images.deleteAll(); products.deleteAll(); categories.deleteAll(); super.setup();
        category = categories.saveAndFlush(Category.builder().id(UUID.randomUUID().toString())
                .name("Hoa cưới").status(CategoryStatus.ACTIVE).build());
    }

    @AfterEach void clean() { images.deleteAll(); products.deleteAll(); categories.deleteAll(); }

    Product product(String name, String price, ProductStatus status) {
        return products.saveAndFlush(Product.builder().id(UUID.randomUUID().toString())
                .shop(shop).category(category).name(name).description("mo ta")
                .price(new BigDecimal(price)).stock(5).status(status).build());
    }

    @Test void productPersistsWithoutImageColumnsAndDefaultsAdminHiddenFalse() {
        var item = product("Bó hồng", "100000", ProductStatus.ACTIVE);
        assertThat(products.findById(item.getId()).orElseThrow().isAdminHidden()).isFalse();
    }

    @Test void productImagesArePersistedAndFetchedInBatch() {
        var item = product("Bó hồng", "100000", ProductStatus.ACTIVE);
        images.saveAndFlush(ProductImage.builder().id(UUID.randomUUID().toString()).product(item)
                .imageUrl("https://cdn.example.com/a.jpg").isPrimary(true).displayOrder(0).build());
        assertThat(images.findByProductIdInOrderByDisplayOrderAscIdAsc(java.util.List.of(item.getId())))
                .singleElement().extracting(ProductImage::getImageUrl).isEqualTo("https://cdn.example.com/a.jpg");
    }

    @Test void productWithoutImagesReturnsEmptyBatch() {
        var item = product("Không ảnh", "50000", ProductStatus.ACTIVE);
        assertThat(images.findByProductIdInOrderByDisplayOrderAscIdAsc(java.util.List.of(item.getId()))).isEmpty();
    }

    // --- Task 2: ProductCardAssembler ---

    @Autowired com.example.flowershop.service.ProductCardAssembler assembler;

    /**
     * cards() đòi entity đang được quản lý vì Product.shop là LAZY, nên phải gọi trong transaction
     * giống hệt cách CatalogService/FavoriteService gọi nó ở production.
     */
    com.example.flowershop.service.ProductCardAssembler.ProductCard card(String productId) {
        return new org.springframework.transaction.support.TransactionTemplate(transactions)
                .execute(s -> assembler.cards(java.util.List.of(
                        products.findById(productId).orElseThrow())).get(0));
    }

    @Test void cardFallsBackWhenProductHasNoImageAndNoReview() {
        var item = product("Trơ trọi", "70000", ProductStatus.ACTIVE);
        var card = card(item.getId());
        assertThat(card.imageUrl()).isNull();
        assertThat(card.rating()).isZero();
        assertThat(card.reviewCount()).isZero();
        assertThat(card.available()).isTrue();
    }

    @Test void cardUsesPrimaryImageNotFirstImage() {
        var item = product("Có ảnh", "70000", ProductStatus.ACTIVE);
        images.saveAndFlush(ProductImage.builder().id(UUID.randomUUID().toString()).product(item)
                .imageUrl("https://cdn.example.com/phu.jpg").isPrimary(false).displayOrder(0).build());
        images.saveAndFlush(ProductImage.builder().id(UUID.randomUUID().toString()).product(item)
                .imageUrl("https://cdn.example.com/chinh.jpg").isPrimary(true).displayOrder(1).build());
        assertThat(card(item.getId()).imageUrl()).isEqualTo("https://cdn.example.com/chinh.jpg");
    }

    @Test void adminHiddenAndBannedShopMakeCardUnavailable() {
        var item = product("Bị ẩn", "70000", ProductStatus.ACTIVE);
        item.setAdminHidden(true); products.saveAndFlush(item);
        assertThat(card(item.getId()).available()).isFalse();
        item.setAdminHidden(false); products.saveAndFlush(item);
        shop.setStatus(ShopStatus.BANNED); shops.saveAndFlush(shop);
        assertThat(card(item.getId()).available()).isFalse();
    }

    // --- Task 3: CatalogService.browse ---

    @Autowired com.example.flowershop.service.CatalogService catalog;

    @Test void browseShowsOnlyPubliclyVisibleProducts() {
        var visible = product("Hiện", "100000", ProductStatus.ACTIVE);
        product("Ẩn", "100000", ProductStatus.INACTIVE);
        var hidden = product("Vi phạm", "100000", ProductStatus.ACTIVE);
        hidden.setAdminHidden(true); products.saveAndFlush(hidden);
        assertThat(catalog.browse("", null, null, "newest", 0, 12).content())
                .extracting(com.example.flowershop.service.ProductCardAssembler.ProductCard::id)
                .containsExactly(visible.getId());
    }

    @Test void browseHidesProductsOfBannedShop() {
        product("Hiện", "100000", ProductStatus.ACTIVE);
        shop.setStatus(ShopStatus.BANNED); shops.saveAndFlush(shop);
        assertThat(catalog.browse("", null, null, "newest", 0, 12).content()).isEmpty();
    }

    @Test void browseSortsByPrice() {
        product("Rẻ", "50000", ProductStatus.ACTIVE);
        product("Đắt", "900000", ProductStatus.ACTIVE);
        assertThat(catalog.browse("", null, null, "price_asc", 0, 12).content())
                .extracting(com.example.flowershop.service.ProductCardAssembler.ProductCard::name)
                .containsExactly("Rẻ", "Đắt");
        assertThat(catalog.browse("", null, null, "price_desc", 0, 12).content())
                .extracting(com.example.flowershop.service.ProductCardAssembler.ProductCard::name)
                .containsExactly("Đắt", "Rẻ");
    }

    @Test void browseEscapesWildcardsInSearchTerm() {
        product("Bó hồng", "50000", ProductStatus.ACTIVE);
        assertThat(catalog.browse("%", null, null, "newest", 0, 12).content()).isEmpty();
        assertThat(catalog.browse("_", null, null, "newest", 0, 12).content()).isEmpty();
        assertThat(catalog.browse("hồng", null, null, "newest", 0, 12).content()).hasSize(1);
    }

    @Test void browseRejectsBadPagingAndSort() {
        assertThatThrownBy(() -> catalog.browse("", null, null, "newest", -1, 12))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> catalog.browse("", null, null, "newest", 0, 500))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> catalog.browse("", null, null, "rating", 0, 12))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test void browseFiltersByCategory() {
        var other = categories.saveAndFlush(Category.builder().id(UUID.randomUUID().toString())
                .name("Hoa khai trương").status(CategoryStatus.ACTIVE).build());
        var mine = product("Hoa cưới", "50000", ProductStatus.ACTIVE);
        products.saveAndFlush(Product.builder().id(UUID.randomUUID().toString()).shop(shop)
                .category(other).name("Kệ hoa").price(new BigDecimal("80000")).stock(1)
                .status(ProductStatus.ACTIVE).build());
        assertThat(catalog.browse("", category.getId(), null, "newest", 0, 12).content())
                .extracting(com.example.flowershop.service.ProductCardAssembler.ProductCard::id)
                .containsExactly(mine.getId());
    }

    // --- Task 4: detail + categories ---

    @Test void detailReturnsImagesAndHidesUnavailableProducts() {
        var item = product("Bó hồng", "100000", ProductStatus.ACTIVE);
        images.saveAndFlush(ProductImage.builder().id(UUID.randomUUID().toString()).product(item)
                .imageUrl("https://cdn.example.com/a.jpg").isPrimary(true).displayOrder(0).build());
        assertThat(catalog.detail(item.getId()).images()).containsExactly("https://cdn.example.com/a.jpg");
        item.setAdminHidden(true); products.saveAndFlush(item);
        assertThatThrownBy(() -> catalog.detail(item.getId()))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    }

    @Test void detailOfUnknownIdAndBannedShopBothReturnNotFound() {
        var item = product("Bó hồng", "100000", ProductStatus.ACTIVE);
        assertThatThrownBy(() -> catalog.detail("khong-ton-tai"))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        shop.setStatus(ShopStatus.BANNED); shops.saveAndFlush(shop);
        assertThatThrownBy(() -> catalog.detail(item.getId()))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    }

    @Test void detailWorksWhenProductHasNoImages() {
        var item = product("Trơ trọi", "100000", ProductStatus.ACTIVE);
        assertThat(catalog.detail(item.getId()).images()).isEmpty();
        assertThat(catalog.detail(item.getId()).rating()).isZero();
    }

    @Test void publicCategoriesCountOnlyVisibleProducts() {
        product("Hiện", "100000", ProductStatus.ACTIVE);
        product("Ẩn", "100000", ProductStatus.INACTIVE);
        assertThat(catalog.categories()).singleElement()
                .extracting(com.example.flowershop.service.CatalogService.CategoryCard::productCount)
                .isEqualTo(1L);
    }

    // --- Task 5: endpoint công khai ---

    @Test void publicCatalogEndpointsNeedNoLogin() throws Exception {
        product("Bó hồng", "100000", ProductStatus.ACTIVE);
        mvc.perform(get("/api/public/products?page=0&size=12")).andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Bó hồng"));
        mvc.perform(get("/api/public/categories")).andExpect(status().isOk());
        mvc.perform(get("/api/public/products/khong-ton-tai")).andExpect(status().isNotFound());
        mvc.perform(get("/api/public/products?size=500")).andExpect(status().isBadRequest());
    }
}
