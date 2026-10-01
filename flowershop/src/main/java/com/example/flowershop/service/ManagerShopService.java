package com.example.flowershop.service;

import com.example.flowershop.dto.account.ShopResponse;
import com.example.flowershop.entity.*;
import com.example.flowershop.entity.enums.*;
import com.example.flowershop.repository.*;
import jakarta.validation.constraints.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.*;

@Service @Transactional(readOnly=true)
public class ManagerShopService {
    public record Profile(@NotBlank @Size(max=255) String name,@NotNull @Size(max=5000) String description,
                          @NotNull @Size(max=255) @Pattern(regexp="^$|https://[^\\s]+",message="Logo phải là URL HTTPS hoặc để trống") String logoUrl) {}
    public record ActiveInput(@NotNull Boolean active) {}
    public record StaffResponse(String userId,String email,String fullName,boolean active) {
        static StaffResponse from(ShopStaff s) { return new StaffResponse(s.getUser().getId(),s.getUser().getEmail(),s.getUser().getFullName(),s.isActive()); }
    }
    private final ShopRepository shops;
    private final ShopStaffRepository staff;
    private final AddressRepository addresses;
    public ManagerShopService(ShopRepository shops,ShopStaffRepository staff,AddressRepository addresses) {
        this.shops=shops; this.staff=staff; this.addresses=addresses;
    }
    public List<com.example.flowershop.dto.account.AddressResponse> address(String id,String actor) {
        owned(id,actor,false); return addresses.findByShopIdAndUserIsNullOrderByCreatedDateAscIdAsc(id).stream().map(com.example.flowershop.dto.account.AddressResponse::from).toList();
    }
    @Transactional public com.example.flowershop.dto.account.AddressResponse saveAddress(String id,String actor,com.example.flowershop.dto.account.AddressRequest input) {
        var shop=owned(id,actor,true);
        var list=addresses.findByShopIdAndUserIsNullOrderByCreatedDateAscIdAsc(id);
        var a=list.stream().filter(x->Boolean.TRUE.equals(x.getIsDefault())).findFirst().orElseGet(()->list.isEmpty()?new Address():list.get(0));
        if(a.getId()==null) { a.setId(UUID.randomUUID().toString()); a.setShop(shop); a.setCreatedBy(actor); }
        list.forEach(x->x.setIsDefault(false));
        a.setAddressLine(input.addressLine().strip()); a.setCity(input.city().strip()); a.setDistrict(input.district().strip()); a.setWard(input.ward().strip());
        a.setGhnWardCode(input.ghnWardCode().strip()); a.setGhnDistrictId(input.ghnDistrictId());
        a.setIsDefault(true); a.setLastModifyBy(actor);
        return com.example.flowershop.dto.account.AddressResponse.from(addresses.save(a));
    }
    public List<ShopResponse> mine(String actor) { return shops.findByOwnerIdOrderByNameAsc(actor).stream().map(ShopResponse::from).toList(); }
    private Shop owned(String id,String actor,boolean write) {
        Shop s=(write?shops.findForUpdate(id):shops.findById(id)).orElseThrow(ManagerShopService::missing);
        if(!s.getOwner().getId().equals(actor)) throw missing();
        if(write && s.getStatus()!=ShopStatus.ACTIVE && s.getStatus()!=ShopStatus.PENDING)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Shop bị khóa hoặc ngừng hoạt động, chỉ được xem.");
        return s;
    }
    @Transactional public ShopResponse update(String id,String actor,Profile input) {
        var s=owned(id,actor,true); s.setName(input.name().strip()); s.setDescription(input.description().strip());
        s.setLogoUrl(input.logoUrl().strip()); s.setLastModifyBy(actor); shops.flush(); return ShopResponse.from(s);
    }
    public List<StaffResponse> staff(String id,String actor) { owned(id,actor,false); return staff.findByShopIdOrderByIdAsc(id).stream().map(StaffResponse::from).toList(); }
    @Transactional public StaffResponse active(String id,String actor,String userId,boolean active) {
        var shop=owned(id,actor,true);
        if(shop.getStatus()!=ShopStatus.ACTIVE) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Shop phải đang hoạt động.");
        var member=staff.findByShopIdAndUserId(id,userId).orElseThrow(ManagerShopService::missing);
        if(active && (member.getUser().getRole()!=UserRole.SHOP_STAFF || member.getUser().getStatus()!=UserStatus.ACTIVE || !Boolean.TRUE.equals(member.getUser().getIsEmailVerified())))
            throw new IllegalArgumentException("Tài khoản nhân viên chưa đủ điều kiện.");
        member.setActive(active); return StaffResponse.from(member);
    }
    public List<ShopResponse> assigned(String actor) {
        return staff.findByUserIdAndActiveTrue(actor).stream().map(s->ShopResponse.from(s.getShop())).toList();
    }
    private static ResponseStatusException missing() { return new ResponseStatusException(HttpStatus.NOT_FOUND,"Không tìm thấy cửa hàng hoặc nhân viên."); }
}
