package com.example.flowershop;

import com.example.flowershop.entity.*;
import com.example.flowershop.entity.enums.*;
import com.example.flowershop.repository.*;
import com.example.flowershop.service.CustomFlowerOrderService;
import com.example.flowershop.service.CustomFlowerOrderService.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:customflowers;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa","spring.datasource.password=","spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop","app.shop-follow-mail.enabled=false","spring.jpa.show-sql=false"})
@Transactional
@AutoConfigureMockMvc
class CustomFlowerOrderTests {
    @MockitoBean JavaMailSender mail;
    @MockitoBean com.example.flowershop.service.TokenAuthService tokenAuth;
    @Autowired CustomFlowerOrderService service;
    @Autowired UserRepository users;
    @Autowired ShopRepository shops;
    @Autowired CategoryRepository categories;
    @Autowired ProductRepository products;
    @Autowired CustomOrderRequestRepository requests;
    @Autowired MockMvc mvc;
    @Autowired jakarta.validation.Validator validator;
    User owner,customer; Shop shop; Category category; Product rose;
    String id(){return UUID.randomUUID().toString();}
    User user(UserRole role){return users.saveAndFlush(User.builder().id(id()).email(id()+"@example.test").fullName("Test user").role(role).status(UserStatus.ACTIVE).isEmailVerified(true).build());}
    Product product(String name,int price,int stock){return products.saveAndFlush(Product.builder().id(id()).shop(shop).category(category).name(name).price(BigDecimal.valueOf(price)).stock(stock).build());}
    RequestInput input(String reference){return new RequestInput(shop.getId(),new BigDecimal("300000"),2,"Pastel birthday bouquet",new Preferences("Rose","Pink","Birthday","Paper",null,reference));}
    @BeforeEach void fixture(){
        owner=user(UserRole.SHOP);customer=user(UserRole.CUSTOMER);
        shop=shops.saveAndFlush(Shop.builder().id(id()).owner(owner).name("Custom test shop").status(ShopStatus.ACTIVE).build());
        category=categories.saveAndFlush(Category.builder().id(id()).name("Flowers").status(CategoryStatus.ACTIVE).build());
        rose=product("Rose",100000,10);
    }
    @Test void persistsDesignAndPrivateHistory(){
        var r=service.create(customer.getId(),input(rose.getId()));
        assertThat(r.status()).isEqualTo(CustomOrderStatus.REQUESTED);
        assertThat(service.detail(customer.getId(),r.id()).design().estimatedTotal()).isEqualByComparingTo("200000");
        assertThat(service.mine(customer.getId(),0).totalElements()).isEqualTo(1);
        var other=user(UserRole.CUSTOMER);
        assertThat(service.mine(other.getId(),0).content()).isEmpty();
        assertThatThrownBy(()->service.detail(other.getId(),r.id())).isInstanceOf(ResponseStatusException.class);
        assertThat(rose.getStock()).isEqualTo(10);
    }
    @Test void optionalDescriptionAndMultipleAccessoriesPersist(){
        for (String description : new String[]{null, "", "   ", "Đẹp"}) {
            var input = new RequestInput(shop.getId(), new BigDecimal("300000"), 1, description,
                    new Preferences(null, null, "Khác", "Giấy kraft, Ruy băng, Thiệp chúc mừng", null, null));
            assertThat(validator.validate(input)).isEmpty();
            var created = service.create(customer.getId(), input);
            var saved = service.detail(customer.getId(), created.id());
            assertThat(saved.description()).isEqualTo(description == null ? "" : description.strip());
            assertThat(saved.design().wrapping()).isEqualTo(input.preferences().wrapping());
            assertThat(saved.design().occasion()).isEqualTo("Khác");
        }
        var tooLong = new RequestInput(shop.getId(), new BigDecimal("300000"), 1, "a".repeat(3001),
                new Preferences(null, null, null, null, null, null));
        assertThat(validator.validate(tooLong)).anyMatch(v -> v.getPropertyPath().toString().equals("description"));
    }
    @Test void filtersSuggestionsByBudgetQuantityVisibilityAndStock(){
        product("Too expensive",200000,10);product("No stock",90000,1);
        var hidden=product("Hidden",80000,10);hidden.setAdminHidden(true);products.saveAndFlush(hidden);
        var exact=product("Exact budget",150000,2);
        var result=service.suggestions(new BigDecimal("300000"),2,shop.getId(),"",0);
        assertThat(result.content()).hasSize(2);
        assertThat(result.content().get(0).product().id()).isEqualTo(exact.getId());
        assertThat(result.content().get(0).remainingBudget()).isEqualByComparingTo("0");
        shop.setStatus(ShopStatus.INACTIVE);shops.saveAndFlush(shop);
        assertThat(service.suggestions(new BigDecimal("300000"),2,null,"",0).content()).isEmpty();
    }
    @Test void referenceIsRevalidatedAtSubmission(){
        rose.setPrice(new BigDecimal("200000"));products.saveAndFlush(rose);
        assertThatThrownBy(()->service.create(customer.getId(),input(rose.getId()))).isInstanceOf(ResponseStatusException.class);
    }
    @Test void invalidBudgetAndQuantityAreRejected(){
        assertThatThrownBy(()->service.suggestions(BigDecimal.ZERO,1,null,"",0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->service.suggestions(new BigDecimal("1.001"),1,null,"",0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->service.suggestions(new BigDecimal("300000"),101,null,"",0)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void lifecycleAllowsNegotiationThenAcceptance(){
        var r=service.create(customer.getId(),input(null));
        service.decide(shop.getId(),owner.getId(),r.id(),new Decision(CustomOrderStatus.NEGOTIATING,"Can make pink roses",new BigDecimal("250000")));
        var accepted=service.decide(shop.getId(),owner.getId(),r.id(),new Decision(CustomOrderStatus.ACCEPTED,"Design confirmed",new BigDecimal("260000")));
        assertThat(accepted.design().quotedPrice()).isEqualByComparingTo("260000");
        assertThat(service.detail(customer.getId(),r.id()).status()).isEqualTo(CustomOrderStatus.ACCEPTED);
        assertThatThrownBy(()->service.decide(shop.getId(),owner.getId(),r.id(),new Decision(CustomOrderStatus.REJECTED,"Changed mind",null))).isInstanceOf(ResponseStatusException.class);
    }
    @Test void rejectsOverBudgetQuoteAndUnauthorizedShop(){
        var r=service.create(customer.getId(),input(null));
        assertThatThrownBy(()->service.decide(shop.getId(),owner.getId(),r.id(),new Decision(CustomOrderStatus.ACCEPTED,"Quote",new BigDecimal("300001")))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->service.decide(shop.getId(),owner.getId(),r.id(),new Decision(CustomOrderStatus.ACCEPTED,"Quote",null))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->service.shopRequests(shop.getId(),user(UserRole.SHOP).getId(),null,0)).isInstanceOf(ResponseStatusException.class);
    }
    @Test void readsLegacyFlowerArray(){
        var r=service.create(customer.getId(),input(null));var entity=requests.findById(r.id()).orElseThrow();
        entity.setDesiredComponents("{\"flowers\":[\"Rose\",\"Baby\"],\"wrapper\":\"Kraft\"}");requests.saveAndFlush(entity);
        var design=service.detail(customer.getId(),r.id()).design();
        assertThat(design.quantity()).isEqualTo(1);assertThat(design.flowers()).isEqualTo("Rose, Baby");assertThat(design.wrapping()).isEqualTo("Kraft");
    }
    @Test void publicSuggestionsAndPrivateApis() throws Exception {
        mvc.perform(get("/budget-flowers")).andExpect(status().isOk()).andExpect(forwardedUrl("/index.html"));
        mvc.perform(get("/custom-orders/new")).andExpect(status().isOk()).andExpect(forwardedUrl("/index.html"));
        mvc.perform(get("/api/public/custom-orders/suggestions").param("budget","300000").param("quantity","2").param("shopId",shop.getId())).andExpect(status().isOk()).andExpect(jsonPath("$.content[0].estimatedTotal").value(200000));
        mvc.perform(get("/api/public/custom-orders/suggestions").param("budget","-1")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/customer/custom-orders")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/shop/mine/"+shop.getId()+"/custom-orders")).andExpect(status().isUnauthorized());
    }
    @Test void customerCanReadSubmittedRequestsAndShopCanListThem() throws Exception {
        var r = service.create(customer.getId(), input(null));
        org.mockito.Mockito.when(tokenAuth.authenticate("customer-test"))
                .thenReturn(com.example.flowershop.dto.auth.CurrentUser.from(customer));
        org.mockito.Mockito.when(tokenAuth.authenticate("shop-test"))
                .thenReturn(com.example.flowershop.dto.auth.CurrentUser.from(owner));
        mvc.perform(get("/api/customer/custom-orders").header("Authorization", "Bearer customer-test"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].id").value(r.id()));
        mvc.perform(get("/api/customer/custom-orders").param("page", "0").header("Authorization", "Bearer customer-test"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/customer/custom-orders/" + r.id()).header("Authorization", "Bearer customer-test"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(r.id()));
        mvc.perform(get("/api/shop/mine/" + shop.getId() + "/custom-orders")
                        .param("page", "0").param("status", "REQUESTED").header("Authorization", "Bearer shop-test"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].id").value(r.id()));
    }
    @Test void rejectsPastDateAndOtherShopsReference(){
        var past=new RequestInput(shop.getId(),new BigDecimal("300000"),1,"Birthday bouquet please",new Preferences(null,null,null,null,"2000-01-01",null));
        assertThatThrownBy(()->service.create(customer.getId(),past)).isInstanceOf(IllegalArgumentException.class);
        var other=shops.saveAndFlush(Shop.builder().id(id()).owner(user(UserRole.SHOP)).name("Other shop").status(ShopStatus.ACTIVE).build());
        var wrong=new RequestInput(other.getId(),new BigDecimal("300000"),1,"Birthday bouquet please",new Preferences(null,null,null,null,null,rose.getId()));
        assertThatThrownBy(()->service.create(customer.getId(),wrong)).isInstanceOf(ResponseStatusException.class);
    }
}
