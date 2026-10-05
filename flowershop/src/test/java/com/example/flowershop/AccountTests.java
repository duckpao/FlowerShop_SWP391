package com.example.flowershop;

import com.example.flowershop.dto.account.*;
import com.example.flowershop.dto.auth.LoginRequest;
import com.example.flowershop.entity.*;
import com.example.flowershop.entity.enums.*;
import com.example.flowershop.repository.*;
import com.example.flowershop.service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:accounttest;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa","spring.datasource.password=","spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop","debug=false"})
@AutoConfigureMockMvc
class AccountTests {
    @Autowired AccountService accounts;
    @Autowired TokenAuthService auth;
    @Autowired UserRepository users;
    @Autowired AddressRepository addresses;
    @Autowired AuthSessionRepository sessions;
    @Autowired PasswordEncoder encoder;
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @MockitoBean JavaMailSender mail;
    User user, other;
    String bearer, csrf;
    MockHttpSession httpSession;

    @BeforeEach void setup() throws Exception {
        jdbc.update("DELETE FROM Orders"); addresses.deleteAll(); jdbc.update("DELETE FROM Shops");
        sessions.deleteAll(); users.deleteAll();
        user=create("one@example.com"); other=create("two@example.com");
        bearer="Bearer "+auth.login(new LoginRequest(user.getEmail(),"FlowerShop123")).accessToken();
        loadCsrf();
    }
    void loadCsrf() throws Exception {
        var request=get("/api/auth/csrf");
        if (httpSession != null) request.session(httpSession);
        var result=mvc.perform(request).andReturn();
        httpSession=(MockHttpSession)result.getRequest().getSession(false);
        var match=Pattern.compile("\"token\":\"([^\"]+)\"").matcher(result.getResponse().getContentAsString());
        assertThat(match.find()).isTrue(); csrf=match.group(1);
    }
    User create(String email) {
        return users.saveAndFlush(User.builder().id(UUID.randomUUID().toString()).email(email).fullName("Test User")
                .passwordHash(encoder.encode("FlowerShop123")).role(UserRole.CUSTOMER).status(UserStatus.ACTIVE).isEmailVerified(true).build());
    }
    AddressRequest input(boolean def) { return new AddressRequest("12 Đường Test","Hà Nội","Khu vực Test","Phường Test","00001",1442,def); }
    void expect404(Runnable operation) {
        assertThatThrownBy(operation::run).isInstanceOfSatisfying(ResponseStatusException.class,e -> assertThat(e.getStatusCode().value()).isEqualTo(404));
    }
    @Test void profileUpdatesOnlyPermittedFieldsAndNeverReturnsSecrets() throws Exception {
        mvc.perform(put("/api/account/profile").header("Authorization",bearer).session(httpSession).header("X-CSRF-TOKEN",csrf)
                .contentType("application/json").content("{\"fullName\":\" New Name \",\"phone\":\"0912345678\",\"role\":\"ADMIN\",\"email\":\"attacker@example.com\",\"passwordHash\":\"bad\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.fullName").value("New Name"))
                .andExpect(jsonPath("$.role").value("CUSTOMER")).andExpect(jsonPath("$.email").value(user.getEmail()))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
        assertThat(users.findById(user.getId()).orElseThrow().getPasswordHash()).isEqualTo(user.getPasswordHash());
        assertThat(accounts.profile(other.getId()).fullName()).isEqualTo("Test User");
    }
    @Test void validationAndCsrfAndAuthenticationAreRequired() throws Exception {
        mvc.perform(get("/api/account/profile")).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/account/profile").header("Authorization",bearer).contentType("application/json")
                .content("{\"fullName\":\"Name\",\"phone\":\"\"}")).andExpect(status().isForbidden());
        mvc.perform(put("/api/account/profile").header("Authorization",bearer).session(httpSession).header("X-CSRF-TOKEN",csrf)
                .contentType("application/json").content("{\"fullName\":\" \",\"phone\":\"letters\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.fullName").exists()).andExpect(jsonPath("$.errors.phone").exists());
        loadCsrf();
        mvc.perform(post("/api/account/addresses").header("Authorization",bearer).session(httpSession).header("X-CSRF-TOKEN",csrf)
                .contentType("application/json").content("{\"addressLine\":\"\",\"city\":\"Hà Nội\",\"district\":\"\",\"ward\":\"\"}"))
                .andExpect(status().isBadRequest());
    }
    @Test void allFourRolesHaveProfilesAndPersonalAddresses() throws Exception {
        for (var role:List.of(UserRole.ADMIN,UserRole.SHOP,UserRole.SHOP_STAFF,UserRole.CUSTOMER)) {
            user.setRole(role); users.saveAndFlush(user);
            mvc.perform(get("/api/account/profile").header("Authorization",bearer)).andExpect(status().isOk());
            mvc.perform(get("/api/account/addresses").header("Authorization",bearer))
                    .andExpect(status().isOk());
        }
    }
    @Test void personalAddressesCanBeOutsideDeliveryArea() throws Exception {
        mvc.perform(get("/api/account/delivery-areas").header("Authorization",bearer))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0]").value("Hà Nội"));
        mvc.perform(post("/api/account/addresses").header("Authorization",bearer).session(httpSession).header("X-CSRF-TOKEN",csrf)
                .contentType("application/json").content("{\"addressLine\":\"12 Test\",\"city\":\"Đà Nẵng\",\"district\":\"Test\",\"ward\":\"Test\",\"isDefault\":false}"))
    @Test void anyCityIsAcceptedNationwide() throws Exception {
        mvc.perform(post("/api/account/addresses").header("Authorization",bearer).session(httpSession).header("X-CSRF-TOKEN",csrf)
                .contentType("application/json").content("{\"addressLine\":\"12 Test\",\"city\":\"Đà Nẵng\",\"district\":\"Test\",\"ward\":\"Test\",\"ghnWardCode\":\"20308\",\"ghnDistrictId\":1490}"))
                .andExpect(status().isCreated());
        assertThat(addresses.count()).isEqualTo(1);
    }
    @Test void httpAddressLifecycleUsesJwtOwnerAndReturnsExpectedShapes() throws Exception {
        String body="{\"addressLine\":\"12 Test\",\"city\":\"Hà Nội\",\"district\":\"Test\",\"ward\":\"Test\",\"ghnWardCode\":\"00001\",\"ghnDistrictId\":1442,\"isDefault\":false,\"userId\":\""+other.getId()+"\"}";
        mvc.perform(post("/api/account/addresses").header("Authorization",bearer).session(httpSession).header("X-CSRF-TOKEN",csrf)
                .contentType("application/json").content(body)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.isDefault").value(true)).andExpect(jsonPath("$.user").doesNotExist());
        var owned=accounts.list(user.getId()); assertThat(owned).hasSize(1);
        assertThat(accounts.list(other.getId())).isEmpty();
        String id=owned.get(0).id();
        loadCsrf();
        mvc.perform(put("/api/account/addresses/"+id).header("Authorization",bearer).session(httpSession).header("X-CSRF-TOKEN",csrf)
                .contentType("application/json").content(body.replace("12 Test","25 Test")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.addressLine").value("25 Test"));
        loadCsrf();
        mvc.perform(put("/api/account/addresses/"+id+"/default").header("Authorization",bearer).session(httpSession).header("X-CSRF-TOKEN",csrf))
                .andExpect(status().isOk()).andExpect(jsonPath("$.isDefault").value(true));
        loadCsrf();
        mvc.perform(delete("/api/account/addresses/"+id).header("Authorization",bearer).session(httpSession).header("X-CSRF-TOKEN",csrf))
                .andExpect(status().isNoContent());
        assertThat(accounts.list(user.getId())).isEmpty();
    }
    @Test void createUpdateDefaultAndDeleteMaintainOneDefault() {
        var first=accounts.save(user.getId(),null,input(false));
        assertThat(first.isDefault()).isTrue();
        var second=accounts.save(user.getId(),null,input(true));
        assertThat(accounts.list(user.getId()).stream().filter(AddressResponse::isDefault)).hasSize(1);
        accounts.setDefault(user.getId(),first.id());
        var changed=accounts.save(user.getId(),second.id(),new AddressRequest("25 Test","Hà Nội","Test","Test","00001",1442,false));
        assertThat(changed.addressLine()).isEqualTo("25 Test");
        accounts.delete(user.getId(),first.id());
        assertThat(accounts.list(user.getId())).singleElement().satisfies(a -> assertThat(a.isDefault()).isTrue());
        accounts.delete(user.getId(),second.id()); assertThat(accounts.list(user.getId())).isEmpty();
    }
    @Test void otherOwnersAndShopAddressesCannotBeAccessed() {
        var foreign=accounts.save(other.getId(),null,input(false));
        expect404(()->accounts.save(user.getId(),foreign.id(),input(true)));
        expect404(()->accounts.setDefault(user.getId(),foreign.id()));
        expect404(()->accounts.delete(user.getId(),foreign.id()));
        assertThat(accounts.list(user.getId())).isEmpty();
        jdbc.update("INSERT INTO Shops(id,owner_id,name,created_date,last_modify_date) VALUES ('shop-test',?, 'Test Shop',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)",user.getId());
        jdbc.update("INSERT INTO Addresses(id,user_id,shop_id,address_line,city,is_default,created_date,last_modify_date) VALUES ('shop-address',?,'shop-test','12 Test','Hà Nội',false,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)",user.getId());
        expect404(()->accounts.delete(user.getId(),"shop-address"));
        assertThat(accounts.list(user.getId())).isEmpty();
    }
    @Test void orderAddressCannotBeEditedOrDeleted() {
        var a=accounts.save(user.getId(),null,input(false));
        jdbc.update("INSERT INTO Shops(id,owner_id,name,created_date,last_modify_date) VALUES ('shop-test',?, 'Test Shop',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)",user.getId());
        jdbc.update("INSERT INTO Orders(id,customer_id,shop_id,delivery_address_id,sub_total,total_amount,created_date,last_modify_date) VALUES ('order-test',?,'shop-test',?,100,100,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)",user.getId(),a.id());
        assertThatThrownBy(()->accounts.delete(user.getId(),a.id())).isInstanceOfSatisfying(ResponseStatusException.class,e->assertThat(e.getStatusCode().value()).isEqualTo(409));
        assertThatThrownBy(()->accounts.save(user.getId(),a.id(),input(false))).isInstanceOfSatisfying(ResponseStatusException.class,e->assertThat(e.getStatusCode().value()).isEqualTo(409));
        assertThat(addresses.existsById(a.id())).isTrue();
    }
    @Test void personalAddressDoesNotRequireDistrict() throws Exception {
        mvc.perform(post("/api/account/addresses").header("Authorization",bearer).session(httpSession).header("X-CSRF-TOKEN",csrf)
                .contentType("application/json").content("{\"addressLine\":\"12 Test\",\"city\":\"Test Province\",\"ward\":\"Test Ward\",\"isDefault\":false}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.district").value(""));
        assertThat(accounts.list(user.getId())).singleElement().satisfies(a -> {
            assertThat(a.city()).isEqualTo("Test Province");
            assertThat(a.ward()).isEqualTo("Test Ward");
        });
    }
    @Test void addressLimitIsEnforced() {
        for(int i=0;i<10;i++) accounts.save(user.getId(),null,input(false));
        assertThatThrownBy(()->accounts.save(user.getId(),null,input(false))).isInstanceOf(IllegalArgumentException.class);
        assertThat(accounts.list(user.getId())).hasSize(10);
    }
    @Test void concurrentCreatesStillHaveExactlyOneDefault() throws Exception {
        var pool=Executors.newFixedThreadPool(2);
        try {
            var start=new CountDownLatch(1);
            Callable<AddressResponse> work=()-> { start.await(); return accounts.save(user.getId(),null,input(true)); };
            var first=pool.submit(work); var second=pool.submit(work); start.countDown();
            first.get(10,TimeUnit.SECONDS); second.get(10,TimeUnit.SECONDS);
            assertThat(accounts.list(user.getId())).hasSize(2);
            assertThat(accounts.list(user.getId()).stream().filter(AddressResponse::isDefault)).hasSize(1);
        } finally { pool.shutdownNow(); }
    }
}
