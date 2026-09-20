package com.example.flowershop.service;

import com.example.flowershop.dto.account.CustomerResponse;
import com.example.flowershop.entity.User;
import com.example.flowershop.entity.enums.*;
import com.example.flowershop.repository.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

@Service
@Transactional(readOnly=true)
public class AdminCustomerService {
    private final UserRepository users;
    private final AuthSessionRepository sessions;
    public AdminCustomerService(UserRepository users,AuthSessionRepository sessions) { this.users=users; this.sessions=sessions; }
    public record CustomerPage(List<CustomerResponse> content, int page, int size, long totalElements, int totalPages) {}

    public CustomerPage list(String query, String status, int page, int size) {
        if (page<0 || page>100000 || size<1 || size>100 || query.length()>100)
            throw new IllegalArgumentException("Trang phải từ 0 đến 100000, kích thước từ 1 đến 100, từ khóa tối đa 100 ký tự.");
        UserStatus filter=null;
        if (!status.isBlank()) {
            try { filter=UserStatus.valueOf(status); }
            catch (IllegalArgumentException e) { throw new IllegalArgumentException("Trạng thái không hợp lệ."); }
        }
        final UserStatus selected=filter;
        String term=query.strip().toLowerCase(Locale.ROOT).replace("!","!!").replace("%","!%").replace("_","!_");
        Specification<User> spec=(root,cq,cb) -> {
            var predicates=new ArrayList<jakarta.persistence.criteria.Predicate>();
            predicates.add(cb.equal(root.get("role"),UserRole.CUSTOMER));
            if (selected!=null) predicates.add(cb.equal(root.get("status"),selected));
            if (!term.isEmpty()) predicates.add(cb.or(
                    cb.like(cb.lower(root.get("email")),"%"+term+"%",'!'),
                    cb.like(cb.lower(root.get("fullName")),"%"+term+"%",'!'),
                    cb.like(root.get("phone"),"%"+term+"%",'!')));
            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
        var result=users.findAll(spec,PageRequest.of(page,size,Sort.by(Sort.Order.desc("createdDate"),Sort.Order.asc("id"))));
        return new CustomerPage(result.getContent().stream().map(CustomerResponse::from).toList(),page,size,result.getTotalElements(),result.getTotalPages());
    }
    public CustomerResponse detail(String id) {
        return CustomerResponse.from(customer(users.findById(id).orElseThrow(AdminCustomerService::missing)));
    }
    @Transactional
    public CustomerResponse setBlocked(String id, boolean blocked, String actorId) {
        User user=customer(users.findByIdForUpdate(id).orElseThrow(AdminCustomerService::missing));
        if (!blocked && user.getStatus()==UserStatus.INACTIVE)
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Tài khoản đang ngừng hoạt động; thao tác này chỉ mở khóa tài khoản bị khóa.");
        user.setStatus(blocked ? UserStatus.BANNED : UserStatus.ACTIVE);
        user.setLastModifyBy(actorId);
        if (blocked) sessions.revokeAll(id);
        return CustomerResponse.from(user);
    }
    private static User customer(User u) { if (u.getRole()!=UserRole.CUSTOMER) throw missing(); return u; }
    private static ResponseStatusException missing() { return new ResponseStatusException(HttpStatus.NOT_FOUND,"Không tìm thấy khách hàng."); }
}
