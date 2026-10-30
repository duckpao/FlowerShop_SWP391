package com.example.flowershop;

import com.example.flowershop.controller.AdminShopController;
import com.example.flowershop.controller.CartController;
import com.example.flowershop.controller.ManagerProductController;
import com.example.flowershop.dto.auth.CurrentUser;
import com.example.flowershop.service.AdminShopService;
import com.example.flowershop.service.CartService;
import com.example.flowershop.service.ManagerProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Exercises argument binding; authentication and authorization are tested separately. */
class AllRolesRequestBindingTests {
    private final AdminShopService shops = mock(AdminShopService.class);
    private final CartService carts = mock(CartService.class);
    private final ManagerProductService products = mock(ManagerProductService.class);
    private MockMvc mvc;

    @BeforeEach void setup() {
        mvc = MockMvcBuilders.standaloneSetup(new AdminShopController(shops), new CartController(carts),
                        new ManagerProductController(products))
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    @Override public boolean supportsParameter(MethodParameter parameter) {
                        return parameter.getParameterType() == CurrentUser.class;
                    }
                    @Override public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer container,
                            NativeWebRequest request, WebDataBinderFactory binder) {
                        return new CurrentUser("actor-123", "actor@example.test", "Actor", "SHOP");
                    }
                }).build();
    }

    @Test void adminShopSearchBindsDefaultsAndFilters() throws Exception {
        mvc.perform(get("/api/admin/shops")).andExpect(status().isOk());
        verify(shops).list("", "", 0, 10);
        mvc.perform(get("/api/admin/shops").param("q", "Rose").param("status", "ACTIVE")
                        .param("page", "2").param("size", "5"))
                .andExpect(status().isOk());
        verify(shops).list("Rose", "ACTIVE", 2, 5);
    }

    @Test void adminShopTransitionBindsActionAndId() throws Exception {
        mvc.perform(put("/api/admin/shops/shop-456/approve")).andExpect(status().isOk());
        verify(shops).transition("shop-456", "approve", "actor-123");
    }

    @Test void cartBindsOwnerAndItemId() throws Exception {
        mvc.perform(get("/api/cart/actor-123")).andExpect(status().isOk());
        verify(carts).getCart("actor-123");
        mvc.perform(delete("/api/cart/actor-123/items/item-456")).andExpect(status().isNoContent());
        verify(carts).removeCartItem("actor-123", "item-456");
    }

    @Test void managerProductsBindPaginationAndNestedMediaIds() throws Exception {
        mvc.perform(get("/api/shop/mine/shop-456/products").param("page", "3"))
                .andExpect(status().isOk());
        verify(products).list("shop-456", "actor-123", 3);
        mvc.perform(delete("/api/shop/mine/shop-456/products/product-789/images/image-012"))
                .andExpect(status().isNoContent());
        verify(products).deleteImage("shop-456", "product-789", "image-012", "actor-123");
    }

    @Test void videoUploadBindsFileAndOptionalText() throws Exception {
        var file = new MockMultipartFile("file", "sample.mp4", "video/mp4", new byte[]{1, 2, 3});
        when(products.uploadVideo(eq("shop-456"), eq("product-789"), eq("actor-123"),
                any(MultipartFile.class), eq("Bouquet"), eq("Preview"))).thenReturn(List.of());
        mvc.perform(multipart("/api/shop/mine/shop-456/products/product-789/videos").file(file)
                        .param("title", "Bouquet").param("description", "Preview"))
                .andExpect(status().isCreated()).andExpect(content().json("[]"));
        verify(products).uploadVideo(eq("shop-456"), eq("product-789"), eq("actor-123"),
                any(MultipartFile.class), eq("Bouquet"), eq("Preview"));
    }
}
