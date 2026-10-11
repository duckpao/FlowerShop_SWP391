package com.example.flowershop;

import com.example.flowershop.entity.Address;
import com.example.flowershop.entity.Shop;
import com.example.flowershop.entity.User;
import com.example.flowershop.exception.ApiException;
import com.example.flowershop.repository.ShopRepository;
import com.example.flowershop.service.GhnService;
import com.example.flowershop.service.ShopGhnStoreService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ShopGhnStoreServiceTests {
    private final GhnService ghn = mock(GhnService.class);
    private final ShopRepository shops = mock(ShopRepository.class);
    private final ShopGhnStoreService service = new ShopGhnStoreService(ghn, shops);

    private Shop shop(String ownerPhone) {
        return Shop.builder().id("shop-1").name("Shop Một")
                .owner(User.builder().id("owner-1").phone(ownerPhone).build()).build();
    }

    private Address pickup() {
        Address a = new Address();
        a.setAddressLine("1 Tràng Tiền");
        a.setWard("Phường Tràng Tiền");
        a.setDistrict("Quận Hoàn Kiếm");
        a.setCity("Hà Nội");
        a.setGhnDistrictId(1489);
        a.setGhnWardCode("1A0218");
        return a;
    }

    @Test
    void registersStoreOnceAndSavesIdOnShop() {
        Shop shop = shop("0987654321");
        when(ghn.registerStore(anyString(), anyString(), anyString(), anyInt(), anyString())).thenReturn(4242);

        assertThat(service.ensureStore(shop, pickup())).isEqualTo(4242);
        assertThat(shop.getGhnShopId()).isEqualTo(4242);
        verify(shops).save(shop);

        // Đã có mã thì không đăng ký lại.
        assertThat(service.ensureStore(shop, pickup())).isEqualTo(4242);
        verify(ghn, times(1)).registerStore(anyString(), anyString(), anyString(), anyInt(), anyString());
    }

    @Test
    void registerSendsPickupAddressAndOwnerPhone() {
        when(ghn.registerStore(anyString(), anyString(), anyString(), anyInt(), anyString())).thenReturn(1);

        service.registerStore(shop("0987654321"), pickup());

        verify(ghn).registerStore("Shop Một", "0987654321",
                "1 Tràng Tiền, Phường Tràng Tiền, Quận Hoàn Kiếm, Hà Nội", 1489, "1A0218");
    }

    @Test
    void rejectsShopWithoutPhoneOrWithoutGhnCodes() {
        assertThatThrownBy(() -> service.registerStore(shop(null), pickup())).isInstanceOf(ApiException.class);
        Address noCodes = pickup();
        noCodes.setGhnWardCode(null);
        assertThatThrownBy(() -> service.registerStore(shop("0987654321"), noCodes)).isInstanceOf(ApiException.class);
        verifyNoInteractions(ghn);
    }
}
