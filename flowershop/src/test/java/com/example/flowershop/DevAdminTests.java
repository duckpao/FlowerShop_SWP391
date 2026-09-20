package com.example.flowershop;

import com.example.flowershop.entity.User;
import com.example.flowershop.entity.enums.*;
import com.example.flowershop.repository.*;
import com.example.flowershop.security.DevAdminFilter;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockHttpSession;
import java.util.UUID;
import java.util.regex.Pattern;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:devadmin;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa","spring.datasource.password=","spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop","debug=false"})
@ActiveProfiles("dev-admin")
@AutoConfigureMockMvc
class DevAdminTests {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @MockitoBean JavaMailSender mail;
    User customer;
    @BeforeEach void setup() {
        users.deleteAll();
        customer=users.saveAndFlush(User.builder().id(UUID.randomUUID().toString()).email("customer@test.example")
                .fullName("Test Customer").role(UserRole.CUSTOMER).status(UserStatus.ACTIVE).isEmailVerified(true).build());
    }
    @Test void loopbackCanListAndReadWithoutToken() throws Exception {
        mvc.perform(get("/api/admin/customers")).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/admin/customers/"+customer.getId())).andExpect(status().isOk());
    }
    @Test void externalRequestsAndOtherEndpointsDoNotGainAdmin() throws Exception {
        mvc.perform(get("/api/admin/customers").with(r->{r.setRemoteAddr("192.0.2.10");return r;})).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/shops")).andExpect(status().isOk());
        mvc.perform(get("/api/admin/settings")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/account/profile")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/customers").header("Authorization","Bearer invalid")).andExpect(status().isUnauthorized());
    }
    @Test void writesStillRequireCsrfAndUseExplicitTestActor() throws Exception {
        String path="/api/admin/customers/"+customer.getId()+"/blocked";
        mvc.perform(put(path).contentType("application/json").content("{\"blocked\":true}")).andExpect(status().isForbidden());
        assertThat(users.findById(customer.getId()).orElseThrow().getStatus()).isEqualTo(UserStatus.ACTIVE);
        var csrf=mvc.perform(get("/api/auth/csrf")).andReturn();
        var session=(MockHttpSession)csrf.getRequest().getSession(false);
        var match=Pattern.compile("\"token\":\"([^\"]+)\"").matcher(csrf.getResponse().getContentAsString());
        assertThat(match.find()).isTrue();
        mvc.perform(put(path).session(session).header("X-CSRF-TOKEN",match.group(1)).contentType("application/json")
                .content("{\"blocked\":true}")).andExpect(status().isOk());
        var updated=users.findById(customer.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(UserStatus.BANNED);
        assertThat(updated.getLastModifyBy()).isEqualTo(DevAdminFilter.ACTOR_ID);
        mvc.perform(get("/api/account/profile").session(session)).andExpect(status().isUnauthorized());
    }
    @Test void shopActionsAreScopedAndRequireCsrf() throws Exception {
        mvc.perform(get("/api/admin/shops").with(r->{r.setRemoteAddr("192.0.2.10");return r;})).andExpect(status().isUnauthorized());
        for(String action:java.util.List.of("approve","block","unblock")) {
            String path="/api/admin/shops/missing/"+action;
            mvc.perform(put(path)).andExpect(status().isForbidden());
            var csrf=mvc.perform(get("/api/auth/csrf")).andReturn();
            var session=(MockHttpSession)csrf.getRequest().getSession(false);
            var match=Pattern.compile("\"token\":\"([^\"]+)\"").matcher(csrf.getResponse().getContentAsString());
            assertThat(match.find()).isTrue();
            mvc.perform(put(path).session(session).header("X-CSRF-TOKEN",match.group(1)))
                    .andExpect(status().isNotFound()).andExpect(jsonPath("$.message").value("Không tìm thấy cửa hàng."));
        }
    }
}
