package com.example.flowershop;

import com.example.flowershop.entity.*;
import com.example.flowershop.entity.enums.*;
import com.example.flowershop.repository.*;
import com.example.flowershop.service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:customworkflow;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa", "spring.datasource.password=", "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop", "app.shop-follow-mail.enabled=false", "spring.jpa.show-sql=false"})
@Transactional
@AutoConfigureMockMvc
class CustomOrderWorkflowTests {
    @MockitoBean JavaMailSender mail;
    @MockitoBean GhnService ghn;
    @MockitoBean TokenAuthService tokenAuth;
    @Autowired MockMvc mvc;
    @Autowired CustomOrderWorkflowService workflow;
    @Autowired ManagerOrderService manager;
    @Autowired CustomerOrderService customerOrders;
    @Autowired PaymentService paymentService;
    @Autowired OrderService orderService;
    @Autowired UserRepository users;
    @Autowired ShopRepository shops;
    @Autowired AddressRepository addresses;
    @Autowired OrderRepository orders;
    @Autowired OrderDetailRepository details;
    @Autowired PaymentRepository payments;
    @Autowired ProductRepository products;
    @Autowired CategoryRepository categories;
    User customer, owner;
    Shop shop;
    Address destination;
    Product reference;
    String id() { return UUID.randomUUID().toString(); }
    User user(UserRole role) { return users.saveAndFlush(User.builder().id(id()).email(id()+"@example.test")
            .fullName("Test User").phone("0901234567").isEmailVerified(true).role(role).status(UserStatus.ACTIVE).build()); }
    @BeforeEach void setup() {
        customer=user(UserRole.CUSTOMER); owner=user(UserRole.SHOP);
        shop=shops.saveAndFlush(Shop.builder().id(id()).owner(owner).name("Design Shop").status(ShopStatus.ACTIVE).build());
        destination=addresses.saveAndFlush(Address.builder().id(id()).user(customer).addressLine("Customer street")
                .recipientName("Recipient").recipientPhone("0901234567").ghnDistrictId(2).ghnWardCode("002").build());
        addresses.saveAndFlush(Address.builder().id(id()).shop(shop).addressLine("Shop street").isDefault(true)
                .ghnDistrictId(1).ghnWardCode("001").build());
        var category=categories.saveAndFlush(Category.builder().id(id()).name("Flowers").status(CategoryStatus.ACTIVE).build());
        reference=products.saveAndFlush(Product.builder().id(id()).name("Reference rose").shop(shop).category(category)
                .price(new BigDecimal("100000")).stock(10).build());
        when(ghn.calculateFee(anyInt(), anyString(), anyInt(), anyString(), anyInt())).thenReturn(new BigDecimal("30000"));
        when(tokenAuth.authenticate("customer-test")).thenReturn(com.example.flowershop.dto.auth.CurrentUser.from(customer));
        when(tokenAuth.authenticate("shop-test")).thenReturn(com.example.flowershop.dto.auth.CurrentUser.from(owner));
    }
    CustomOrderWorkflowService.CreateInput input() {
        var design=new CustomFlowerOrderService.RequestInput(shop.getId(), new BigDecimal("500000"), 3, "",
                new CustomFlowerOrderService.Preferences("Rose", "Pink", "Birthday", "Paper, Ribbon", null, reference.getId()));
        return new CustomOrderWorkflowService.CreateInput(destination.getId(), design);
    }
    CustomOrderWorkflowService.Receipt create() { return workflow.create(customer.getId(), input()); }
    void quote(String id) { workflow.quote(shop.getId(), owner.getId(), id, new CustomOrderWorkflowService.Quote(new BigDecimal("350000"), "Pink bouquet")); }

    @Test void requestAppearsInNormalShopAndCustomerOrdersWithDesign() {
        var created=create();
        assertThat(created.status()).isEqualTo("AWAITING_QUOTE");
        assertThat(manager.list(shop.getId(), owner.getId(), 0, null).content()).extracting(ManagerOrderService.OrderSummary::id).contains(created.orderId());
        assertThat(manager.queueCount(shop.getId(), owner.getId()).pendingCount()).isEqualTo(1);
        assertThat(customerOrders.detail(customer.getId(), created.orderId()).customDesign().preferences().quantity()).isEqualTo(3);
        assertThat(reference.getStock()).isEqualTo(10);
        assertThat(payments.findByOrderId(created.orderId())).isEmpty();
    }
    @Test void quoteRequiresCustomerAgreementAndPreservesExactBatchPrice() {
        var id=create().orderId(); quote(id);
        var order=customerOrders.detail(customer.getId(), id);
        assertThat(order.status()).isEqualTo("QUOTED");
        assertThat(order.totalAmount()).isEqualByComparingTo("380000");
        assertThat(order.items().get(0).price()).isEqualByComparingTo("350000");
        assertThatThrownBy(() -> manager.ship(shop.getId(), owner.getId(), id)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> manager.confirm(shop.getId(), owner.getId(), id)).isInstanceOf(ResponseStatusException.class);
    }
    @Test void codUsesNormalShippingAndCompletionWorkflow() {
        var id=create().orderId(); quote(id);
        workflow.accept(customer.getId(), id, new CustomOrderWorkflowService.Accept(PaymentMethod.COD, new BigDecimal("380000")));
        assertThat(orders.findById(id).orElseThrow().getStatus()).isEqualTo(OrderStatus.PROCESSING);
        when(ghn.createOrder(anyString(), anyString(), any(), anyString(), anyString(), any(), anyInt(), anyString(), anyInt(), anyInt()))
                .thenReturn(new GhnService.CreateOrderResult("GHN-TEST", 30000));
        manager.ship(shop.getId(), owner.getId(), id);
        assertThat(customerOrders.detail(customer.getId(), id).status()).isEqualTo("DELIVERING");
        when(ghn.track("GHN-TEST")).thenReturn(new GhnService.Tracking("delivered", DeliveryStatus.DELIVERED));
        manager.refreshStatus(shop.getId(), owner.getId(), id);
        assertThat(customerOrders.detail(customer.getId(), id).status()).isEqualTo("COMPLETED");
        assertThat(payments.findByOrderId(id)).allMatch(p -> p.getStatus()==PaymentStatus.SUCCESS);
        assertThat(reference.getStock()).isEqualTo(10);
    }
    @Test void onlineWaitsForPaymentAndStartsTimeoutAtAcceptance() {
        var id=create().orderId(); quote(id);
        orders.findById(id).orElseThrow().setCreatedDate(LocalDateTime.now().minusDays(1));
        workflow.accept(customer.getId(), id, new CustomOrderWorkflowService.Accept(PaymentMethod.ONLINE, new BigDecimal("380000")));
        assertThat(paymentService.generatePaymentQr(id, customer.getId()).getAmount()).isEqualByComparingTo("380000");
        orderService.cancelExpiredOrder(id, "Expired");
        assertThat(orders.findById(id).orElseThrow().getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThatThrownBy(() -> manager.confirm(shop.getId(), owner.getId(), id)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> manager.ship(shop.getId(), owner.getId(), id)).isInstanceOf(ResponseStatusException.class);
        orders.findById(id).orElseThrow().setPaymentStartedAt(LocalDateTime.now().minusMinutes(11));
        orderService.cancelExpiredOrder(id, "Expired");
        assertThat(orders.findById(id).orElseThrow().getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(reference.getStock()).isEqualTo(10);
    }
    @Test void duplicateAcceptanceAndQuoteChangesAfterAcceptanceAreRejected() {
        var id=create().orderId(); quote(id);
        workflow.accept(customer.getId(), id, new CustomOrderWorkflowService.Accept(PaymentMethod.COD, new BigDecimal("380000")));
        assertThatThrownBy(() -> workflow.accept(customer.getId(), id, new CustomOrderWorkflowService.Accept(PaymentMethod.COD, new BigDecimal("380000"))))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> quote(id)).isInstanceOf(ResponseStatusException.class);
        assertThat(payments.findByOrderId(id)).hasSize(1);
    }
    @Test void rejectionIsVisibleInCommonOrderHistory() {
        var id=create().orderId();
        manager.cancel(shop.getId(), owner.getId(), id, "Cannot supply these flowers");
        var detail=customerOrders.detail(customer.getId(), id);
        assertThat(detail.status()).isEqualTo("CANCELLED");
        assertThat(detail.cancelReason()).isEqualTo("Cannot supply these flowers");
        assertThatThrownBy(() -> quote(id)).isInstanceOf(ResponseStatusException.class);
    }
    @Test void rejectsCrossAccountChangesAndOverBudgetQuotes() {
        var id=create().orderId();
        assertThatThrownBy(() -> workflow.quote(shop.getId(), "other-owner", id, new CustomOrderWorkflowService.Quote(new BigDecimal("100"), "")))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> workflow.quote(shop.getId(), owner.getId(), id, new CustomOrderWorkflowService.Quote(new BigDecimal("500001"), "")))
                .isInstanceOf(ResponseStatusException.class);
        quote(id);
        assertThatThrownBy(() -> workflow.accept("other-customer", id, new CustomOrderWorkflowService.Accept(PaymentMethod.COD, new BigDecimal("380000"))))
                .isInstanceOf(ResponseStatusException.class);
    }
    @Test void migratedRequestRequiresOwnedDeliveryAddressBeforeQuote() {
        var id=create().orderId(); var order=orders.findById(id).orElseThrow();
        order.setDeliveryAddress(null);
        assertThat(customerOrders.detail(customer.getId(), id).needsDeliveryAddress()).isTrue();
        assertThatThrownBy(() -> quote(id)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> workflow.deliveryAddress("other-customer", id, new CustomOrderWorkflowService.DeliveryInput(destination.getId())))
                .isInstanceOf(ResponseStatusException.class);
        workflow.deliveryAddress(customer.getId(), id, new CustomOrderWorkflowService.DeliveryInput(destination.getId()));
        quote(id);
        assertThat(orders.findById(id).orElseThrow().getStatus()).isEqualTo(OrderStatus.QUOTED);
    }
    @Test void customerAndShopCanUseCommonOrderApisWithSecurityAndValidation() throws Exception {
        var json=new com.fasterxml.jackson.databind.ObjectMapper();
        var csrfResult=mvc.perform(get("/api/auth/csrf")).andReturn();
        var csrf=json.readTree(csrfResult.getResponse().getContentAsString());
        var session=(MockHttpSession)csrfResult.getRequest().getSession(false);
        var created=mvc.perform(post("/api/customer/orders/custom").session(session)
                        .header(csrf.path("headerName").asText(), csrf.path("token").asText())
                        .header("Authorization", "Bearer customer-test").contentType("application/json")
                        .content(json.writeValueAsString(input())))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("AWAITING_QUOTE")).andReturn();
        var id=json.readTree(created.getResponse().getContentAsString()).path("orderId").asText();
        mvc.perform(get("/api/shop/mine/"+shop.getId()+"/orders").param("status", "AWAITING_QUOTE")
                        .header("Authorization", "Bearer shop-test"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].id").value(id));
        csrfResult=mvc.perform(get("/api/auth/csrf")).andReturn();
        csrf=json.readTree(csrfResult.getResponse().getContentAsString());
        session=(MockHttpSession)csrfResult.getRequest().getSession(false);
        mvc.perform(post("/api/shop/mine/"+shop.getId()+"/orders/"+id+"/quote").session(session)
                        .header(csrf.path("headerName").asText(), csrf.path("token").asText())
                        .header("Authorization", "Bearer shop-test").contentType("application/json")
                        .content("{\"amount\":350000,\"note\":\"Confirmed design\"}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/customer/orders/"+id).header("Authorization", "Bearer customer-test"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.customDesign.preferences.quantity").value(3))
                .andExpect(jsonPath("$.totalAmount").value(380000));
        csrfResult=mvc.perform(get("/api/auth/csrf")).andReturn();
        csrf=json.readTree(csrfResult.getResponse().getContentAsString());
        session=(MockHttpSession)csrfResult.getRequest().getSession(false);
        mvc.perform(post("/api/customer/orders/"+id+"/accept-quote").session(session)
                        .header(csrf.path("headerName").asText(), csrf.path("token").asText())
                        .header("Authorization", "Bearer customer-test").contentType("application/json")
                        .content("{\"paymentMethod\":\"COD\",\"expectedTotal\":380000}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PROCESSING"));
    }
}
