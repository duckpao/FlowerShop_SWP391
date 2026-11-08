package com.example.flowershop;

import com.example.flowershop.dto.auth.CurrentUser;
import com.example.flowershop.entity.*;
import com.example.flowershop.entity.Order;
import com.example.flowershop.entity.enums.*;
import com.example.flowershop.repository.*;
import com.example.flowershop.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:adminapi;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa", "spring.datasource.password=", "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.show-sql=false",
        "app.shop-follow-mail.enabled=false", "app.occasion-reminder.enabled=false"})
@AutoConfigureMockMvc
@Transactional
public class AdminApiTests {
    @MockitoBean JavaMailSender mail;
    @MockitoBean TokenAuthService tokens;
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired UserRepository users;
    @Autowired ShopRepository shops;
    @Autowired ProductRepository products;
    @Autowired CategoryRepository categories;
    @Autowired OrderRepository orders;
    @Autowired AuthSessionRepository sessions;
    @Autowired ManagerApplicationService applications;
    @Autowired jakarta.persistence.EntityManager entityManager;
    final ObjectMapper json=new ObjectMapper();
    User admin,customer,owner;
    Shop shop;
    Product product;
    String id() { return UUID.randomUUID().toString(); }
    User user(UserRole role,String name) {
        return users.saveAndFlush(User.builder().id(id()).email(id()+"@example.test").fullName(name)
                .role(role).status(UserStatus.ACTIVE).isEmailVerified(true).build());
    }
    // Test-only equivalent of the MySQL date bucketing function used by the dashboard.
    public static String dateFormat(Timestamp timestamp,String format) {
        return timestamp.toLocalDateTime().format(DateTimeFormatter.ofPattern(switch(format) {
            case "%Y-%m-%d" -> "yyyy-MM-dd"; case "%Y-%m" -> "yyyy-MM"; case "%Y" -> "yyyy";
            default -> throw new IllegalArgumentException(format);
        }));
    }
    @BeforeEach void setup() {
        jdbc.execute("CREATE ALIAS IF NOT EXISTS DATE_FORMAT FOR 'com.example.flowershop.AdminApiTests.dateFormat'");
        admin=user(UserRole.ADMIN,"Admin"); customer=user(UserRole.CUSTOMER,"Customer 100% flowers"); owner=user(UserRole.SHOP,"Owner");
        shop=shops.saveAndFlush(Shop.builder().id(id()).owner(owner).name("Shop 100% flowers").status(ShopStatus.PENDING).build());
        var category=categories.saveAndFlush(Category.builder().id(id()).name("Admin test category").status(CategoryStatus.ACTIVE).build());
        product=products.saveAndFlush(Product.builder().id(id()).shop(shop).category(category).name("Admin test rose")
                .price(new BigDecimal("100000")).stock(3).status(ProductStatus.ACTIVE).build());
        when(tokens.authenticate("admin-test")).thenReturn(CurrentUser.from(admin));
        when(tokens.authenticate("customer-test")).thenReturn(CurrentUser.from(customer));
        when(tokens.authenticate("shop-test")).thenReturn(CurrentUser.from(owner));
    }
    MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder request) {
        return request.header("Authorization","Bearer admin-test");
    }
    MockHttpServletRequestBuilder write(MockHttpServletRequestBuilder request) throws Exception {
        var result=mvc.perform(get("/api/auth/csrf")).andReturn();
        var csrf=json.readTree(result.getResponse().getContentAsString());
        return auth(request.session((MockHttpSession)result.getRequest().getSession(false))
                .header(csrf.path("headerName").asText(),csrf.path("token").asText()).contentType("application/json"));
    }
    @Test void adminListsBindFiltersAndEscapeSearchWildcards() throws Exception {
        mvc.perform(auth(get("/api/admin/customers").param("q","100%").param("status","ACTIVE").param("page","0").param("size","1")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].id").value(customer.getId()))
                .andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(auth(get("/api/admin/shops").param("q","100%").param("status","PENDING").param("size","1")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].id").value(shop.getId()));
        mvc.perform(auth(get("/api/admin/products").param("q","Admin test").param("shopId",shop.getId()).param("status","ACTIVE")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].id").value(product.getId()));
        mvc.perform(auth(get("/api/admin/customers/"+customer.getId()))).andExpect(status().isOk());
        mvc.perform(auth(get("/api/admin/shops/"+shop.getId()))).andExpect(status().isOk());
        mvc.perform(auth(get("/api/admin/categories"))).andExpect(status().isOk());
        mvc.perform(auth(get("/api/admin/vouchers"))).andExpect(status().isOk());
    }
    @Test void blockingCustomerRevokesSessionsAndCannotBlockAdmin() throws Exception {
        var session=new AuthSession(); session.setId(id()); session.setUserId(customer.getId()); session.setRefreshHash("x".repeat(64));
        session.setExpiresAt(Instant.now().plusSeconds(3600)); sessions.saveAndFlush(session);
        mvc.perform(write(put("/api/admin/customers/"+customer.getId()+"/blocked")).content("{\"blocked\":true}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("BANNED"));
        entityManager.clear();
        assertThat(sessions.findById(session.getId()).orElseThrow().isRevoked()).isTrue();
        mvc.perform(write(put("/api/admin/customers/"+customer.getId()+"/blocked")).content("{\"blocked\":false}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ACTIVE"));
        mvc.perform(write(put("/api/admin/customers/"+admin.getId()+"/blocked")).content("{\"blocked\":true}"))
                .andExpect(status().isNotFound());
    }
    @Test void shopApprovalBlockingAndUnblockingUseExpectedStates() throws Exception {
        for (String action:List.of("approve","block","unblock")) {
            mvc.perform(write(put("/api/admin/shops/"+shop.getId()+"/"+action)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.status").value(action.equals("block")?"BANNED":"ACTIVE"));
        }
        mvc.perform(write(put("/api/admin/shops/"+shop.getId()+"/approve"))).andExpect(status().isConflict());
    }
    @Test void categoryCrudAndProductModerationUseAdminApis() throws Exception {
        String body="{\"name\":\"New category\",\"description\":\"\",\"status\":\"ACTIVE\",\"groupName\":\"New group\"}";
        var created=mvc.perform(write(post("/api/admin/categories")).content(body)).andExpect(status().isCreated()).andReturn();
        String categoryId=json.readTree(created.getResponse().getContentAsString()).path("id").asText();
        mvc.perform(write(put("/api/admin/categories/"+categoryId)).content(body.replace("New category","Updated category")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Updated category"));
        mvc.perform(write(put("/api/admin/category-groups")).content("{\"from\":\"New group\",\"to\":\"Updated group\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.updated").value(1));
        mvc.perform(write(delete("/api/admin/categories/"+categoryId))).andExpect(status().isNoContent());
        for (boolean hidden:new boolean[]{true,false}) {
            mvc.perform(write(put("/api/admin/products/"+product.getId()+"/hidden")).content("{\"hidden\":"+hidden+"}"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.adminHidden").value(hidden));
        }
    }
    @Test void approvingApplicationCreatesOneShopAndRejectingDoesNotPromoteCustomer() throws Exception {
        var input=new ManagerApplicationService.Input("Applicant","0901234567","New flower shop","Flowers","Street","City","District","Ward");
        var pending=applications.submit(customer.getId(),input);
        mvc.perform(auth(get("/api/admin/shops/applications").param("status","PENDING").param("page","0")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].id").value(pending.id()));
        mvc.perform(write(put("/api/admin/shops/applications/"+pending.id())).content("{\"approve\":true,\"note\":\"Approved\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("APPROVED"));
        assertThat(shops.findByOwnerIdOrderByNameAsc(customer.getId())).hasSize(1);
        assertThat(users.findById(customer.getId()).orElseThrow().getRole()).isEqualTo(UserRole.SHOP);
        mvc.perform(write(put("/api/admin/shops/applications/"+pending.id())).content("{\"approve\":true,\"note\":\"Again\"}"))
                .andExpect(status().isConflict());
        var other=user(UserRole.CUSTOMER,"Other applicant");
        var rejected=applications.submit(other.getId(),input);
        mvc.perform(write(put("/api/admin/shops/applications/"+rejected.id())).content("{\"approve\":false,\"note\":\"Rejected\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("REJECTED"));
        assertThat(users.findById(other.getId()).orElseThrow().getRole()).isEqualTo(UserRole.CUSTOMER);
        assertThat(shops.findByOwnerIdOrderByNameAsc(other.getId())).isEmpty();
    }
    @Test void dashboardIncludesCustomRequestsButOnlyCompletedOrdersCountAsRevenue() throws Exception {
        for (var state:List.of(OrderStatus.AWAITING_QUOTE,OrderStatus.QUOTED,OrderStatus.PROCESSING,OrderStatus.COMPLETED,OrderStatus.CANCELLED)) {
            orders.saveAndFlush(Order.builder().id(id()).customer(customer).shop(shop).orderType(OrderType.CUSTOM).status(state)
                    .subTotal(new BigDecimal("100000")).totalAmount(new BigDecimal("100000")).build());
        }
        for (String period:List.of("DAY","MONTH","YEAR")) {
            mvc.perform(auth(get("/api/admin/dashboard").param("granularity",period)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.summary.ordersPlacing").value(3))
                    .andExpect(jsonPath("$.totals.orders.ordersPlacing").value(3))
                    .andExpect(jsonPath("$.summary.ordersClosed").value(1))
                    .andExpect(jsonPath("$.summary.revenue").value(100000));
        }
    }
    @Test void malformedFiltersReturn400RatherThan500() throws Exception {
        for (String path:List.of("/api/admin/dashboard?from=invalid&to=2026-11-06", "/api/admin/customers?page=wrong",
                "/api/admin/shops?size=wrong", "/api/admin/products?page=-1", "/api/admin/dashboard?granularity=INVALID",
                "/api/admin/dashboard?from=2026-11-06", "/api/admin/shops/applications?status=UNKNOWN")) {
            mvc.perform(auth(get(path))).andExpect(status().isBadRequest());
        }
    }
    @Test void nonAdminsCannotAccessAnyAdminSection() throws Exception {
        for (String path:List.of("/api/admin/dashboard","/api/admin/customers","/api/admin/shops",
                "/api/admin/categories","/api/admin/products","/api/admin/shops/applications","/api/admin/vouchers")) {
            mvc.perform(get(path)).andExpect(status().isUnauthorized());
            for (String token:List.of("customer-test","shop-test")) {
                mvc.perform(get(path).header("Authorization","Bearer "+token)).andExpect(status().isForbidden());
            }
        }
    }
}
