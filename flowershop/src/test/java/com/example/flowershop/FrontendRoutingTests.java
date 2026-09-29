package com.example.flowershop;

import org.junit.jupiter.api.Test;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class FrontendRoutingTests extends ManagerShopTests {
    @Test void reactRoutesForwardToIndexWithoutOpeningProtectedApis() throws Exception {
        for (String path : java.util.List.of("/", "/login", "/account", "/admin", "/admin/approvals", "/shop-admin", "/shop-admin/staff", "/shops/test-shop",
                "/products", "/products/test-product", "/favorites", "/admin/categories")) {
            mvc.perform(get(path)).andExpect(status().isOk()).andExpect(forwardedUrl("/index.html"));
        }
        mvc.perform(get("/index.html")).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"root\"")));
        mvc.perform(get("/api/admin/shops")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/shop/mine")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/account/profile")).andExpect(status().isUnauthorized());
        mvc.perform(get("/assets/missing-file.js")).andExpect(status().isNotFound());
    }
}
