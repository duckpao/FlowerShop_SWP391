package com.example.flowershop;
import com.example.flowershop.entity.*;
import com.example.flowershop.entity.enums.*;
import com.example.flowershop.repository.*;
import com.example.flowershop.service.*;
import com.example.flowershop.dto.auth.LoginRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.junit.jupiter.api.*;
import java.math.BigDecimal;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
class ManagerApplicationProductTests extends ManagerShopTests {
    @Autowired ManagerApplicationService applications;
    @Autowired ManagerApplicationRepository applicationRows;
    @Autowired ManagerProductService catalog;
    @Autowired com.example.flowershop.service.CatalogService publicCatalog;
    @Autowired ProductRepository products;
    @Autowired CategoryRepository categories;
    @Autowired com.example.flowershop.repository.ProductImageRepository productImages;
    Category category;
    @Override @BeforeEach void setup() {
        productImages.deleteAll();applicationRows.deleteAll();products.deleteAll();categories.deleteAll();super.setup();
        category=categories.saveAndFlush(Category.builder().id(UUID.randomUUID().toString()).name("Flowers").status(CategoryStatus.ACTIVE).build());
    }
    @AfterEach void clean() {productImages.deleteAll();applicationRows.deleteAll();products.deleteAll();categories.deleteAll();}
    ManagerApplicationService.Input application() {return new ManagerApplicationService.Input("Applicant","0912345678","New flower shop","Fresh flowers","12 Test","Hà Nội","Test","Test");}
    ManagerProductService.Input product() {return productWithImages(null);}
    ManagerProductService.Input productWithImages(java.util.List<ManagerProductService.ImageInput> list) {
        return new ManagerProductService.Input("Rose bouquet","Red roses",category.getId(),new BigDecimal("250000"),10,ProductStatus.ACTIVE,list);
    }
    @Test void customerStaysCustomerUntilApprovalCreatesExactlyOneShopAndRevokesSessions() {
        var customer=user(UserRole.CUSTOMER);var tokens=auth.login(new LoginRequest(customer.getEmail(),"FlowerShop123"));
        var a=applications.submit(customer.getId(),application());
        assertThat(users.findById(customer.getId()).orElseThrow().getRole()).isEqualTo(UserRole.CUSTOMER);
        assertThat(shops.findByOwnerIdOrderByNameAsc(customer.getId())).isEmpty();
        assertThatThrownBy(()->applications.submit(customer.getId(),application())).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        var approved=applications.decide(a.id(),owner.getId(),new ManagerApplicationService.Decision(true,"Approved"));
        assertThat(users.findById(customer.getId()).orElseThrow().getRole()).isEqualTo(UserRole.SHOP);
        assertThat(shops.findByOwnerIdOrderByNameAsc(customer.getId())).hasSize(1);
        assertThat(shops.findById(approved.shopId()).orElseThrow().getStatus()).isEqualTo(ShopStatus.ACTIVE);
        assertThat(addresses.findByShopIdAndUserIsNullOrderByCreatedDateAscIdAsc(approved.shopId())).hasSize(1);
        assertThatThrownBy(()->auth.authenticate(tokens.accessToken())).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(()->auth.refresh(tokens.refreshToken())).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(()->applications.decide(a.id(),owner.getId(),new ManagerApplicationService.Decision(true,""))).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        var item=catalog.save(approved.shopId(),null,customer.getId(),product());
        assertThat(publicCatalog.published(approved.shopId(),0).content()).extracting("id").containsExactly(item.id());
    }
    @Test void rejectedApplicationPreservesRoleAndCanBeResubmitted() {
        var customer=user(UserRole.CUSTOMER);var a=applications.submit(customer.getId(),application());
        applications.decide(a.id(),owner.getId(),new ManagerApplicationService.Decision(false,"Need more information"));
        assertThat(users.findById(customer.getId()).orElseThrow().getRole()).isEqualTo(UserRole.CUSTOMER);
        assertThat(shops.findByOwnerIdOrderByNameAsc(customer.getId())).isEmpty();
        assertThat(applications.submit(customer.getId(),application()).status()).isEqualTo("PENDING");
    }
    @Test void roleChangeBeforeApprovalCannotGrantManager() {
        var customer=user(UserRole.CUSTOMER);var a=applications.submit(customer.getId(),application());
        customer.setRole(UserRole.SHOP_STAFF);users.saveAndFlush(customer);
        assertThatThrownBy(()->applications.decide(a.id(),owner.getId(),new ManagerApplicationService.Decision(true,""))).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        assertThat(shops.findByOwnerIdOrderByNameAsc(customer.getId())).isEmpty();
        assertThat(applications.mine(customer.getId()).get(0).status()).isEqualTo("PENDING");
    }
    @Test void managerCannotReadCreateEditHideOrMoveOtherShopsProducts() {
        var second=shops.saveAndFlush(Shop.builder().id(UUID.randomUUID().toString()).owner(other).name("Other shop").status(ShopStatus.ACTIVE).build());
        var item=catalog.save(shop.getId(),null,owner.getId(),product());
        assertThatThrownBy(()->catalog.list(shop.getId(),other.getId(),0)).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        assertThatThrownBy(()->catalog.save(shop.getId(),null,other.getId(),product())).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        assertThatThrownBy(()->catalog.save(shop.getId(),item.id(),other.getId(),product())).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        assertThatThrownBy(()->catalog.save(second.getId(),item.id(),other.getId(),product())).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        assertThatThrownBy(()->catalog.hide(second.getId(),item.id(),other.getId())).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        assertThatThrownBy(()->catalog.hide(shop.getId(),item.id(),other.getId())).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        assertThat(catalog.list(shop.getId(),owner.getId(),0).content()).hasSize(1);
        catalog.hide(shop.getId(),item.id(),owner.getId());assertThat(publicCatalog.published(shop.getId(),0).content()).isEmpty();
    }
    @Test void pendingAndBannedShopsCannotPublish() {
        for(var status:java.util.List.of(ShopStatus.PENDING,ShopStatus.BANNED)) {
            shop.setStatus(status);shops.saveAndFlush(shop);
            assertThatThrownBy(()->catalog.save(shop.getId(),null,owner.getId(),product())).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
            assertThatThrownBy(()->publicCatalog.published(shop.getId(),0)).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        }
    }
    @Test void endpointRolesCsrfAndProductValidation() throws Exception {
        var customer=user(UserRole.CUSTOMER);String customerToken=auth.login(new LoginRequest(customer.getEmail(),"FlowerShop123")).accessToken();
        mvc.perform(get("/api/admin/shops/applications").header("Authorization","Bearer "+customerToken)).andExpect(status().isForbidden());
        mvc.perform(get("/api/shop/mine/"+shop.getId()+"/products").header("Authorization","Bearer "+customerToken)).andExpect(status().isForbidden());
        var admin=user(UserRole.ADMIN);String adminToken=auth.login(new LoginRequest(admin.getEmail(),"FlowerShop123")).accessToken();
        mvc.perform(get("/api/admin/shops/applications").header("Authorization","Bearer "+adminToken)).andExpect(status().isOk());
        String managerToken=auth.login(new LoginRequest(owner.getEmail(),"FlowerShop123")).accessToken();
        mvc.perform(post("/api/shop/mine/"+shop.getId()+"/products").header("Authorization","Bearer "+managerToken).contentType("application/json").content("{}")).andExpect(status().isForbidden());
        var csrf=mvc.perform(get("/api/auth/csrf")).andReturn();
        var session=(org.springframework.mock.web.MockHttpSession)csrf.getRequest().getSession(false);
        var match=java.util.regex.Pattern.compile("\"token\":\"([^\"]+)\"").matcher(csrf.getResponse().getContentAsString());assertThat(match.find()).isTrue();
        mvc.perform(post("/api/shop/mine/"+shop.getId()+"/products").session(session).header("X-CSRF-TOKEN",match.group(1)).header("Authorization","Bearer "+managerToken).contentType("application/json").content("{}")).andExpect(status().isBadRequest());
    }

    @Test void savingImagesReplacesListAndForcesExactlyOnePrimary() {
        var saved = catalog.save(shop.getId(), null, owner.getId(), productWithImages(java.util.List.of(
                new ManagerProductService.ImageInput("https://cdn.example.com/a.jpg", false),
                new ManagerProductService.ImageInput("https://cdn.example.com/b.jpg", false))));
        var stored = productImages.findByProductIdOrderByDisplayOrderAscIdAsc(saved.id());
        assertThat(stored).hasSize(2);
        assertThat(stored.stream().filter(i -> Boolean.TRUE.equals(i.getIsPrimary())).count()).isEqualTo(1);
        assertThat(stored.get(0).getImageUrl()).isEqualTo("https://cdn.example.com/a.jpg");

        catalog.save(shop.getId(), saved.id(), owner.getId(), productWithImages(java.util.List.of(
                new ManagerProductService.ImageInput("https://cdn.example.com/c.jpg", true))));
        assertThat(productImages.findByProductIdOrderByDisplayOrderAscIdAsc(saved.id()))
                .singleElement().extracting(i -> i.getImageUrl()).isEqualTo("https://cdn.example.com/c.jpg");
    }

    @Test void resultListsPrimaryImageFirstSoReopeningTheFormKeepsIt() {
        var saved = catalog.save(shop.getId(), null, owner.getId(), productWithImages(java.util.List.of(
                new ManagerProductService.ImageInput("https://cdn.example.com/a.jpg", false),
                new ManagerProductService.ImageInput("https://cdn.example.com/b.jpg", true))));
        // Giao diện dựng lại cờ primary theo vị trí đầu danh sách, nên ảnh chính phải đứng đầu.
        assertThat(saved.images()).containsExactly("https://cdn.example.com/b.jpg", "https://cdn.example.com/a.jpg");
    }

    @Test void nullImageListLeavesExistingImagesUntouched() {
        var saved = catalog.save(shop.getId(), null, owner.getId(), productWithImages(java.util.List.of(
                new ManagerProductService.ImageInput("https://cdn.example.com/a.jpg", true))));
        catalog.save(shop.getId(), saved.id(), owner.getId(), productWithImages(null));
        assertThat(productImages.findByProductIdOrderByDisplayOrderAscIdAsc(saved.id())).hasSize(1);
    }

    @Test void managerCannotClearAdminHiddenFlag() {
        var saved = catalog.save(shop.getId(), null, owner.getId(), productWithImages(null));
        var entity = products.findById(saved.id()).orElseThrow();
        entity.setAdminHidden(true); products.saveAndFlush(entity);
        catalog.save(shop.getId(), saved.id(), owner.getId(), productWithImages(null));
        assertThat(products.findById(saved.id()).orElseThrow().isAdminHidden()).isTrue();
    }
}
