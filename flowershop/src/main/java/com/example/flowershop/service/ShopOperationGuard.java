package com.example.flowershop.service;

import com.example.flowershop.entity.Shop;
import com.example.flowershop.entity.enums.ShopStatus;
import com.example.flowershop.repository.ShopRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/** Business services must call within their write transaction, after checking shop ownership/membership. */
@Service
public class ShopOperationGuard {
    private final ShopRepository shops;
    private final com.example.flowershop.repository.ShopStaffRepository staff;
    private final com.example.flowershop.repository.UserRepository users;
    public ShopOperationGuard(ShopRepository shops,com.example.flowershop.repository.ShopStaffRepository staff,com.example.flowershop.repository.UserRepository users) { this.shops=shops; this.staff=staff; this.users=users; }
    @Transactional(propagation=Propagation.MANDATORY)
    public Shop requireAccess(String shopId,String actorId) {
        Shop shop=requireActive(shopId);
        var user=users.findById(actorId).orElseThrow(()->new ResponseStatusException(HttpStatus.FORBIDDEN));
        boolean allowed=user.getStatus()==com.example.flowershop.entity.enums.UserStatus.ACTIVE && Boolean.TRUE.equals(user.getIsEmailVerified()) &&
                ((user.getRole()==com.example.flowershop.entity.enums.UserRole.SHOP && shop.getOwner().getId().equals(actorId)) ||
                (user.getRole()==com.example.flowershop.entity.enums.UserRole.SHOP_STAFF && staff.findByShopIdAndUserId(shopId,actorId).map(com.example.flowershop.entity.ShopStaff::isActive).orElse(false)));
        if(!allowed) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Không có quyền làm việc tại cửa hàng này.");
        return shop;
    }
    @Transactional(propagation=Propagation.MANDATORY)
    public Shop requireActive(String shopId) {
        Shop shop=shops.findForUpdate(shopId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Không tìm thấy cửa hàng."));
        if(shop.getStatus()!=ShopStatus.ACTIVE)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Cửa hàng chưa được phép hoạt động.");
        return shop;
    }
}
