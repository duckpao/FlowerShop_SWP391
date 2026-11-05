package com.example.flowershop;

import com.example.flowershop.entity.*;
import com.example.flowershop.entity.enums.*;
import com.example.flowershop.repository.*;
import com.example.flowershop.service.AdminCatalogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:admincat;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.shop-follow-mail.enabled=false", "app.occasion-reminder.enabled=false"
})
@Transactional
class AdminCategoryTests {
    @MockitoBean JavaMailSender sender;
    @Autowired AdminCatalogService service;
    @Autowired CategoryRepository categories;
    @Autowired ProductRepository products;
    @Autowired ShopRepository shops;
    @Autowired UserRepository users;

    private AdminCatalogService.CategoryInput input(String name, String group) {
        return new AdminCatalogService.CategoryInput(name, "mô tả", CategoryStatus.ACTIVE, group);
    }

    @Test
    void createStoresTrimmedGroupAndBlankGroupBecomesNull() {
        var grouped = service.createCategory("admin", input("Hoa Cúc", "  Hoa tươi "));
        var ungrouped = service.createCategory("admin", input("Hoa Lạ", "   "));
        assertThat(grouped.groupName()).isEqualTo("Hoa tươi");
        assertThat(ungrouped.groupName()).isNull();
    }

    @Test
    void renameGroupMovesEveryCategoryAndEmptyNameUngroups() {
        service.createCategory("admin", input("A", "Chủ đề"));
        service.createCategory("admin", input("B", "Chủ đề"));
        service.createCategory("admin", input("C", "Kiểu dáng"));

        assertThat(service.renameGroup("admin", new AdminCatalogService.GroupRenameInput("Chủ đề", "Dịp lễ"))).isEqualTo(2);
        assertThat(categories.findByGroupName("Dịp lễ")).hasSize(2);
        assertThat(categories.findByGroupName("Chủ đề")).isEmpty();

        service.renameGroup("admin", new AdminCatalogService.GroupRenameInput("Dịp lễ", ""));
        assertThat(categories.findByGroupName("Dịp lễ")).isEmpty();
        assertThat(service.categories().stream().filter(c -> c.groupName() == null)).hasSize(2);
    }

    @Test
    void deleteRemovesEmptyCategoryButRefusesOneWithProducts() {
        var empty = service.createCategory("admin", input("Trống", null));
        service.deleteCategory(empty.id());
        assertThat(categories.findById(empty.id())).isEmpty();

        var used = service.createCategory("admin", input("Đang dùng", null));
        var owner = users.saveAndFlush(User.builder().id(UUID.randomUUID().toString()).email(UUID.randomUUID() + "@example.test")
                .fullName("Owner").role(UserRole.SHOP).status(UserStatus.ACTIVE).isEmailVerified(true).build());
        var shop = shops.saveAndFlush(Shop.builder().id(UUID.randomUUID().toString()).owner(owner).name("S").status(ShopStatus.ACTIVE).build());
        products.saveAndFlush(Product.builder().id(UUID.randomUUID().toString()).shop(shop)
                .category(categories.findById(used.id()).orElseThrow()).name("Bó hoa").price(new BigDecimal("1000"))
                .stock(1).status(ProductStatus.ACTIVE).type(ProductType.READY_MADE).build());

        assertThatThrownBy(() -> service.deleteCategory(used.id())).isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("đang có sản phẩm");
        assertThat(categories.findById(used.id())).isPresent();
    }
}
