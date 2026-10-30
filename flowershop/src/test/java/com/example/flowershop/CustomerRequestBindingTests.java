package com.example.flowershop;

import com.example.flowershop.controller.CustomerOrderController;
import com.example.flowershop.controller.FavoriteController;
import com.example.flowershop.controller.PublicCatalogController;
import com.example.flowershop.dto.auth.CurrentUser;
import com.example.flowershop.service.CatalogService;
import com.example.flowershop.service.CustomerOrderService;
import com.example.flowershop.service.FavoriteService;
import com.example.flowershop.service.ProductReviewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CustomerRequestBindingTests {
    private final FavoriteService favorites = mock(FavoriteService.class);
    private final CustomerOrderService orders = mock(CustomerOrderService.class);
    private final CatalogService catalog = mock(CatalogService.class);
    private final ProductReviewService reviews = mock(ProductReviewService.class);
    private MockMvc mvc;

    @BeforeEach void setup() {
        mvc = MockMvcBuilders.standaloneSetup(new FavoriteController(favorites),
                        new CustomerOrderController(orders), new PublicCatalogController(catalog, reviews))
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    @Override public boolean supportsParameter(MethodParameter parameter) {
                        return parameter.getParameterType() == CurrentUser.class;
                    }
                    @Override public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer container,
                            NativeWebRequest request, WebDataBinderFactory binder) {
                        return new CurrentUser("customer-123", "customer@example.test", "Customer", "CUSTOMER");
                    }
                }).build();
    }

    @Test void favoritesLoadAfterLoginWithDefaultAndExplicitPage() throws Exception {
        when(favorites.mine("customer-123", 0)).thenReturn(new CatalogService.Results(List.of(), 0, 12, 0, 0));
        mvc.perform(get("/api/customer/favorites"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content").isEmpty());
        mvc.perform(get("/api/customer/favorites").param("page", "0"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.page").value(0));
        verify(favorites, times(2)).mine("customer-123", 0);
    }

    @Test void customerOrdersBindPageAndStatus() throws Exception {
        when(orders.list("customer-123", 1, "PENDING"))
                .thenReturn(new CustomerOrderService.Results(List.of(), 1, 0, 0));
        mvc.perform(get("/api/customer/orders").param("page", "1").param("status", "PENDING"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.page").value(1));
        verify(orders).list("customer-123", 1, "PENDING");
    }

    @Test void catalogLoadsWithDefaultPagination() throws Exception {
        when(catalog.browse("", null, null, null, null, null, "newest", 0, 12))
                .thenReturn(new CatalogService.Results(List.of(), 0, 12, 0, 0));
        mvc.perform(get("/api/public/products"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.size").value(12));
        verify(catalog).browse("", null, null, null, null, null, "newest", 0, 12);
    }

    @Test void orderDetailUsesRequestedIdAndInvalidPageIsRejected() throws Exception {
        mvc.perform(get("/api/customer/orders/order-456")).andExpect(status().isOk());
        verify(orders).detail("customer-123", "order-456");
        mvc.perform(get("/api/customer/favorites").param("page", "invalid"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(favorites);
    }
}
