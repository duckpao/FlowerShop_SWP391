package com.example.flowershop;

import com.example.flowershop.entity.*;
import com.example.flowershop.entity.enums.*;
import com.example.flowershop.exception.ApiException;
import com.example.flowershop.repository.*;
import com.example.flowershop.service.*;
import com.example.flowershop.service.CustomerRefundService.RefundInput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** UC23: khách gửi/hủy yêu cầu hoàn tiền; chủ shop, admin, staff xử lý; người lạ bị chặn. */
class CustomerRefundTests {
    private final OrderRepository orders = mock(OrderRepository.class);
    private final PaymentRepository payments = mock(PaymentRepository.class);
    private final RefundRepository refunds = mock(RefundRepository.class);
    private final ShopRepository shops = mock(ShopRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final ShopStaffRepository staffRepo = mock(ShopStaffRepository.class);
    private final NotificationService notifications = mock(NotificationService.class);
    private final CustomerRefundService customer = new CustomerRefundService(orders, payments, refunds,
            mock(CloudinaryService.class), notifications);
    private final StaffRefundService handler = new StaffRefundService(refunds,
            new ShopOperationGuard(shops, staffRepo, users), notifications);
    private final List<Refund> existing = new ArrayList<>();

    private final User owner = user("owner-1", UserRole.SHOP);
    private final Shop shop = Shop.builder().id("shop-1").name("Hoa Xuân").owner(owner).status(ShopStatus.ACTIVE).build();
    private Order order;

    private static User user(String id, UserRole role) {
        return User.builder().id(id).role(role).isEmailVerified(true).status(UserStatus.ACTIVE).build();
    }

    @BeforeEach
    void setUp() {
        order = Order.builder().id("o-1").shop(shop).status(OrderStatus.COMPLETED)
                .customer(User.builder().id("c-1").fullName("Khách").phone("0901234567").build())
                .totalAmount(new BigDecimal("300000")).build();
        when(orders.findById("o-1")).thenReturn(Optional.of(order));
        when(payments.findByOrderId("o-1")).thenReturn(List.of(
                Payment.builder().id("p-1").amount(new BigDecimal("300000")).status(PaymentStatus.SUCCESS).build(),
                Payment.builder().id("p-2").amount(new BigDecimal("999999")).status(PaymentStatus.FAILED).build()));
        when(refunds.findByOrderIdOrderByCreatedDateAsc("o-1")).thenReturn(existing);
        when(refunds.save(any(Refund.class))).thenAnswer(inv -> inv.getArgument(0));
        when(shops.findForUpdate("shop-1")).thenReturn(Optional.of(shop));
        when(users.findById("owner-1")).thenReturn(Optional.of(owner));
        when(users.findById("stranger")).thenReturn(Optional.of(user("stranger", UserRole.SHOP)));
    }

    private Refund refund(RefundStatus status) {
        Refund r = Refund.builder().id("r-1").order(order).amount(new BigDecimal("90000")).reason("x").status(status).build();
        when(refunds.findById("r-1")).thenReturn(Optional.of(r));
        return r;
    }

    @Test
    void customerRequestsRefundForPaidAmountAndShopIsNotified() {
        var result = customer.request("c-1", "o-1", new RefundInput("Hoa bị dập, héo", "Hoa héo", List.of("https://img/1.png")));
        assertThat(result.amount()).isEqualByComparingTo("300000");
        assertThat(result.status()).isEqualTo("REQUESTED");
        verify(refunds).save(argThat(r -> "Hoa héo".equals(r.getDescription())
                && r.getEvidenceImages().contains("https://img/1.png") && "c-1".equals(r.getCreatedBy())));
        verify(notifications).notifyRefundRequested(any());
    }

    @Test
    void amountNeverExceedsPaidMinusProcessedRefunds() {
        existing.add(Refund.builder().id("old").order(order).amount(new BigDecimal("100000")).reason("x").status(RefundStatus.PROCESSED).build());
        assertThat(customer.request("c-1", "o-1", new RefundInput("Giao hàng trễ", "", List.of())).amount()).isEqualByComparingTo("200000");
        existing.clear();
        existing.add(Refund.builder().id("all").order(order).amount(new BigDecimal("300000")).reason("x").status(RefundStatus.PROCESSED).build());
        assertThatThrownBy(() -> customer.request("c-1", "o-1", new RefundInput("Đổi ý", "", List.of())))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void rejectsDuplicateOpenRequestMissingEvidenceAndNonRefundableOrder() {
        assertThatThrownBy(() -> customer.request("c-1", "o-1", new RefundInput("Hoa bị dập, héo", "", List.of())))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> customer.request("c-1", "o-1", new RefundInput("Khác", " ", List.of())))
                .isInstanceOf(ApiException.class);
        existing.add(Refund.builder().id("open").order(order).amount(BigDecimal.TEN).reason("x").status(RefundStatus.REQUESTED).build());
        assertThatThrownBy(() -> customer.request("c-1", "o-1", new RefundInput("Đổi ý", "", List.of())))
                .isInstanceOf(ApiException.class);
        existing.clear();
        order.setStatus(OrderStatus.PROCESSING);
        assertThatThrownBy(() -> customer.request("c-1", "o-1", new RefundInput("Đổi ý", "", List.of())))
                .isInstanceOf(ApiException.class);
        verify(refunds, never()).save(any());
    }

    @Test
    void customerCannotUseSomeoneElsesOrder() {
        assertThatThrownBy(() -> customer.request("other", "o-1", new RefundInput("Đổi ý", "", List.of())))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getStatus().value()).isEqualTo(404));
        refund(RefundStatus.REQUESTED);
        assertThatThrownBy(() -> customer.cancel("other", "o-1", "r-1")).isInstanceOf(ApiException.class);
    }

    @Test
    void customerCancelsOnlyPendingRequest() {
        Refund pending = refund(RefundStatus.REQUESTED);
        assertThat(customer.cancel("c-1", "o-1", "r-1").status()).isEqualTo("CANCELLED");
        assertThat(pending.getStatus()).isEqualTo(RefundStatus.CANCELLED);
        assertThatThrownBy(() -> customer.cancel("c-1", "o-1", "r-1")).isInstanceOf(ApiException.class);
        refund(RefundStatus.APPROVED);
        assertThatThrownBy(() -> customer.cancel("c-1", "o-1", "r-1")).isInstanceOf(ApiException.class);
    }

    @Test
    void shopOwnerApprovesThenProcesses() {
        Refund r = refund(RefundStatus.REQUESTED);
        assertThat(handler.updateStatus("shop-1", "owner-1", "r-1", "APPROVED", null).status()).isEqualTo("APPROVED");
        assertThat(handler.updateStatus("shop-1", "owner-1", "r-1", "PROCESSED", null).status()).isEqualTo("PROCESSED");
        verify(notifications).notifyRefundResolved(r);
    }

    @Test
    void rejectingNeedsResponseAndStoresIt() {
        Refund r = refund(RefundStatus.REQUESTED);
        assertThatThrownBy(() -> handler.updateStatus("shop-1", "owner-1", "r-1", "REJECTED", "  "))
                .isInstanceOf(ResponseStatusException.class);
        assertThat(r.getStatus()).isEqualTo(RefundStatus.REQUESTED);
        var out = handler.updateStatus("shop-1", "owner-1", "r-1", "REJECTED", "Hoa không có lỗi");
        assertThat(out.shopResponse()).isEqualTo("Hoa không có lỗi");
        assertThat(r.getShopResponse()).isEqualTo("Hoa không có lỗi");
    }

    @Test
    void adminListsAndProcessesAnyShop() {
        Refund r = refund(RefundStatus.REQUESTED);
        assertThat(handler.adminUpdateStatus("admin-1", "r-1", "approved", null).status()).isEqualTo("APPROVED");
        assertThat(r.getLastModifyBy()).isEqualTo("admin-1");
        assertThatThrownBy(() -> handler.adminUpdateStatus("admin-1", "r-1", "REJECTED", ""))
                .isInstanceOf(ResponseStatusException.class);
        assertThat(handler.adminUpdateStatus("admin-1", "r-1", "REJECTED", "Không đủ điều kiện").shopResponse())
                .isEqualTo("Không đủ điều kiện");
        when(refunds.findByStatus(eq(RefundStatus.REJECTED), any())).thenReturn(
                new org.springframework.data.domain.PageImpl<>(List.of(r)));
        assertThat(handler.adminList("rejected", 0).content()).hasSize(1);
        assertThatThrownBy(() -> handler.adminUpdateStatus("admin-1", "none", "APPROVED", null))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void staffOfThisShopStillWorksAndStrangersAreBlocked() {
        Refund r = refund(RefundStatus.REQUESTED);
        User staff = user("staff-1", UserRole.SHOP_STAFF);
        when(users.findById("staff-1")).thenReturn(Optional.of(staff));
        ShopStaff member = new ShopStaff();
        member.setId("ss-1");
        member.setShop(shop);
        member.setUser(staff);
        member.setActive(true);
        when(staffRepo.findByShopIdAndUserId("shop-1", "staff-1")).thenReturn(Optional.of(member));
        assertThat(handler.updateStatus("shop-1", "staff-1", "r-1", "APPROVED").status()).isEqualTo("APPROVED");

        assertThatThrownBy(() -> handler.updateStatus("shop-1", "stranger", "r-1", "PROCESSED", null))
                .isInstanceOfSatisfying(ResponseStatusException.class, e -> assertThat(e.getStatusCode().value()).isEqualTo(403));
        assertThatThrownBy(() -> handler.list("shop-1", "stranger"))
                .isInstanceOfSatisfying(ResponseStatusException.class, e -> assertThat(e.getStatusCode().value()).isEqualTo(403));
        assertThat(r.getStatus()).isEqualTo(RefundStatus.APPROVED);
    }

    @Test
    void ownerCannotReachRefundOfAnotherShop() {
        refund(RefundStatus.REQUESTED);
        Shop other = Shop.builder().id("shop-2").owner(owner).status(ShopStatus.ACTIVE).build();
        when(shops.findForUpdate("shop-2")).thenReturn(Optional.of(other));
        assertThatThrownBy(() -> handler.updateStatus("shop-2", "owner-1", "r-1", "APPROVED", null))
                .isInstanceOfSatisfying(ResponseStatusException.class, e -> assertThat(e.getStatusCode().value()).isEqualTo(404));
    }
}
