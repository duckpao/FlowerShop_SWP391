package com.example.flowershop;

import com.example.flowershop.dto.auth.CurrentUser;
import com.example.flowershop.entity.*;
import com.example.flowershop.entity.Order;
import com.example.flowershop.entity.enums.*;
import com.example.flowershop.repository.*;
import com.example.flowershop.service.TokenAuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import java.util.UUID;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:designchatapi;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa", "spring.datasource.password=", "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.open-in-view=false", "spring.jpa.show-sql=false",
        "app.shop-follow-mail.enabled=false", "app.occasion-reminder.enabled=false"})
@AutoConfigureMockMvc
class CustomDesignChatApiTests {
    @MockitoBean JavaMailSender mail;
    @MockitoBean TokenAuthService tokens;
    @Autowired MockMvc mvc;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired UserRepository users;
    @Autowired ShopRepository shops;
    @Autowired OrderRepository orders;
    @Autowired OrderDetailRepository details;
    @Autowired ChatSessionRepository sessions;
    User customer,owner;
    Shop shop;
    Order order;
    String sessionId;
    final ObjectMapper json=new ObjectMapper();
    String id() { return UUID.randomUUID().toString(); }
    User user(UserRole role) {
        return users.saveAndFlush(User.builder().id(id()).email(id()+"@example.test").fullName("Chat "+role)
                .role(role).status(UserStatus.ACTIVE).isEmailVerified(true).build());
    }
    @BeforeEach void fixture() {
        new TransactionTemplate(transactionManager).executeWithoutResult(tx -> {
            customer=user(UserRole.CUSTOMER); owner=user(UserRole.SHOP);
            shop=shops.saveAndFlush(Shop.builder().id(id()).owner(owner).name("Chat design shop").status(ShopStatus.ACTIVE).build());
            order=orders.saveAndFlush(Order.builder().id(id()).customer(customer).shop(shop).orderType(OrderType.CUSTOM)
                    .status(OrderStatus.AWAITING_QUOTE).subTotal(BigDecimal.ZERO).totalAmount(BigDecimal.ZERO).build());
            details.saveAndFlush(OrderDetail.builder().id(id()).order(order).price(BigDecimal.ZERO).quantity(1)
                    .customMaterials("{\"budget\":500000,\"description\":\"Design\",\"preferences\":{\"quantity\":2,\"deliveryDate\":\"2027-02-28\"}}").build());
            sessionId=sessions.saveAndFlush(ChatSession.builder().id(id()).customer(customer).shop(shop).build()).getId();
        });
        when(tokens.authenticate("customer-test")).thenReturn(CurrentUser.from(customer));
        when(tokens.authenticate("shop-test")).thenReturn(CurrentUser.from(owner));
    }
    MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder request,String role) {
        return request.header("Authorization","Bearer "+role+"-test");
    }
    MockHttpServletRequestBuilder write(MockHttpServletRequestBuilder request,String role) throws Exception {
        var result=mvc.perform(get("/api/auth/csrf")).andReturn();
        var csrf=json.readTree(result.getResponse().getContentAsString());
        return auth(request.session((MockHttpSession)result.getRequest().getSession(false))
                .header(csrf.path("headerName").asText(),csrf.path("token").asText()).contentType("application/json"),role);
    }
    @Test void customerCanGoFromCommittedOrderDetailsToExistingChatAndSendDesign() throws Exception {
        mvc.perform(auth(get("/api/customer/orders/"+order.getId()),"customer"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.customDesign.preferences.deliveryDate").value("2027-02-28"));
        mvc.perform(auth(get("/api/customer/chat/sessions"),"customer")).andExpect(status().isOk());
        mvc.perform(write(post("/api/customer/chat/sessions"),"customer").content("{\"shopId\":\""+shop.getId()+"\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(sessionId));
        mvc.perform(write(post("/api/customer/chat/sessions/"+sessionId+"/read"),"customer"))
                .andExpect(status().isNoContent());
        sendDesign("/api/customer/chat/sessions/"+sessionId+"/messages","customer");
        mvc.perform(auth(get("/api/customer/chat/sessions/"+sessionId+"/messages"),"customer"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].context.orderId").value(order.getId()));
    }
    @Test void shopCanGoFromCommittedOrderDetailsToExistingChatAndSendDesign() throws Exception {
        String root="/api/shop/mine/"+shop.getId();
        mvc.perform(auth(get(root+"/orders/"+order.getId()),"shop")).andExpect(status().isOk());
        mvc.perform(auth(get(root+"/chat/sessions"),"shop")).andExpect(status().isOk());
        mvc.perform(write(post(root+"/chat/sessions"),"shop").content("{\"customOrderId\":\""+order.getId()+"\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(sessionId));
        mvc.perform(write(post(root+"/chat/sessions/"+sessionId+"/read"),"shop")).andExpect(status().isNoContent());
        sendDesign(root+"/chat/sessions/"+sessionId+"/messages","shop");
        mvc.perform(auth(get(root+"/chat/sessions/"+sessionId+"/messages"),"shop"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].context.orderId").value(order.getId()));
    }
    void sendDesign(String endpoint,String role) throws Exception {
        mvc.perform(write(post(endpoint),role).content("{\"message\":\"Discuss design\",\"context\":{\"type\":\"CUSTOM_ORDER\",\"id\":\""+order.getId()+"\"}}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.context.orderId").value(order.getId()));
    }
}
