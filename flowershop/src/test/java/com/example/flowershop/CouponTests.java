package com.example.flowershop;

import com.example.flowershop.entity.*;
import com.example.flowershop.entity.Order;
import com.example.flowershop.entity.enums.*;
import com.example.flowershop.repository.*;
import com.example.flowershop.service.*;
import com.example.flowershop.dto.order.MakeOrderRequest;
import com.example.flowershop.exception.ApiException;
import com.example.flowershop.dto.auth.CurrentUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:coupons;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa","spring.datasource.password=","spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop","app.shop-follow-mail.enabled=false","spring.jpa.show-sql=false"})
@AutoConfigureMockMvc @Transactional
class CouponTests {
    @MockitoBean JavaMailSender mail;
    @MockitoBean GhnService ghn;
    @MockitoBean TokenAuthService tokens;
    @Autowired CouponService service;
    @Autowired CustomerOrderService customerOrders;
    @Autowired OrderService cartOrders;
    @Autowired CustomOrderWorkflowService workflow;
    @Autowired ManagerOrderService manager;
    @Autowired CouponRepository coupons;
    @Autowired UserRepository users;
    @Autowired ShopRepository shops;
    @Autowired ProductRepository products;
    @Autowired CategoryRepository categories;
    @Autowired AddressRepository addresses;
    @Autowired OrderRepository orders;
    @Autowired OrderDetailRepository orderDetails;
    @Autowired RefundRepository refunds;
    @Autowired jakarta.persistence.EntityManager entityManager;
    @Autowired PaymentRepository payments;
    @Autowired CartItemRepository cart;
    @Autowired CustomFlowerOptionRepository options;
    @Autowired CustomOrderRequestRepository customRequests;
    @Autowired MockMvc mvc;
    @Autowired org.springframework.transaction.PlatformTransactionManager transactionManager;
    User admin,owner,customer;
    Shop shop,otherShop;
    Product product;
    Address destination;
    final ObjectMapper json=new ObjectMapper();
    String id() { return UUID.randomUUID().toString(); }
    BigDecimal money(String amount) { return new BigDecimal(amount); }
    User user(UserRole role) { return users.saveAndFlush(User.builder().id(id()).email(id()+"@test.example").role(role)
            .fullName("Test user").phone("0901234567").status(UserStatus.ACTIVE).isEmailVerified(true).build()); }
    @BeforeEach void setup() {
        admin=user(UserRole.ADMIN); owner=user(UserRole.SHOP); customer=user(UserRole.CUSTOMER);
        shop=shops.saveAndFlush(Shop.builder().id(id()).owner(owner).name("Voucher shop").status(ShopStatus.ACTIVE).build());
        otherShop=shops.saveAndFlush(Shop.builder().id(id()).owner(user(UserRole.SHOP)).name("Other shop").status(ShopStatus.ACTIVE).build());
        var category=categories.saveAndFlush(Category.builder().id(id()).name("Flowers").build());
        product=products.saveAndFlush(Product.builder().id(id()).shop(shop).category(category).name("Rose").price(money("500000")).stock(20).build());
        destination=addresses.saveAndFlush(Address.builder().id(id()).user(customer).recipientName("Customer").recipientPhone("0901234567")
                .addressLine("Street").ghnDistrictId(2).ghnWardCode("002").build());
        addresses.saveAndFlush(Address.builder().id(id()).shop(shop).addressLine("Pickup").isDefault(true).ghnDistrictId(1).ghnWardCode("001").build());
        when(ghn.calculateFee(anyInt(),anyString(),anyInt(),anyString(),anyInt())).thenReturn(money("30000"));
        when(tokens.authenticate("admin")).thenReturn(CurrentUser.from(admin));
        when(tokens.authenticate("shop")).thenReturn(CurrentUser.from(owner));
        when(tokens.authenticate("customer")).thenReturn(CurrentUser.from(customer));
    }
    Coupon coupon(String code,CouponTarget target,String percent,String cap) {
        return coupons.saveAndFlush(Coupon.builder().id(id()).code(code).target(target).discountType(DiscountType.PERCENTAGE)
                .discountValue(money(percent)).maxDiscountValue(money(cap)).startDate(LocalDateTime.now().minusDays(1))
                .endDate(LocalDateTime.now().plusDays(1)).usageLimit(10).build());
    }
    CouponService.Context ctx() { return new CouponService.Context(shop.getId(),money("500000"),money("30000"),Map.of(product.getId(),money("500000"))); }
    CouponService.Input input(CouponTarget target,String cap) { return new CouponService.Input("NEW20",target,DiscountType.PERCENTAGE,money("10"),BigDecimal.ZERO,
            money(cap),LocalDateTime.now().minusDays(1),LocalDateTime.now().plusDays(1),100,CouponStatus.ACTIVE,null); }
    @Test void crudIsScopedAndDeletePreservesHistoricalReferences() {
        var global=service.save(admin.getId(),null,null,input(CouponTarget.ORDER,"20000"));
        assertThat(service.list(admin.getId(),null)).extracting(CouponService.View::id).contains(global.id());
        assertThat(service.list(owner.getId(),shop.getId())).isEmpty();
        assertThatThrownBy(() -> service.delete(owner.getId(),shop.getId(),global.id())).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.save(owner.getId(),shop.getId(),null,input(CouponTarget.SHIPPING,"20000"))).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.list(owner.getId(),otherShop.getId())).isInstanceOf(ApiException.class);
        service.save(admin.getId(),null,global.id(),input(CouponTarget.SHIPPING,"70000"));
        service.delete(admin.getId(),null,global.id());
        assertThat(coupons.findById(global.id()).orElseThrow().isDeleted()).isTrue();
        assertThat(service.list(admin.getId(),null)).isEmpty();
    }
    private CustomOrderRequest acceptedDesign() {
        return customRequests.saveAndFlush(CustomOrderRequest.builder().id(id()).customer(customer).shop(shop)
                .budget(money("500000")).status(CustomOrderStatus.ACCEPTED)
                .desiredComponents("{\"quantity\":2,\"quotedPrice\":500000,\"depositPercent\":30}").build());
    }
    private com.example.flowershop.dto.order.MakeCustomOrderRequest depositInput() {
        var input=new com.example.flowershop.dto.order.MakeCustomOrderRequest();
        input.setDeliveryAddressId(destination.getId());return input;
    }
    @Test void separateDepositLocksVouchersAndChargesPercentageOfDiscountedFlowers() {
        var design=acceptedDesign();
        var low=coupon("CUSTOMLOW",CouponTarget.ORDER,"5","20000");low.setShop(shop);
        var high=coupon("CUSTOMHIGH",CouponTarget.ORDER,"20","70000");high.setShop(shop);
        var ship=coupon("CUSTOMSHIP",CouponTarget.SHIPPING,"100","30000");coupons.flush();
        var input=depositInput();input.setCouponCode(low.getCode());input.setShippingCouponCode(ship.getCode());
        var preview=cartOrders.previewCustomVouchers(customer.getId(),design.getId(),input);
        assertThat(preview.totalAmount()).isEqualByComparingTo("480000");
        assertThat(preview.depositAmount()).isEqualByComparingTo("144000");
        assertThat(preview.remainingAmount()).isEqualByComparingTo("336000");
        assertThat(preview.orderVouchers()).filteredOn(v->v.code().equals(high.getCode())).allSatisfy(v->assertThat(v.eligible()).isFalse());
        input.setExpectedTotal(preview.totalAmount());
        input.setExpectedDeposit(preview.depositAmount());
        var created=cartOrders.makeCustomOrder(customer.getId(),design.getId(),input);
        var saved=orders.findById(created.getId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(OrderStatus.AWAITING_DEPOSIT);
        assertThat(saved.getTotalAmount()).isEqualByComparingTo("480000");
        assertThat(saved.getDepositAmount()).isEqualByComparingTo("144000");
        assertThat(created.getCouponCode()).isEqualTo(low.getCode());
        assertThat(created.getShippingCouponCode()).isEqualTo(ship.getCode());
        assertThat(coupons.findById(low.getId()).orElseThrow().getUsedCount()).isEqualTo(1);
        assertThat(coupons.findById(ship.getId()).orElseThrow().getUsedCount()).isEqualTo(1);
        assertThat(payments.findByOrderId(saved.getId())).isEmpty();
        var gateway=PaymentBusinessTests.newGateway(orders,payments,refunds,cartOrders);
        org.springframework.test.util.ReflectionTestUtils.setField(gateway,"merchantId","merchant-test");
        org.springframework.test.util.ReflectionTestUtils.setField(gateway,"secretKey","secret-test");
        var paymentInput=com.example.flowershop.dto.payment.CreatePaymentRequest.builder()
                .orderId(saved.getId()).paymentType(PaymentType.DEPOSIT).build();
        var first=gateway.createPayment(paymentInput,customer.getId());
        var retry=gateway.createPayment(paymentInput,customer.getId());
        assertThat(first.getAmount()).isEqualByComparingTo(preview.depositAmount());
        assertThat(retry.getFields().get("order_amount")).isEqualTo("144000");
        assertThat(first.getInvoiceNumber()).isNotEqualTo(retry.getInvoiceNumber());
        assertThat(coupons.findById(low.getId()).orElseThrow().getUsedCount()).isEqualTo(1);
        cartOrders.cancelByCustomer(customer.getId(),saved.getId(),"Changed design");
        assertThat(coupons.findById(low.getId()).orElseThrow().getUsedCount()).isZero();
        assertThat(coupons.findById(ship.getId()).orElseThrow().getUsedCount()).isZero();
    }
    @Test void separateDepositRejectsStaleTotalAndProductVoucherForReferenceProduct() {
        var design=acceptedDesign();var input=depositInput();
        input.setExpectedTotal(money("1"));
        assertThatThrownBy(()->cartOrders.makeCustomOrder(customer.getId(),design.getId(),input)).isInstanceOf(ApiException.class);
    }
    @Test void separateDepositPreviewChecksCustomerOwnershipAndDoesNotUseReferenceProductVouchers() {
        var design=acceptedDesign();var input=depositInput();
        var productVoucher=coupon("CUSTOMPRODUCT",CouponTarget.PRODUCT,"20","70000");productVoucher.setProduct(product);coupons.flush();
        var preview=cartOrders.previewCustomVouchers(customer.getId(),design.getId(),input);
        assertThat(preview.orderVouchers()).filteredOn(v->v.code().equals(productVoucher.getCode()))
                .allSatisfy(v->{assertThat(v.eligible()).isFalse();assertThat(v.reason()).contains("sản phẩm");});
        assertThatThrownBy(()->cartOrders.previewCustomVouchers(owner.getId(),design.getId(),input)).isInstanceOf(ApiException.class);
    }
    @Test void shopManualCancellationRestoresStockReleasesVoucherAndRequestsRefundOnlyOnce() {
        product.setStock(18);products.flush();
        var order=orders.saveAndFlush(Order.builder().id(id()).customer(customer).shop(shop).orderType(OrderType.STANDARD)
                .status(OrderStatus.PROCESSING).subTotal(money("1000000")).totalAmount(money("1000000")).build());
        orderDetails.saveAndFlush(OrderDetail.builder().id(id()).order(order).product(product).quantity(2).price(money("500000")).build());
        var voucher=coupon("SHOPCANCEL",CouponTarget.ORDER,"10","20000");
        service.apply(order,new CouponService.Context(shop.getId(),money("1000000"),BigDecimal.ZERO,Map.of(product.getId(),money("1000000"))),voucher.getCode(),null);
        payments.saveAndFlush(Payment.builder().id(id()).order(order).paymentType(PaymentType.DEPOSIT).paymentMethod(PaymentMethod.ONLINE)
                .amount(money("100000")).status(PaymentStatus.SUCCESS).build());
        var pending=payments.saveAndFlush(Payment.builder().id(id()).order(order).paymentType(PaymentType.FINAL).paymentMethod(PaymentMethod.COD)
                .amount(money("880000")).status(PaymentStatus.PENDING).build());
        manager.changeStatus(shop.getId(),owner.getId(),order.getId(),"CANCELLED");
        assertThat(product.getStock()).isEqualTo(20);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(voucher.getUsedCount()).isZero();
        entityManager.refresh(pending);assertThat(pending.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(refunds.findByOrderIdOrderByCreatedDateAsc(order.getId())).hasSize(1).allSatisfy(refund->assertThat(refund.getAmount()).isEqualByComparingTo("100000"));
        assertThatThrownBy(()->manager.cancel(shop.getId(),owner.getId(),order.getId(),"Again")).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        assertThat(product.getStock()).isEqualTo(20);
    }
    @Test void discountCapsAndRoundingUseActualMoney() {
        coupon("CAP70",CouponTarget.ORDER,"50","70000");
        assertThat(service.preview(ctx(),"CAP70",null).discountAmount()).isEqualByComparingTo("70000");
        var small=new CouponService.Context(shop.getId(),money("101"),money("30000"),Map.of());
        assertThat(service.preview(small,"CAP70",null).discountAmount()).isEqualByComparingTo("50");
        coupon("FREE",CouponTarget.SHIPPING,"100","70000");
        assertThat(service.preview(ctx(),null,"FREE").shippingDiscountAmount()).isEqualByComparingTo("30000");
    }
    @Test void shippingAllowsOnlyLowestActualSavingAndDisablesOthers() {
        coupon("CAP20",CouponTarget.ORDER,"100","20000");
        coupon("ONEPERCENT",CouponTarget.ORDER,"1","70000");
        coupon("SHIP",CouponTarget.SHIPPING,"100","30000");
        var result=service.preview(ctx(),"ONEPERCENT","SHIP");
        assertThat(result.totalAmount()).isEqualByComparingTo("495000");
        assertThat(result.orderVouchers()).filteredOn(v -> v.code().equals("CAP20"))
                .allSatisfy(v -> { assertThat(v.eligible()).isFalse(); assertThat(v.reason()).isEqualTo("mã voucher không phù hợp"); });
        assertThatThrownBy(() -> service.preview(ctx(),"CAP20","SHIP")).isInstanceOf(ApiException.class).hasMessage("mã voucher không phù hợp");
    }
    @Test void recommendsBestCombinationRatherThanAlwaysStacking() {
        coupon("LOW",CouponTarget.ORDER,"10","20000");
        var high=coupon("HIGH",CouponTarget.ORDER,"20","70000");
        coupon("SHIP",CouponTarget.SHIPPING,"100","30000");
        var result=service.preview(ctx(),null,null);
        assertThat(result.recommendedOrderCode()).isEqualTo("HIGH");
        assertThat(result.recommendedShippingCode()).isNull();
        assertThat(result.bestSaving()).isEqualByComparingTo("70000");
        high.setDiscountValue(money("5")); coupons.saveAndFlush(high);
        result=service.preview(ctx(),null,null);
        assertThat(result.recommendedOrderCode()).isEqualTo("LOW");
        assertThat(result.recommendedShippingCode()).isEqualTo("SHIP");
        assertThat(result.bestSaving()).isEqualByComparingTo("50000");
    }
    @Test void minimumExcludesExpiredUnavailableAndWrongShopVouchersAndAcceptsTies() {
        var expired=coupon("EXPIRED",CouponTarget.ORDER,"1","20000"); expired.setEndDate(LocalDateTime.now().minusHours(1));
        var future=coupon("FUTURE",CouponTarget.ORDER,"1","20000"); future.setStartDate(LocalDateTime.now().plusHours(1));
        var used=coupon("USED",CouponTarget.ORDER,"1","20000"); used.setUsedCount(10);
        var wrong=coupon("OTHER",CouponTarget.ORDER,"1","20000"); wrong.setShop(otherShop);
        var min=coupon("MINIMUM",CouponTarget.ORDER,"1","20000"); min.setMinOrderValue(money("600000"));
        coupon("LOW1",CouponTarget.ORDER,"10","20000"); coupon("LOW2",CouponTarget.ORDER,"10","20000"); coupon("SHIP",CouponTarget.SHIPPING,"100","30000");
        coupons.flush();
        var result=service.preview(ctx(),"LOW2","SHIP");
        assertThat(result.orderVouchers()).filteredOn(CouponService.Choice::eligible).extracting(CouponService.Choice::code).containsExactlyInAnyOrder("LOW1","LOW2");
        assertThat(result.orderVouchers()).extracting(CouponService.Choice::code).doesNotContain("OTHER");
    }
    @Test void productVoucherOnlyDiscountsItsProductLines() {
        var c=coupon("PRODUCT",CouponTarget.PRODUCT,"50","70000"); c.setProduct(product); coupons.saveAndFlush(c);
        var ctx=new CouponService.Context(shop.getId(),money("500000"),money("30000"),Map.of(product.getId(),money("100000")));
        assertThat(service.preview(ctx,"PRODUCT",null).discountAmount()).isEqualByComparingTo("50000");
        var withoutProduct=new CouponService.Context(shop.getId(),money("500000"),money("30000"),Map.of());
        assertThat(service.preview(withoutProduct,null,null).orderVouchers()).allMatch(v -> !v.eligible());
    }
    @Test void directCheckoutPersistsBothDiscountsAndPaymentTotalAndReleasesOnce() {
        var low=coupon("LOW",CouponTarget.ORDER,"10","20000"); var ship=coupon("SHIP",CouponTarget.SHIPPING,"100","30000");
        var result=customerOrders.checkout(customer.getId(),new CustomerOrderService.CheckoutRequest(destination.getId(),product.getId(),1,"","LOW","SHIP"));
        assertThat(result.totalAmount()).isEqualByComparingTo("480000");
        var saved=customerOrders.detail(customer.getId(),result.orderId());
        assertThat(saved.couponCode()).isEqualTo("LOW"); assertThat(saved.shippingCouponCode()).isEqualTo("SHIP");
        assertThat(saved.discountAmount()).isEqualByComparingTo("20000"); assertThat(saved.shippingDiscountAmount()).isEqualByComparingTo("30000");
        assertThat(payments.findByOrderId(result.orderId())).allSatisfy(p -> assertThat(p.getAmount()).isEqualByComparingTo("480000"));
        assertThat(low.getUsedCount()).isEqualTo(1); assertThat(ship.getUsedCount()).isEqualTo(1);
        manager.cancel(shop.getId(),owner.getId(),result.orderId(),"Cancel");
        service.release(orders.findById(result.orderId()).orElseThrow());
        assertThat(low.getUsedCount()).isZero(); assertThat(ship.getUsedCount()).isZero();
    }
    @Test void cartCheckoutUsesSameRulesAndExpiryReleasesBothOnce() {
        var low=coupon("LOW",CouponTarget.ORDER,"10","20000"); var ship=coupon("SHIP",CouponTarget.SHIPPING,"100","30000");
        var item=cart.saveAndFlush(CartItem.builder().id(id()).user(customer).product(product).quantity(1).build());
        var req=new MakeOrderRequest(); req.setShopId(shop.getId()); req.setCartItemIds(List.of(item.getId())); req.setDeliveryAddressId(destination.getId());
        req.setCouponId("LOW"); req.setShippingCouponCode("SHIP"); req.setPaymentMethod("ONLINE");
        assertThat(cartOrders.previewVouchers(customer.getId(),req).totalAmount()).isEqualByComparingTo("480000");
        var result=cartOrders.makeOrder(customer.getId(),req);
        assertThat(result.getTotalAmount()).isEqualByComparingTo("480000");
        var order=orders.findById(result.getId()).orElseThrow(); order.setPaymentStartedAt(LocalDateTime.now().minusMinutes(11));
        cartOrders.cancelExpiredOrder(order.getId(),"Expired"); cartOrders.cancelExpiredOrder(order.getId(),"Expired again");
        assertThat(low.getUsedCount()).isZero(); assertThat(ship.getUsedCount()).isZero();
    }
    @Test void customQuoteAppliesVouchersOnlyAtAcceptance() {
        CustomFlowerCatalogFixture.seed(options);
        var low=coupon("LOW",CouponTarget.ORDER,"10","20000"); coupon("SHIP",CouponTarget.SHIPPING,"100","30000");
        var design=new CustomFlowerOrderService.RequestInput(shop.getId(),1,"",new CustomFlowerOrderService.Preferences(null,null,null,null,null,null));
        var receipt=workflow.create(customer.getId(),new CustomOrderWorkflowService.CreateInput(destination.getId(),design));
        workflow.quote(shop.getId(),owner.getId(),receipt.orderId(),new CustomOrderWorkflowService.Quote(money("400000"),"Quote"));
        var preview=workflow.previewVouchers(customer.getId(),receipt.orderId(),new CustomOrderWorkflowService.VoucherInput("LOW","SHIP"));
        assertThat(low.getUsedCount()).isZero();
        workflow.accept(customer.getId(),receipt.orderId(),new CustomOrderWorkflowService.Accept(PaymentMethod.COD,preview.totalAmount(),"LOW","SHIP"));
        assertThat(customerOrders.detail(customer.getId(),receipt.orderId()).totalAmount()).isEqualByComparingTo("380000");
        assertThat(low.getUsedCount()).isEqualTo(1);
    }
    @Test void quotedCustomWithVouchersCanPayDepositInsteadOfBeingForcedToFullOrCod() {
        CustomFlowerCatalogFixture.seed(options);
        coupon("DEPOSITLOW",CouponTarget.ORDER,"10","20000");
        coupon("DEPOSITSHIP",CouponTarget.SHIPPING,"100","30000");
        var design=new CustomFlowerOrderService.RequestInput(shop.getId(),1,"",
                new CustomFlowerOrderService.Preferences(null,null,null,null,null,null));
        var receipt=workflow.create(customer.getId(),new CustomOrderWorkflowService.CreateInput(destination.getId(),design));
        workflow.quote(shop.getId(),owner.getId(),receipt.orderId(),new CustomOrderWorkflowService.Quote(money("400000"),"Quote"));
        var preview=workflow.previewVouchers(customer.getId(),receipt.orderId(),new CustomOrderWorkflowService.VoucherInput("DEPOSITLOW","DEPOSITSHIP"));
        assertThat(preview.totalAmount()).isEqualByComparingTo("380000");
        assertThat(preview.depositAmount()).isEqualByComparingTo("190000");
        var result=workflow.accept(customer.getId(),receipt.orderId(),new CustomOrderWorkflowService.Accept(
                PaymentMethod.ONLINE,preview.totalAmount(),"DEPOSITLOW","DEPOSITSHIP",PaymentType.DEPOSIT,preview.depositAmount()));
        assertThat(result.status()).isEqualTo("AWAITING_DEPOSIT");
        var order=orders.findById(receipt.orderId()).orElseThrow();
        assertThat(order.getPaymentStartedAt()).isNotNull();
        var gateway=PaymentBusinessTests.newGateway(orders,payments,refunds,cartOrders);
        org.springframework.test.util.ReflectionTestUtils.setField(gateway,"merchantId","merchant-test");
        org.springframework.test.util.ReflectionTestUtils.setField(gateway,"secretKey","secret-test");
        var payment=gateway.createPayment(com.example.flowershop.dto.payment.CreatePaymentRequest.builder()
                .orderId(order.getId()).paymentType(PaymentType.DEPOSIT).build(),customer.getId());
        assertThat(payment.getAmount()).isEqualByComparingTo("190000");
        assertThat(payment.getFields().get("order_amount")).isEqualTo("190000");
    }
    @Test void multiShopCheckoutKeepsEachShopsVoucherAndDiscountedTotal() {
        var first=coupon("FIRST",CouponTarget.ORDER,"5","20000"); first.setShop(shop); coupons.saveAndFlush(first);
        var second=coupon("SECOND",CouponTarget.ORDER,"10","50000"); second.setShop(otherShop); coupons.saveAndFlush(second);
        var otherProduct=products.saveAndFlush(Product.builder().id(id()).shop(otherShop).category(product.getCategory()).name("Other rose").price(money("500000")).stock(20).build());
        addresses.saveAndFlush(Address.builder().id(id()).shop(otherShop).addressLine("Other pickup").isDefault(true).ghnDistrictId(3).ghnWardCode("003").build());
        var item1=cart.saveAndFlush(CartItem.builder().id(id()).user(customer).product(product).quantity(1).build());
        var item2=cart.saveAndFlush(CartItem.builder().id(id()).user(customer).product(otherProduct).quantity(1).build());
        var a=new com.example.flowershop.dto.order.MakeOrdersRequest.ShopOrder(); a.setShopId(shop.getId()); a.setCartItemIds(List.of(item1.getId())); a.setCouponId("FIRST"); a.setExpectedTotal(money("510000"));
        var b=new com.example.flowershop.dto.order.MakeOrdersRequest.ShopOrder(); b.setShopId(otherShop.getId()); b.setCartItemIds(List.of(item2.getId())); b.setCouponId("SECOND"); b.setExpectedTotal(money("480000"));
        var request=new com.example.flowershop.dto.order.MakeOrdersRequest(); request.setDeliveryAddressId(destination.getId()); request.setPaymentMethod("ONLINE"); request.setRecipientName("Customer"); request.setPhone("0901234567"); request.setShops(List.of(a,b));
        var result=cartOrders.makeOrders(customer.getId(),request);
        assertThat(result).hasSize(2);
        assertThat(result).extracting(com.example.flowershop.dto.order.OrderResponse::getCouponCode).containsExactlyInAnyOrder("FIRST","SECOND");
        assertThat(result.stream().map(com.example.flowershop.dto.order.OrderResponse::getTotalAmount).reduce(BigDecimal.ZERO,BigDecimal::add)).isEqualByComparingTo("990000");
        assertThat(result.stream().map(r -> orders.findById(r.getId()).orElseThrow().getCheckoutGroup()).distinct().toList()).hasSize(1).doesNotContainNull();
    }
    MockHttpServletRequestBuilder csrf(MockHttpServletRequestBuilder request,String role) throws Exception {
        var result=mvc.perform(get("/api/auth/csrf")).andReturn(); var token=json.readTree(result.getResponse().getContentAsString());
        return request.session((MockHttpSession)result.getRequest().getSession(false)).header(token.path("headerName").asText(),token.path("token").asText())
                .header("Authorization","Bearer "+role).contentType("application/json");
    }
    String body(String target,int cap) {
        return "{\"code\":\"TEST20\",\"target\":\""+target+"\",\"discountType\":\"PERCENTAGE\",\"discountValue\":10,\"minOrderValue\":0,\"maxDiscountValue\":"+cap+
                ",\"startDate\":\"2026-01-01T00:00:00\",\"endDate\":\"2027-01-01T00:00:00\",\"usageLimit\":100,\"status\":\"ACTIVE\"}";
    }
    @Test void apisEnforceRolesOwnershipAndCapBounds() throws Exception {
        mvc.perform(get("/api/admin/vouchers")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/vouchers").header("Authorization","Bearer shop")).andExpect(status().isForbidden());
        mvc.perform(get("/api/shop/mine/"+otherShop.getId()+"/vouchers").header("Authorization","Bearer shop")).andExpect(status().isNotFound());
        mvc.perform(csrf(post("/api/admin/vouchers"),"admin").content(body("ORDER",0))).andExpect(status().isBadRequest());
        mvc.perform(csrf(post("/api/admin/vouchers"),"admin").content(body("ORDER",100001))).andExpect(status().isBadRequest());
        mvc.perform(csrf(post("/api/shop/mine/"+shop.getId()+"/vouchers"),"shop").content(body("SHIPPING",20000))).andExpect(status().isBadRequest());
        mvc.perform(csrf(post("/api/admin/vouchers"),"admin").content(body("SHIPPING",20000))).andExpect(status().isCreated()).andExpect(jsonPath("$.shopId").isEmpty());
        mvc.perform(csrf(post("/api/admin/vouchers"),"customer").content(body("ORDER",20000))).andExpect(status().isForbidden());
    }
    @Test void capAcceptsOneDongThroughOneHundredThousandAndCheckoutUsesNewCap() throws Exception {
        for(int cap:new int[]{1,19000,71000,100000}) {
            mvc.perform(csrf(post("/api/admin/vouchers"),"admin").content(body("ORDER",cap).replace("TEST20","CAP"+cap)))
                    .andExpect(status().isCreated()).andExpect(jsonPath("$.maxDiscountValue").value(cap));
        }
        for(int cap:new int[]{-1,0,100001}) {
            mvc.perform(csrf(post("/api/admin/vouchers"),"admin").content(body("ORDER",cap)))
                    .andExpect(status().isBadRequest());
            assertThatThrownBy(()->service.save(admin.getId(),null,null,input(CouponTarget.ORDER,Integer.toString(cap)))).isInstanceOf(ApiException.class);
        }
        coupon("SMALLCAP",CouponTarget.ORDER,"100","1");
        coupon("MAXCAP",CouponTarget.ORDER,"100","100000");
        assertThat(service.preview(ctx(),"SMALLCAP",null).discountAmount()).isEqualByComparingTo("1");
        assertThat(service.preview(ctx(),"MAXCAP",null).discountAmount()).isEqualByComparingTo("100000");
        var result=customerOrders.checkout(customer.getId(),new CustomerOrderService.CheckoutRequest(destination.getId(),product.getId(),1,"","MAXCAP",null));
        assertThat(result.discountAmount()).isEqualByComparingTo("100000");
        assertThat(result.totalAmount()).isEqualByComparingTo("430000");
    }
    @Test void merchandisePreviewWorksWithoutAddressButCannotBypassCheckoutShipping() throws Exception {
        var code=coupon("ADDRESSFREE",CouponTarget.ORDER,"10","20000");
        coupon("SHIPADDRESS",CouponTarget.SHIPPING,"100","30000");
        var item=cart.saveAndFlush(CartItem.builder().id(id()).user(customer).product(product).quantity(1).build());
        var body=new HashMap<String,Object>();
        body.put("shopId",shop.getId()); body.put("cartItemIds",List.of(item.getId()));
        body.put("couponId",code.getCode()); body.put("merchandiseOnly",true);
        String path="/api/orders/"+customer.getId()+"/voucher-preview";
        mvc.perform(csrf(post(path),"customer").content(json.writeValueAsString(body)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.discountAmount").value(20000))
                .andExpect(jsonPath("$.totalAmount").value(480000))
                .andExpect(jsonPath("$.shippingVouchers[0].eligible").value(false));
        verify(ghn,never()).calculateFee(anyInt(),anyString(),anyInt(),anyString(),anyInt());
        assertThat(coupons.findById(code.getId()).orElseThrow().getUsedCount()).isZero();
        body.put("shippingCouponCode","SHIPADDRESS");
        mvc.perform(csrf(post(path),"customer").content(json.writeValueAsString(body))).andExpect(status().isBadRequest());
        body.remove("shippingCouponCode");
        mvc.perform(csrf(post("/api/orders/"+customer.getId()),"customer").content(json.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
        body.put("merchandiseOnly",false);
        mvc.perform(csrf(post(path),"customer").content(json.writeValueAsString(body))).andExpect(status().isBadRequest());
    }
    @Test void checkoutApiRejectsForgedHighVoucherWithShipping() throws Exception {
        coupon("LOW",CouponTarget.ORDER,"10","20000"); coupon("HIGH",CouponTarget.ORDER,"20","70000"); coupon("SHIP",CouponTarget.SHIPPING,"100","30000");
        String request="{\"addressId\":\""+destination.getId()+"\",\"productId\":\""+product.getId()+"\",\"quantity\":1,\"couponCode\":\"HIGH\",\"shippingCouponCode\":\"SHIP\"}";
        mvc.perform(csrf(post("/api/customer/orders/checkout"),"customer").content(request)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("mã voucher không phù hợp"));
    }
    @Test
    @Transactional(propagation=org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    void concurrentTransactionsCannotConsumeLastUseTwice() throws Exception {
        var c=coupon("RACE-"+id(),CouponTarget.ORDER,"10","20000"); c.setUsageLimit(1); coupons.saveAndFlush(c);
        var pool=java.util.concurrent.Executors.newFixedThreadPool(2);
        var start=new java.util.concurrent.CountDownLatch(1);
        var context=ctx();
        try {
            java.util.concurrent.Callable<Boolean> attempt=() -> {
                start.await();
                try {
                    return new org.springframework.transaction.support.TransactionTemplate(transactionManager).execute(tx -> {
                        service.apply(new Order(),context,c.getCode(),null);
                        return true;
                    });
                } catch(ApiException e) { return false; }
            };
            var first=pool.submit(attempt); var second=pool.submit(attempt); start.countDown();
            assertThat(List.of(first.get(15,java.util.concurrent.TimeUnit.SECONDS),second.get(15,java.util.concurrent.TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(true,false);
            assertThat(coupons.findById(c.getId()).orElseThrow().getUsedCount()).isEqualTo(1);
        } finally { pool.shutdownNow(); coupons.deleteById(c.getId()); }
    }
}
