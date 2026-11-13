package com.example.flowershop;

import com.example.flowershop.dto.account.AddressRequest;
import com.example.flowershop.entity.Address;
import com.example.flowershop.entity.Order;
import com.example.flowershop.entity.User;
import com.example.flowershop.repository.AddressRepository;
import com.example.flowershop.repository.OrderRepository;
import com.example.flowershop.repository.ShopRepository;
import com.example.flowershop.repository.UserRepository;
import com.example.flowershop.service.AccountService;
import com.example.flowershop.service.CloudinaryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Địa chỉ gắn với đơn hàng: sửa được khi mọi đơn dùng nó chưa tạo vận đơn (chưa đến DELIVERING);
 * bị khóa khi có đơn đã tạo vận đơn hoặc đã giao; xóa vẫn bị chặn khi còn đơn trỏ tới.
 */
class AccountAddressEditTests {

    private UserRepository users;
    private AddressRepository addresses;
    private OrderRepository orders;
    private AccountService service;
    private Address address;

    @BeforeEach
    void setUp() {
        users = mock(UserRepository.class);
        addresses = mock(AddressRepository.class);
        orders = mock(OrderRepository.class);
        var user = User.builder().id("cus-1").build();
        when(users.findByIdForUpdate("cus-1")).thenReturn(Optional.of(user));
        address = Address.builder().id("addr-1").user(user).addressLine("Số 1").city("Hà Nội").district("Cầu Giấy")
                .ward("Dịch Vọng").ghnDistrictId(1).ghnWardCode("1A").recipientName("Lan").recipientPhone("0900000001")
                .addressType("HOME").isDefault(true).build();
        when(addresses.findByUserIdAndShopIsNullOrderByCreatedDateAscIdAsc("cus-1")).thenReturn(new ArrayList<>(List.of(address)));
        when(addresses.save(any())).thenAnswer(i -> i.getArgument(0));
        service = new AccountService(users, addresses, orders, mock(CloudinaryService.class), mock(ShopRepository.class));
    }

    private static AddressRequest edit(String line, String name, String phone) {
        return new AddressRequest(line, "Hà Nội", "Cầu Giấy", "Dịch Vọng", true, name, phone, null, null, "HOME", 1, "1A");
    }

    private static Order orderWith(String name, String phone) {
        var order = new Order();
        order.setId("o-1");
        order.setRecipientName(name);
        order.setRecipientPhone(phone);
        return order;
    }

    @Test
    void addressCanBeEditedWhileEveryOrderUsingItHasNoWaybill() {
        when(orders.existsShippedByDeliveryAddressId("addr-1")).thenReturn(false);

        var response = service.save("cus-1", "addr-1", edit("Số 99 Phố Mới", "Lan", "0900000001"));

        assertEquals("Số 99 Phố Mới", response.addressLine());
        assertEquals("Số 99 Phố Mới", address.getAddressLine());
    }

    @Test
    void editIsRefusedOnceAnOrderUsingItHasAWaybillOrIsDelivering() {
        when(orders.existsShippedByDeliveryAddressId("addr-1")).thenReturn(true);

        var e = assertThrows(ResponseStatusException.class,
                () -> service.save("cus-1", "addr-1", edit("Số 99 Phố Mới", "Lan", "0900000001")));

        assertEquals(409, e.getStatusCode().value());
        assertTrue(e.getReason().contains("vận đơn"));
        assertEquals("Số 1", address.getAddressLine(), "address must stay untouched");
    }

    @Test
    void recipientOfUnshippedOrdersFollowsTheAddressWhenItHadFollowedItBefore() {
        var following = orderWith("Lan", "0900000001");
        var customised = orderWith("Mẹ của Lan", "0911111111");
        when(orders.findNotShippedByDeliveryAddressId("addr-1")).thenReturn(List.of(following, customised));

        service.save("cus-1", "addr-1", edit("Số 1", "Lan Anh", "0900000002"));

        assertEquals("Lan Anh", following.getRecipientName());
        assertEquals("0900000002", following.getRecipientPhone());
        assertEquals("Mẹ của Lan", customised.getRecipientName(), "value typed for this order is kept");
        assertEquals("0911111111", customised.getRecipientPhone());
    }

    @Test
    void newAddressNeverChecksOrders() {
        service.save("cus-1", null, edit("Số 5", "Lan", "0900000001"));
        verify(orders, never()).existsShippedByDeliveryAddressId(anyString());
        verify(orders, never()).findNotShippedByDeliveryAddressId(anyString());
    }

    @Test
    void deleteStaysBlockedWhileAnyOrderPointsToTheAddress() {
        when(orders.existsByDeliveryAddressId("addr-1")).thenReturn(true);

        var e = assertThrows(ResponseStatusException.class, () -> service.delete("cus-1", "addr-1"));

        assertEquals(409, e.getStatusCode().value());
        verify(addresses, never()).delete(any());
    }
}
