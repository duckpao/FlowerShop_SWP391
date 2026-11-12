package com.example.flowershop;
import com.example.flowershop.dto.auth.CurrentUser;
import com.example.flowershop.entity.*;
import com.example.flowershop.entity.enums.*;
import com.example.flowershop.repository.*;
import com.example.flowershop.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:subscriptions;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa","spring.datasource.password=","spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop","spring.jpa.open-in-view=false","spring.jpa.show-sql=false",
        "app.shop-subscription.enabled=false","app.shop-follow-mail.enabled=false","app.occasion-reminder.enabled=false"})
@AutoConfigureMockMvc
class ShopSubscriptionTests {
    @MockitoBean SePayGatewayService gateway;
    @MockitoBean HtmlMailSender mail;
    @MockitoBean TokenAuthService tokens;
    @MockitoBean ShopPlanClock clock;
    @Autowired ShopPlanService service;
    @Autowired ManagerProductService manager;
    @Autowired SubscriptionReminderService reminders;
    @Autowired ShopPlanRepository plans;
    @Autowired ShopSubscriptionRepository subscriptions;
    @Autowired ShopPlanPaymentRepository payments;
    @Autowired ShopProductPostingRepository postings;
    @Autowired FeaturedProductRepository featured;
    @Autowired SubscriptionReminderRepository reminderRows;
    @Autowired NotificationService notifications;
    @Autowired ShopProActivationRepository activations;
    @Autowired ShopRepository shops;
    @Autowired UserRepository users;
    @Autowired ProductRepository products;
    @Autowired CategoryRepository categories;
    @Autowired ProductQuotaService quota;
    @Autowired CatalogService catalog;
    @Autowired CartService cartService;
    @Autowired CustomerOrderService customerOrders;
    @Autowired OrderService cartOrders;
    @Autowired CartItemRepository cartItems;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired MockMvc mvc;
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
    TransactionTemplate tx;
    User owner,customer,admin;
    Shop shop;
    Category category;
    LocalDateTime now;
    String id(){return UUID.randomUUID().toString();}
    @BeforeEach void setup() {
        now=LocalDateTime.of(2026,1,31,9,0); tx=new TransactionTemplate(transactionManager);
        when(clock.now()).thenAnswer(call->now);
        when(gateway.subscriptionCheckout(anyString(),any(),anyString(),anyString()))
                .thenReturn(new SePayGatewayService.SubscriptionCheckout("https://gateway.example.test",Map.of("test","test")));
        when(gateway.capturedSubscriptionInvoice(anyString())).thenReturn(Optional.empty());
        when(mail.send(anyString(),anyString(),anyString())).thenReturn(true);
        when(mail.url(anyString())).thenAnswer(call->"http://localhost"+call.getArgument(0));
        tx.executeWithoutResult(t->{
            int[] months={0,1,3,6,12}; int[] prices={0,59000,149000,298000,596000}; String[] codes={"FREE","PRO1","PRO3","PRO6","PRO12"};
            Integer[] limits={5,15,25,40,null};int[] featuredLimits={0,3,5,8,15};
            for(int i=0;i<months.length;i++) plans.saveAndFlush(ShopPlan.builder().id(codes[i]).name(codes[i]).durationMonths(months[i])
                    .price(BigDecimal.valueOf(prices[i])).productLimit(limits[i]).featuredLimit(featuredLimits[i]).enabled(true).build());
            owner=users.saveAndFlush(User.builder().id(id()).email(id()+"@test.example").fullName("Owner").role(UserRole.SHOP).status(UserStatus.ACTIVE).isEmailVerified(true).build());
            admin=users.saveAndFlush(User.builder().id(id()).email(id()+"@test.example").fullName("Admin").role(UserRole.ADMIN).status(UserStatus.ACTIVE).isEmailVerified(true).build());
            customer=users.saveAndFlush(User.builder().id(id()).email(id()+"@test.example").role(UserRole.CUSTOMER).status(UserStatus.ACTIVE).isEmailVerified(true).build());
            shop=shops.saveAndFlush(Shop.builder().id(id()).owner(owner).name("Pro shop").status(ShopStatus.ACTIVE).build());
            category=categories.saveAndFlush(Category.builder().id(id()).name("Flowers").status(CategoryStatus.ACTIVE).build());
        });
        when(tokens.authenticate("shop-test")).thenReturn(CurrentUser.from(owner));
        when(tokens.authenticate("customer-test")).thenReturn(CurrentUser.from(customer));
        when(tokens.authenticate("admin-test")).thenReturn(CurrentUser.from(admin));
    }
    ManagerProductService.Input input(){return new ManagerProductService.Input("Rose","Fresh",category.getId(),new BigDecimal("100000"),5,ProductStatus.ACTIVE,ProductType.READY_MADE,List.of());}
    String create(){return manager.save(shop.getId(),null,owner.getId(),input()).id();}
    String buy(String plan){return service.checkout(shop.getId(),owner.getId(),plan).invoiceNumber();}
    void capture(String invoice){
        var amount=tx.execute(t->payments.invoice(invoice).orElseThrow().getAmount());
        when(gateway.capturedSubscriptionInvoice(invoice)).thenReturn(Optional.of(new SePayGatewayService.CapturedInvoice(invoice,amount,"TX-"+invoice)));
        service.reconcile(invoice);
    }
    @Test void proCheckoutReturnsToFrontendSubscriptionPage() {
        var invoice=buy("PRO1");
        verify(gateway).subscriptionCheckout(eq(invoice),any(BigDecimal.class),eq(owner.getId()),
                eq("http://localhost:5173/shop-admin/subscription"));
    }
    @Test void freeQuotaCountsExistingProductsIncludingHiddenAndDoesNotResetMonthly() {
        String product=null; for(int i=0;i<5;i++)product=create();
        assertThat(service.status(shop.getId(),owner.getId()).productCount()).isEqualTo(5);
        assertThatThrownBy(this::create).isInstanceOf(ResponseStatusException.class).hasMessageContaining("5");
        manager.save(shop.getId(),product,owner.getId(),input());
        manager.hide(shop.getId(),product,owner.getId());
        assertThatThrownBy(this::create).isInstanceOf(ResponseStatusException.class);
        now=now.plusMonths(1).withDayOfMonth(1);
        assertThatThrownBy(this::create).isInstanceOf(ResponseStatusException.class);
    }
    @Test void removalFreesQuotaButHidingDoesNotAndDeletedProductsCannotBeRestored() {
        var ids=new ArrayList<String>();for(int i=0;i<5;i++)ids.add(create());
        manager.hide(shop.getId(),ids.get(0),owner.getId());
        assertThatThrownBy(this::create).isInstanceOf(ResponseStatusException.class);
        manager.remove(shop.getId(),ids.get(0),owner.getId());
        assertThat(service.status(shop.getId(),owner.getId()).productCount()).isEqualTo(4);
        assertThat(tx.<Boolean>execute(t->products.findById(ids.get(0)).orElseThrow().isDeleted())).isTrue();
        assertThatThrownBy(()->manager.unhide(shop.getId(),ids.get(0),owner.getId())).isInstanceOf(ResponseStatusException.class);
        assertThat(create()).isNotBlank();
        assertThat(service.status(shop.getId(),owner.getId()).productCount()).isEqualTo(5);
    }
    @Test void eachPlanEnforcesItsOwnProductAndFeaturedLimits() {
        String[] codes={"PRO1","PRO3","PRO6","PRO12"};Integer[] limits={15,25,40,null};int[] slots={3,5,8,15};
        var ids=new ArrayList<String>();
        for(int i=0;i<codes.length;i++) {
            capture(buy(codes[i]));
            int target=limits[i]==null?45:limits[i];
            while(ids.size()<target)ids.add(create());
            if(limits[i]!=null)assertThatThrownBy(this::create).isInstanceOf(ResponseStatusException.class);
            assertThat(service.status(shop.getId(),owner.getId()).productLimit()).isEqualTo(limits[i]);
            assertThat(service.status(shop.getId(),owner.getId()).featuredLimit()).isEqualTo(slots[i]);
            for(int j=0;j<slots[i];j++)service.feature(shop.getId(),owner.getId(),ids.get(j),true);
            String extra=ids.get(slots[i]);
            assertThatThrownBy(()->service.feature(shop.getId(),owner.getId(),extra,true)).isInstanceOf(ResponseStatusException.class);
        }
        for(var id:ids)service.feature(shop.getId(),owner.getId(),id,false);
    }
    @Test void adminCanConfigureCreateDisableAndReenablePlansWithoutChangingPendingInvoice() {
        String invoice=buy("PRO1");
        var input=new ShopPlanService.PlanInput("Custom Pro",2,new BigDecimal("89000"),2,1,true);
        var custom=service.savePlan(admin.getId(),null,input);
        assertThat(service.plans()).extracting(ShopPlanService.Plan::id).contains(custom.id());
        assertThatThrownBy(()->service.savePlan(owner.getId(),null,input)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(()->service.removePlan(admin.getId(),"FREE")).isInstanceOf(ResponseStatusException.class);
        service.savePlan(admin.getId(),"PRO1",input);
        capture(invoice);
        assertThat(service.status(shop.getId(),owner.getId()).expiresAt()).isEqualTo(now.plusMonths(1));
        assertThat(tx.<BigDecimal>execute(t->payments.invoice(invoice).orElseThrow().getAmount())).isEqualByComparingTo("59000");
        var first=create();create();
        assertThatThrownBy(this::create).isInstanceOf(ResponseStatusException.class);
        service.adminFeature(shop.getId(),admin.getId(),first,true);
        assertThat(service.adminProducts(shop.getId(),admin.getId())).filteredOn(p->p.id().equals(first)).allSatisfy(p->assertThat(p.featured()).isTrue());
        service.removePlan(admin.getId(),"PRO1");
        assertThatThrownBy(()->buy("PRO1")).isInstanceOf(ResponseStatusException.class);
        assertThat(service.status(shop.getId(),owner.getId()).pro()).isTrue();
        assertThat(service.plans()).extracting(ShopPlanService.Plan::id).doesNotContain("PRO1");
        assertThat(service.adminPlans(admin.getId())).filteredOn(p->p.id().equals("PRO1")).allSatisfy(p->assertThat(p.enabled()).isFalse());
        service.savePlan(admin.getId(),"PRO1",input);
        assertThat(buy("PRO1")).isNotBlank();
        service.adminFeature(shop.getId(),admin.getId(),first,false);
    }
    @Test void loweringLimitsPreservesProductsAndCapsPublicFeaturedResultsImmediately() {
        capture(buy("PRO1"));var ids=List.of(create(),create(),create());
        for(var id:ids)service.feature(shop.getId(),owner.getId(),id,true);
        service.savePlan(admin.getId(),"PRO1",new ShopPlanService.PlanInput("Small",1,new BigDecimal("59000"),2,1,true));
        assertThat(service.status(shop.getId(),owner.getId()).productCount()).isEqualTo(3);
        assertThatThrownBy(this::create).isInstanceOf(ResponseStatusException.class);
        assertThat(service.featured().stream().filter(p->ids.contains(p.id())).count()).isEqualTo(1);
        for(var id:ids)service.feature(shop.getId(),owner.getId(),id,false);
    }
    @Test void importedProductsAreRestrictedForFreeAndEveryProPlanAndUnlockOnUpgrade() {
        var ids=new ArrayList<String>();
        // Quota xếp hạng theo createdDate rồi tới id ngẫu nhiên: gán mốc khác nhau để thứ tự tạo là thứ tự xếp hạng.
        for(int i=0;i<46;i++){int n=i;ids.add(tx.execute(t->{
            var saved=products.saveAndFlush(Product.builder().id(id()).shop(shop).category(category)
                    .name("Imported").price(new BigDecimal("100000")).stock(5).status(ProductStatus.ACTIVE).build());
            jdbc.update("update Products set created_date=? where id=?",java.sql.Timestamp.valueOf(LocalDateTime.of(2026,1,1,0,0).plusSeconds(n)),saved.getId());
            return saved.getId();}));}
        assertThat(catalog.browse("",null,shop.getId(),"newest",0,100).totalElements()).isEqualTo(5);
        assertThat(manager.list(shop.getId(),owner.getId(),0).content()).filteredOn(ManagerProductService.Result::quotaBlocked).hasSize(20);
        String blocked=ids.get(5);
        var add=new com.example.flowershop.dto.cart.AddToCartRequest();add.setProductId(blocked);add.setQuantity(1);
        assertThatThrownBy(()->cartService.addToCart(customer.getId(),add)).isInstanceOf(com.example.flowershop.exception.ApiException.class).hasMessageContaining("giới hạn");
        assertThatThrownBy(()->customerOrders.checkout(customer.getId(),new CustomerOrderService.CheckoutRequest("unused",blocked,1,"")))
                .isInstanceOf(com.example.flowershop.exception.ApiException.class).hasMessageContaining("giới hạn");
        String item=tx.execute(t->cartItems.saveAndFlush(CartItem.builder().id(id()).user(customer)
                .product(products.findById(blocked).orElseThrow()).quantity(1).build()).getId());
        var preview=new com.example.flowershop.dto.order.MakeOrderRequest();preview.setShopId(shop.getId());preview.setCartItemIds(List.of(item));
        assertThatThrownBy(()->cartOrders.previewVouchers(customer.getId(),preview,true))
                .isInstanceOf(com.example.flowershop.exception.ApiException.class).hasMessageContaining("giới hạn");
        assertThatThrownBy(()->catalog.detail(blocked)).isInstanceOf(ResponseStatusException.class);
        assertThat(tx.<Boolean>execute(t->quota.allowed(products.findById(blocked).orElseThrow()))).isFalse();
        assertThatThrownBy(()->manager.unhide(shop.getId(),blocked,owner.getId())).isInstanceOf(com.example.flowershop.exception.ApiException.class);
        String[] codes={"PRO1","PRO3","PRO6","PRO12"};int[] limits={15,25,40,46};
        for(int i=0;i<codes.length;i++) {
            capture(buy(codes[i]));
            assertThat(catalog.browse("",null,shop.getId(),"newest",0,100).totalElements()).isEqualTo(limits[i]);
            if(limits[i]<46) {
                var over=ids.get(limits[i]);
                assertThatThrownBy(()->catalog.detail(over)).isInstanceOf(ResponseStatusException.class);
                assertThatThrownBy(()->service.feature(shop.getId(),owner.getId(),over,true)).isInstanceOf(com.example.flowershop.exception.ApiException.class);
            }
        }
        assertThat(catalog.detail(blocked).id()).isEqualTo(blocked);
        now=service.status(shop.getId(),owner.getId()).expiresAt();
        assertThat(catalog.browse("",null,shop.getId(),"newest",0,100).totalElements()).isEqualTo(5);
        manager.remove(shop.getId(),ids.get(0),owner.getId());
        assertThat(catalog.detail(blocked).id()).isEqualTo(blocked);
        assertThat(service.status(shop.getId(),owner.getId()).productCount()).isEqualTo(45);
    }
    @Test void pendingCheckoutDoesNotGrantProAndVerifiedPaymentIsIdempotent() {
        String invoice=buy("PRO1");
        assertThat(service.status(shop.getId(),owner.getId()).pro()).isFalse();
        service.reconcile(invoice); assertThat(service.status(shop.getId(),owner.getId()).pro()).isFalse();
        capture(invoice);
        var expires=service.status(shop.getId(),owner.getId()).expiresAt();
        assertThat(expires).isEqualTo(LocalDateTime.of(2026,2,28,9,0));
        service.reconcile(invoice);assertThat(service.status(shop.getId(),owner.getId()).expiresAt()).isEqualTo(expires);
        assertThat(notifications.mine(owner.getId())).hasSize(1);
    }
    @Test void adminCanGrantAndExtendProWithAuditWithoutInventingPayment() {
        var input=new ShopPlanService.AdminGrant("PRO3","Hỗ trợ shop",id());
        var first=service.adminGrant(shop.getId(),admin.getId(),input);
        assertThat(first.subscription().pro()).isTrue();
        assertThat(first.subscription().expiresAt()).isEqualTo(now.plusMonths(3));
        assertThat(first.history()).hasSize(1);
        assertThat(first.history().get(0).actorName()).isEqualTo("Admin");
        assertThat(first.history().get(0).source()).isEqualTo("ADMIN_GRANT");
        assertThat(first.payments()).isEmpty();
        service.adminGrant(shop.getId(),admin.getId(),input);
        assertThat(service.adminStatus(shop.getId(),admin.getId()).history()).hasSize(1);
        var renewed=service.adminGrant(shop.getId(),admin.getId(),new ShopPlanService.AdminGrant("PRO1","Gia hạn hỗ trợ",id()));
        assertThat(renewed.subscription().expiresAt()).isEqualTo(now.plusMonths(3).plusMonths(1));
        assertThat(notifications.mine(owner.getId())).hasSize(2);
        assertThatThrownBy(()->service.adminGrant(shop.getId(),owner.getId(),new ShopPlanService.AdminGrant("PRO1","Không phải admin",id())))
                .isInstanceOf(ResponseStatusException.class);
    }
    @Test void adminConfirmAndLaterSePayReconciliationActivateOnlyOnce() {
        String invoice=buy("PRO3");
        var result=service.adminConfirm(shop.getId(),admin.getId(),invoice,new ShopPlanService.AdminConfirmation("Đã đối chiếu thanh toán"));
        assertThat(result.subscription().expiresAt()).isEqualTo(now.plusMonths(3));
        assertThat(result.history()).hasSize(1);
        assertThat(result.history().get(0).source()).isEqualTo("ADMIN_CONFIRM");
        service.adminConfirm(shop.getId(),admin.getId(),invoice,new ShopPlanService.AdminConfirmation("Gửi lại"));
        capture(invoice);
        assertThat(service.status(shop.getId(),owner.getId()).expiresAt()).isEqualTo(now.plusMonths(3));
        assertThat(service.adminStatus(shop.getId(),admin.getId()).history()).hasSize(1);
        assertThat(service.payment(shop.getId(),owner.getId(),invoice).status()).isEqualTo("SUCCESS");
    }
    @Test void sePayConfirmationBeforeAdminCannotDoubleExtendAndGrantRequestCannotBeReusedForDifferentPlan() {
        String invoice=buy("PRO1");capture(invoice);
        var expected=service.status(shop.getId(),owner.getId()).expiresAt();
        service.adminConfirm(shop.getId(),admin.getId(),invoice,new ShopPlanService.AdminConfirmation("Đã kiểm tra"));
        assertThat(service.status(shop.getId(),owner.getId()).expiresAt()).isEqualTo(expected);
        String request=id();
        service.adminGrant(shop.getId(),admin.getId(),new ShopPlanService.AdminGrant("PRO1","Hỗ trợ",request));
        assertThatThrownBy(()->service.adminGrant(shop.getId(),admin.getId(),new ShopPlanService.AdminGrant("PRO3","Hỗ trợ",request)))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(()->service.adminConfirm("wrong-shop",admin.getId(),invoice,new ShopPlanService.AdminConfirmation("Wrong shop")))
                .isInstanceOf(ResponseStatusException.class);
    }
    @Test void adminActivationApisValidateRoleCsrfAndRequiredReason() throws Exception {
        String root="/api/admin/shops/"+shop.getId()+"/subscription";
        mvc.perform(get(root).header("Authorization","Bearer shop-test")).andExpect(status().isForbidden());
        mvc.perform(get(root).header("Authorization","Bearer customer-test")).andExpect(status().isForbidden());
        mvc.perform(get(root).header("Authorization","Bearer admin-test")).andExpect(status().isOk());
        mvc.perform(get("/api/admin/shop-plans").header("Authorization","Bearer admin-test")).andExpect(status().isOk());
        var json=new ObjectMapper();
        for(String reason:List.of("", "Cấp Pro cho shop")) {
            var csrfResponse=mvc.perform(get("/api/auth/csrf")).andReturn();var csrf=json.readTree(csrfResponse.getResponse().getContentAsString());
            String body=json.writeValueAsString(new ShopPlanService.AdminGrant("PRO1",reason,id()));
            mvc.perform(post(root+"/activate").session((MockHttpSession)csrfResponse.getRequest().getSession(false))
                            .header(csrf.path("headerName").asText(),csrf.path("token").asText()).header("Authorization","Bearer admin-test")
                            .contentType("application/json").content(body))
                    .andExpect(reason.isEmpty()?status().isBadRequest():status().isOk());
        }
        mvc.perform(post(root+"/activate").header("Authorization","Bearer admin-test").contentType("application/json")
                        .content(json.writeValueAsString(new ShopPlanService.AdminGrant("PRO1","Thiếu CSRF",id()))))
                .andExpect(status().isForbidden());
        assertThat(service.status(shop.getId(),owner.getId()).expiresAt()).isEqualTo(now.plusMonths(1));
    }
    @Test void concurrentCreatesAndReconciliationCannotBypassQuotaOrDoubleExtend() throws Exception {
        for(int i=0;i<4;i++)create();
        var pool=java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            var start=new java.util.concurrent.CountDownLatch(1);
            java.util.concurrent.Callable<Boolean> attempt=()->{start.await();try{create();return true;}catch(ResponseStatusException denied){return false;}};
            var a=pool.submit(attempt);var b=pool.submit(attempt);start.countDown();
            assertThat(List.of(a.get(),b.get())).containsExactlyInAnyOrder(true,false);
            String invoice=buy("PRO1");
            when(gateway.capturedSubscriptionInvoice(invoice)).thenReturn(Optional.of(new SePayGatewayService.CapturedInvoice(invoice,new BigDecimal("59000"),"TX")));
            var reconcileStart=new java.util.concurrent.CountDownLatch(1);
            java.util.concurrent.Callable<Boolean> reconcile=()->{reconcileStart.await();service.reconcile(invoice);return true;};
            var c=pool.submit(reconcile);var d=pool.submit(reconcile);reconcileStart.countDown();c.get();d.get();
            assertThat(service.status(shop.getId(),owner.getId()).expiresAt()).isEqualTo(now.plusMonths(1));
            assertThat(notifications.mine(owner.getId())).hasSize(1);
        } finally {pool.shutdownNow();}
    }
    @Test void simultaneousAdminAndSePayConfirmationGrantOnlyOnePeriod() throws Exception {
        String invoice=buy("PRO1");
        when(gateway.capturedSubscriptionInvoice(invoice)).thenReturn(Optional.of(new SePayGatewayService.CapturedInvoice(invoice,new BigDecimal("59000"),"TX")));
        var pool=java.util.concurrent.Executors.newFixedThreadPool(2);
        var start=new java.util.concurrent.CountDownLatch(1);
        try {
            var adminResult=pool.submit(()->{start.await();service.adminConfirm(shop.getId(),admin.getId(),invoice,new ShopPlanService.AdminConfirmation("Đã đối chiếu"));return true;});
            var sepayResult=pool.submit(()->{start.await();service.reconcile(invoice);return true;});
            start.countDown();
            adminResult.get(15,java.util.concurrent.TimeUnit.SECONDS);
            sepayResult.get(15,java.util.concurrent.TimeUnit.SECONDS);
            assertThat(service.status(shop.getId(),owner.getId()).expiresAt()).isEqualTo(now.plusMonths(1));
            assertThat(service.adminStatus(shop.getId(),admin.getId()).history()).hasSize(1);
            assertThat(notifications.mine(owner.getId())).hasSize(1);
        } finally {pool.shutdownNow();}
    }
    @Test void priceAndDurationAreSnapshottedAndEarlyRenewalPreservesRemainingTime() {
        String first=buy("PRO3");capture(first);
        var end=service.status(shop.getId(),owner.getId()).expiresAt();
        String renewal=buy("PRO6");
        tx.executeWithoutResult(t->{var p=plans.findById("PRO6").orElseThrow();p.setPrice(new BigDecimal("999999"));p.setDurationMonths(1);});
        capture(renewal);
        assertThat(service.status(shop.getId(),owner.getId()).expiresAt()).isEqualTo(end.plusMonths(6));
        assertThat(tx.<BigDecimal>execute(t->payments.invoice(renewal).orElseThrow().getAmount())).isEqualByComparingTo("298000");
    }
    @Test void wrongAmountOrInvoiceCannotActivatePro() {
        String invoice=buy("PRO1");
        when(gateway.capturedSubscriptionInvoice(invoice)).thenReturn(Optional.of(new SePayGatewayService.CapturedInvoice(invoice,BigDecimal.ONE,"WRONG")));
        service.reconcile(invoice);
        assertThat(service.status(shop.getId(),owner.getId()).pro()).isFalse();
        assertThat(service.payment(shop.getId(),owner.getId(),invoice).status()).isEqualTo("REVIEW");
        String other=buy("PRO3");
        when(gateway.capturedSubscriptionInvoice(other)).thenReturn(Optional.of(new SePayGatewayService.CapturedInvoice("FORGED",new BigDecimal("149000"),"WRONG")));
        service.reconcile(other);assertThat(service.status(shop.getId(),owner.getId()).pro()).isFalse();
    }
    @Test void proHasConfiguredFeaturedLimitAndExpiresImmediately() {
        String first=create();
        assertThatThrownBy(()->service.feature(shop.getId(),owner.getId(),first,true)).isInstanceOf(ResponseStatusException.class);
        capture(buy("PRO1"));
        var ids=new ArrayList<String>();ids.add(first);for(int i=0;i<11;i++)ids.add(create());
        for(int i=0;i<3;i++)service.feature(shop.getId(),owner.getId(),ids.get(i),true);
        assertThatThrownBy(()->service.feature(shop.getId(),owner.getId(),ids.get(3),true)).isInstanceOf(ResponseStatusException.class);
        assertThat(service.featured()).hasSize(3);
        service.feature(shop.getId(),owner.getId(),first,false);
        service.feature(shop.getId(),owner.getId(),ids.get(10),true);
        now=service.status(shop.getId(),owner.getId()).expiresAt();
        assertThat(service.featured()).isEmpty();assertThat(service.status(shop.getId(),owner.getId()).pro()).isFalse();
        assertThat(tx.<Long>execute(t->products.findByShopId(shop.getId(),org.springframework.data.domain.Pageable.unpaged()).getTotalElements())).isEqualTo(12);
        capture(buy("PRO1"));
        assertThat(service.status(shop.getId(),owner.getId()).featuredProductIds()).isEmpty();
    }
    @Test void expiredProReturnsToFiveExistingProductsWithoutMonthlyReset() {
        capture(buy("PRO1"));
        now=service.status(shop.getId(),owner.getId()).expiresAt().minusHours(1);
        for(int i=0;i<3;i++)create();
        now=now.plusHours(1);
        var expired=service.status(shop.getId(),owner.getId());
        assertThat(expired.pro()).isFalse();
        assertThat(expired.productLimit()).isEqualTo(5);
        assertThat(expired.productCount()).isEqualTo(3);
        create();create();
        assertThatThrownBy(this::create).isInstanceOf(ResponseStatusException.class).hasMessageContaining("5");
        now=now.plusMonths(1).withDayOfMonth(1);
        assertThatThrownBy(this::create).isInstanceOf(ResponseStatusException.class).hasMessageContaining("5");
    }
    @Test void reminderOnlyRunsInLastThreeValidDatesAtNineAndNineteenWithoutDuplicates() {
        capture(buy("PRO1"));
        var end=LocalDateTime.of(2026,3,1,0,0);
        String subscription=tx.execute(t->{var s=subscriptions.forShop(shop.getId()).orElseThrow();s.setEndsAt(end);return s.getId();});
        now=LocalDateTime.of(2026,2,25,19,0);reminders.queue(subscription);
        assertThat(reminderCount()).isZero();
        for(int day=26;day<=28;day++)for(int hour:new int[]{9,19}) {
            now=LocalDateTime.of(2026,2,day,hour,0);
            assertThat(reminders.dueSubscriptions()).contains(subscription);
            reminders.queue(subscription);reminders.queue(subscription);
            reminders.dueMail().forEach(reminders::deliver);
        }
        assertThat(reminderCount()).isEqualTo(6);
        assertThat(notifications.mine(owner.getId())).hasSize(7);
        verify(mail,times(6)).send(anyString(),anyString(),anyString());
        now=end;reminders.queue(subscription);assertThat(reminderCount()).isEqualTo(6);
    }
    @Test void mailFailureRetriesWithoutDuplicatingBellAndRenewalCancelsOldReminder() {
        capture(buy("PRO1"));
        var end=service.status(shop.getId(),owner.getId()).expiresAt();now=end.minusDays(1).withHour(9);
        String sub=tx.execute(t->subscriptions.forShop(shop.getId()).orElseThrow().getId());
        reminders.queue(sub);String reminder=reminders.dueMail().get(0);
        when(mail.send(anyString(),anyString(),anyString())).thenReturn(false);
        reminders.deliver(reminder);reminders.queue(sub);
        assertThat(notifications.mine(owner.getId())).hasSize(2);
        capture(buy("PRO1")); now=now.plusMinutes(6);
        reminders.deliver(reminder);
        assertThat(tx.<String>execute(t->reminderRows.findById(reminder).orElseThrow().getMailStatus())).isEqualTo("CANCELLED");
    }
    @Test void reminderWindowHandlesLeapYearAndThirtyAndThirtyOneDayMonths() {
        capture(buy("PRO1"));
        String sub=tx.execute(t->subscriptions.forShop(shop.getId()).orElseThrow().getId());
        for(LocalDate end:List.of(LocalDate.of(2028,3,1),LocalDate.of(2026,5,1),LocalDate.of(2026,6,1))) {
            tx.executeWithoutResult(t->subscriptions.findById(sub).orElseThrow().setEndsAt(end.atStartOfDay()));
            for(int before=3;before>=1;before--)for(int slot:new int[]{9,19}) {
                now=end.minusDays(before).atTime(slot,0);
                assertThat(reminders.dueSubscriptions()).contains(sub);
                reminders.queue(sub);
            }
        }
        assertThat(reminderCount()).isEqualTo(18);
    }
    long reminderCount() { return tx.execute(t->reminderRows.findAll().stream().filter(r->r.getSubscription().getShop().getId().equals(shop.getId())).count()); }
    @Test void shopApisEnforceOwnershipRolesAndCsrf() throws Exception {
        String root="/api/shop/mine/"+shop.getId()+"/subscription";
        mvc.perform(get(root)).andExpect(status().isUnauthorized());
        mvc.perform(get(root).header("Authorization","Bearer customer-test")).andExpect(status().isForbidden());
        mvc.perform(get("/api/shop/mine/other/subscription").header("Authorization","Bearer shop-test")).andExpect(status().isNotFound());
        mvc.perform(post(root+"/checkout").header("Authorization","Bearer shop-test").contentType("application/json").content("{\"planId\":\"PRO1\"}"))
                .andExpect(status().isForbidden());
        var csrfResponse=mvc.perform(get("/api/auth/csrf")).andReturn();var json=new ObjectMapper();var csrf=json.readTree(csrfResponse.getResponse().getContentAsString());
        mvc.perform(post(root+"/checkout").session((MockHttpSession)csrfResponse.getRequest().getSession(false))
                .header(csrf.path("headerName").asText(),csrf.path("token").asText()).header("Authorization","Bearer shop-test")
                .contentType("application/json").content("{\"planId\":\"PRO3\",\"amount\":1}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.amount").value(149000));
    }
}
