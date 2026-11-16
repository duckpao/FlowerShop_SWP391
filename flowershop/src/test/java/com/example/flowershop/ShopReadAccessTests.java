package com.example.flowershop;

import com.example.flowershop.entity.Shop;
import com.example.flowershop.entity.User;
import com.example.flowershop.entity.enums.ShopStatus;
import com.example.flowershop.entity.enums.UserRole;
import com.example.flowershop.entity.enums.UserStatus;
import com.example.flowershop.repository.ShopRepository;
import com.example.flowershop.repository.ShopStaffRepository;
import com.example.flowershop.repository.UserRepository;
import com.example.flowershop.service.ShopOperationGuard;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/** Xem danh sách của shop chạy trong transaction chỉ đọc nên không được khóa dòng shop (SELECT ... FOR UPDATE). */
class ShopReadAccessTests {
    private final ShopRepository shops = mock(ShopRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final ShopOperationGuard guard = new ShopOperationGuard(shops, mock(ShopStaffRepository.class), users);

    private final User owner = User.builder().id("o-1").role(UserRole.SHOP).status(UserStatus.ACTIVE).isEmailVerified(true).build();
    private final Shop shop = Shop.builder().id("s-1").owner(owner).status(ShopStatus.ACTIVE).build();

    @Test
    void readAccessDoesNotLockTheShopRow() {
        when(shops.findById("s-1")).thenReturn(Optional.of(shop));
        when(users.findById("o-1")).thenReturn(Optional.of(owner));

        assertThat(guard.requireReadAccess("s-1", "o-1")).isSameAs(shop);
        verify(shops, never()).findForUpdate(anyString());
    }

    @Test
    void writeAccessStillLocksTheShopRow() {
        when(shops.findForUpdate("s-1")).thenReturn(Optional.of(shop));
        when(users.findById("o-1")).thenReturn(Optional.of(owner));

        guard.requireAccess("s-1", "o-1");
        verify(shops).findForUpdate("s-1");
    }

    @Test
    void readAccessStillRejectsOtherUsersAndInactiveShops() {
        when(shops.findById("s-1")).thenReturn(Optional.of(shop));
        User other = User.builder().id("x").role(UserRole.SHOP).status(UserStatus.ACTIVE).isEmailVerified(true).build();
        when(users.findById("x")).thenReturn(Optional.of(other));
        assertThatThrownBy(() -> guard.requireReadAccess("s-1", "x")).isInstanceOf(ResponseStatusException.class);

        shop.setStatus(ShopStatus.INACTIVE);
        assertThatThrownBy(() -> guard.requireReadAccess("s-1", "o-1")).isInstanceOf(ResponseStatusException.class);
    }
}
