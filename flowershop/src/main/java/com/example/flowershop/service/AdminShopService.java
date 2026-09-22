package com.example.flowershop.service;

import com.example.flowershop.dto.account.ShopResponse;
import com.example.flowershop.entity.Shop;
import com.example.flowershop.entity.enums.*;
import com.example.flowershop.repository.ShopRepository;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

@Service
@Transactional(readOnly=true)
public class AdminShopService {
    private final ShopRepository shops;
    public AdminShopService(ShopRepository shops) { this.shops=shops; }
    public record ShopPage(List<ShopResponse> content,int page,int size,long totalElements,int totalPages) {}
    public ShopPage list(String q,String status,int page,int size) {
        if (page<0 || page>100000 || size<1 || size>100 || q.length()>100)
            throw new IllegalArgumentException("Trang từ 0 đến 100000, kích thước từ 1 đến 100, từ khóa tối đa 100 ký tự.");
        ShopStatus filter=null;
        if (!status.isBlank()) {
            try { filter=ShopStatus.valueOf(status); }
            catch (IllegalArgumentException e) { throw new IllegalArgumentException("Trạng thái shop không hợp lệ."); }
        }
        var selected=filter;
        String term=q.strip().toLowerCase(Locale.ROOT).replace("!","!!").replace("%","!%").replace("_","!_");
        Specification<Shop> spec=(root,query,cb)-> {
            var predicates=new ArrayList<jakarta.persistence.criteria.Predicate>();
            if (selected!=null) predicates.add(cb.equal(root.get("status"),selected));
            if (!term.isEmpty()) predicates.add(cb.like(cb.lower(root.get("name")),"%"+term+"%",'!'));
            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
        var result=shops.findAll(spec,PageRequest.of(page,size,Sort.by(Sort.Order.desc("createdDate"),Sort.Order.asc("id"))));
        return new ShopPage(result.getContent().stream().map(ShopResponse::from).toList(),page,size,result.getTotalElements(),result.getTotalPages());
    }
    public ShopResponse detail(String id) { return ShopResponse.from(shops.findById(id).orElseThrow(AdminShopService::missing)); }
    @Transactional
    public ShopResponse transition(String id,String action,String actor) {
        Shop shop=shops.findForUpdate(id).orElseThrow(AdminShopService::missing);
        ShopStatus expected,target;
        switch(action) {
            case "approve" -> { expected=ShopStatus.PENDING; target=ShopStatus.ACTIVE; }
            case "block" -> { expected=ShopStatus.ACTIVE; target=ShopStatus.BANNED; }
            case "unblock" -> { expected=ShopStatus.BANNED; target=ShopStatus.ACTIVE; }
            default -> throw new IllegalArgumentException("Thao tác không hợp lệ.");
        }
        if(shop.getStatus()!=expected) throw new ResponseStatusException(HttpStatus.CONFLICT,"Trạng thái shop đã thay đổi hoặc không phù hợp thao tác. Hãy tải lại.");
        if(target==ShopStatus.ACTIVE) {
            var owner=shop.getOwner();
            if(owner.getRole()!=UserRole.SHOP || owner.getStatus()!=UserStatus.ACTIVE || !Boolean.TRUE.equals(owner.getIsEmailVerified()))
                throw new ResponseStatusException(HttpStatus.CONFLICT,"Chủ shop phải có role SHOP, đang hoạt động và đã xác thực email.");
        }
        shop.setStatus(target); shop.setLastModifyBy(actor);
        shops.flush();
        return ShopResponse.from(shop);
    }
    private static ResponseStatusException missing() { return new ResponseStatusException(HttpStatus.NOT_FOUND,"Không tìm thấy cửa hàng."); }
}
