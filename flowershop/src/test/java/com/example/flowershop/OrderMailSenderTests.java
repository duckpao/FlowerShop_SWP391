package com.example.flowershop;

import com.example.flowershop.entity.*;
import com.example.flowershop.entity.enums.OrderStatus;
import com.example.flowershop.event.OrderStatusChangedEvent;
import com.example.flowershop.repository.*;
import com.example.flowershop.service.HtmlMailSender;
import com.example.flowershop.service.OrderMailSender;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.util.HtmlUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/** Nội dung hai thư của đơn: xác nhận (PROCESSING) và giao thành công (COMPLETED). */
class OrderMailSenderTests {

    private static final String SHOP_PHONE = "0911222333";
    private static final String RECIPIENT_PHONE = "0987654321";

    private final OrderRepository orders = mock(OrderRepository.class);
    private final OrderDetailRepository details = mock(OrderDetailRepository.class);
    private final AddressRepository addresses = mock(AddressRepository.class);
    private final ProductReviewRepository reviews = mock(ProductReviewRepository.class);
    private final JavaMailSender javaMail = mock(JavaMailSender.class);
    private OrderMailSender sender;
    private Order order;
    private Product rose;
    private Product lily;

    @BeforeEach
    void setUp() {
        when(javaMail.createMimeMessage()).thenAnswer(i -> new MimeMessage((Session) null));
        HtmlMailSender html = new HtmlMailSender(javaMail, "shop@example.test", "http://localhost:5173");
        sender = new OrderMailSender(orders, details, mock(PaymentRepository.class), mock(DeliveryRepository.class),
                addresses, reviews, html, mock(PlatformTransactionManager.class));

        User customer = User.builder().id("cus-1").email("khach@example.test").fullName("Trần Thị B")
                .phone("0900000000").isEmailVerified(true).build();
        Shop shop = Shop.builder().id("shop-1").name("FPTU Smart Floral").build();
        Address to = Address.builder().id("addr-1").user(customer).addressLine("Số 10, Ngõ 20, Đường Cầu Giấy")
                .ward("Dịch Vọng").district("Cầu Giấy").city("Hà Nội").recipientPhone(RECIPIENT_PHONE).build();
        order = Order.builder().id("11111111-2222-3333-4444-555555555555").customer(customer).shop(shop)
                .deliveryAddress(to).recipientName("Lê Văn C").recipientPhone(RECIPIENT_PHONE)
                .subTotal(new BigDecimal("700000")).totalAmount(new BigDecimal("650000")).build();
        when(orders.findById(order.getId())).thenReturn(Optional.of(order));

        Address pickup = Address.builder().id("addr-shop").shop(shop).addressLine("Khu Công Nghệ Cao Hòa Lạc")
                .ward("Thạch Hòa").district("Thạch Thất").city("Hà Nội").recipientPhone(SHOP_PHONE)
                .isDefault(true).build();
        when(addresses.findByShopIdAndUserIsNullOrderByCreatedDateAscIdAsc("shop-1")).thenReturn(List.of(pickup));

        rose = Product.builder().id("prod-rose").name("Bó Hồng Đỏ Mix Baby").build();
        lily = Product.builder().id("prod-lily").name("Giỏ Hoa Ly").build();
        when(details.findByOrderId(order.getId())).thenReturn(List.of(
                OrderDetail.builder().id("d1").order(order).product(rose).price(new BigDecimal("350000")).quantity(1).build(),
                OrderDetail.builder().id("d2").order(order).product(lily).price(new BigDecimal("350000")).quantity(1).build(),
                OrderDetail.builder().id("d3").order(order).product(null).price(new BigDecimal("1000")).quantity(1).build()));
        when(reviews.findByProductIdAndUserId(anyString(), anyString())).thenReturn(Optional.empty());
    }

    private String mailAfter(OrderStatus from, OrderStatus to) throws Exception {
        sender.onStatusChanged(new OrderStatusChangedEvent(order.getId(), from, to));
        ArgumentCaptor<MimeMessage> sent = ArgumentCaptor.forClass(MimeMessage.class);
        verify(javaMail).send(sent.capture());
        // esc() mã hóa chữ có dấu thành entity; giải mã lại để so với nội dung người đọc thấy.
        return HtmlUtils.htmlUnescape((String) sent.getValue().getContent());
    }

    @Test
    void confirmationMailShowsBothPlacesWithoutPhones() throws Exception {
        String html = mailAfter(OrderStatus.PENDING, OrderStatus.PROCESSING);

        assertThat(html).contains("Nhận tại", "Lê Văn C", "Số 10, Ngõ 20, Đường Cầu Giấy, Dịch Vọng, Cầu Giấy, Hà Nội");
        assertThat(html).contains("Gửi từ", "FPTU Smart Floral", "Thạch Thất, Hà Nội");
        assertThat(html).doesNotContain(RECIPIENT_PHONE, SHOP_PHONE, "0900000000");
        assertThat(html).doesNotContain("Khu Công Nghệ Cao Hòa Lạc").doesNotContain("Thạch Hòa");
        assertThat(html).doesNotContain("/products/");
    }

    @Test
    void deliveredMailAddsPlacesAndOneReviewLinkPerUnreviewedProduct() throws Exception {
        when(reviews.findByProductIdAndUserId("prod-lily", "cus-1")).thenReturn(Optional.of(new ProductReview()));

        String html = mailAfter(OrderStatus.DELIVERING, OrderStatus.COMPLETED);

        assertThat(html).contains("Giao hàng thành công", "Nhận tại", "Gửi từ", "Thạch Thất, Hà Nội");
        assertThat(html).contains("http://localhost:5173/products/prod-rose", "Đánh giá Bó Hồng Đỏ Mix Baby");
        assertThat(html).doesNotContain("prod-lily");
        assertThat(html).doesNotContain(RECIPIENT_PHONE, SHOP_PHONE);
        assertThat(html).doesNotContain("—");
    }

    @Test
    void deliveredMailDropsReviewBlockWhenEverythingIsReviewed() throws Exception {
        when(reviews.findByProductIdAndUserId(anyString(), anyString())).thenReturn(Optional.of(new ProductReview()));

        String html = mailAfter(OrderStatus.DELIVERING, OrderStatus.COMPLETED);

        assertThat(html).doesNotContain("/products/").doesNotContain("Đánh giá");
        assertThat(html).contains("Xem đơn hàng");
    }

    @Test
    void mailStillGoesOutWhenShopHasNoAddress() throws Exception {
        when(addresses.findByShopIdAndUserIsNullOrderByCreatedDateAscIdAsc("shop-1")).thenReturn(List.of());

        String html = mailAfter(OrderStatus.PENDING, OrderStatus.PROCESSING);

        assertThat(html).contains("Gửi từ", "FPTU Smart Floral").doesNotContain("Thạch Thất");
    }
}
