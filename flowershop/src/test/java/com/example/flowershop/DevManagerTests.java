package com.example.flowershop;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.mock.web.MockHttpSession;
import java.util.regex.Pattern;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("dev-manager")
@TestPropertySource(properties={"spring.datasource.url=jdbc:h2:mem:devmanager;MODE=MySQL;DB_CLOSE_DELAY=-1","app.dev-manager.email=shop1@gmail.com"})
class DevManagerTests extends ManagerShopTests {
    @Test void localAccessUsesConfiguredOwnerButStillRequiresCsrf() throws Exception {
        owner.setEmail("shop1@gmail.com"); owner.setIsEmailVerified(false); users.saveAndFlush(owner);
        mvc.perform(get("/api/shop/mine")).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(shop.getId()));
        String path="/api/shop/mine/"+shop.getId();
        String body="{\"name\":\"Local edit\",\"description\":\"Test\",\"logoUrl\":\"\"}";
        mvc.perform(put(path).contentType("application/json").content(body)).andExpect(status().isForbidden());
        var result=mvc.perform(get("/api/auth/csrf")).andReturn();
        var session=(MockHttpSession)result.getRequest().getSession(false);
        var match=Pattern.compile("\"token\":\"([^\"]+)\"").matcher(result.getResponse().getContentAsString());
        assertThat(match.find()).isTrue();
        mvc.perform(put(path).session(session).header("X-CSRF-TOKEN",match.group(1)).contentType("application/json").content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Local edit"));
        assertThat(users.findById(owner.getId()).orElseThrow().getIsEmailVerified()).isFalse();
        mvc.perform(get("/api/account/profile").session(session)).andExpect(status().isUnauthorized());
    }
    @Test void noRemoteOrAdminAccessAndMissingOwnerFailsClearly() throws Exception {
        mvc.perform(get("/api/shop/mine").with(r->{r.setRemoteAddr("192.0.2.2");return r;})).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/customers")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/shop/mine").header("Authorization","Bearer invalid")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/shop/mine")).andExpect(status().isServiceUnavailable());
        owner.setEmail("shop1@gmail.com"); users.saveAndFlush(owner);
        shop.setOwner(other); shops.saveAndFlush(shop);
        mvc.perform(get("/api/shop/mine/"+shop.getId()+"/staff")).andExpect(status().isNotFound());
    }
    // Normal anonymous request assertion differs in this profile: owner is absent by default.
    @Override @Test void endpointRolesAndCsrf() throws Exception {
        owner.setEmail("shop1@gmail.com"); users.saveAndFlush(owner);
        mvc.perform(get("/api/shop/mine")).andExpect(status().isOk());
        mvc.perform(put("/api/shop/mine/"+shop.getId()).contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
    }
}
