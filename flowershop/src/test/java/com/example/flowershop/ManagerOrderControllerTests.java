package com.example.flowershop;

import com.example.flowershop.controller.ManagerOrderController;
import com.example.flowershop.dto.auth.CurrentUser;
import com.example.flowershop.service.ManagerOrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ManagerOrderControllerTests {
    private final ManagerOrderService service = mock(ManagerOrderService.class);
    private MockMvc mvc;
    private final String base = "/api/shop/mine/shop-123/orders";

    @BeforeEach void setup() {
        // Supply the authenticated principal while testing MVC argument binding.
        mvc = MockMvcBuilders.standaloneSetup(new ManagerOrderController(service))
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    @Override public boolean supportsParameter(MethodParameter parameter) {
                        return parameter.getParameterType() == CurrentUser.class;
                    }
                    @Override public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer container,
                            NativeWebRequest request, WebDataBinderFactory binder) {
                        return new CurrentUser("owner-123", "owner@example.test", "Shop owner", "SHOP");
                    }
                }).build();
    }

    @Test void queueBindsShopAndDefaults() throws Exception {
        when(service.queue("shop-123", "owner-123", 0, null))
                .thenReturn(new ManagerOrderService.Results(List.of(), 0, 0, 0));
        mvc.perform(get(base + "/queue"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content").isEmpty());
        verify(service).queue("shop-123", "owner-123", 0, null);
    }

    @Test void queueBindsPageAndStatusFilter() throws Exception {
        when(service.queue("shop-123", "owner-123", 2, "PENDING"))
                .thenReturn(new ManagerOrderService.Results(List.of(), 2, 0, 0));
        mvc.perform(get(base + "/queue").param("page", "2").param("status", "PENDING"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.page").value(2));
        verify(service).queue("shop-123", "owner-123", 2, "PENDING");
    }

    @Test void queueCountAndOrderListBindShop() throws Exception {
        when(service.queueCount("shop-123", "owner-123"))
                .thenReturn(new ManagerOrderService.QueueCount(3));
        when(service.list("shop-123", "owner-123", 0, "PROCESSING"))
                .thenReturn(new ManagerOrderService.Results(List.of(), 0, 0, 0));
        mvc.perform(get(base + "/queue/count"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.pendingCount").value(3));
        mvc.perform(get(base).param("status", "PROCESSING"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.page").value(0));
        verify(service).list("shop-123", "owner-123", 0, "PROCESSING");
    }

    @Test void detailBindsBothPathVariables() throws Exception {
        mvc.perform(get(base + "/order-456")).andExpect(status().isOk());
        verify(service).detail("shop-123", "owner-123", "order-456");
    }
}
