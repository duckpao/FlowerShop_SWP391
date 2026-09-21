package com.example.flowershop.service;
import com.example.flowershop.entity.*;
import com.example.flowershop.entity.enums.*;
import com.example.flowershop.repository.*;
import jakarta.validation.constraints.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.time.Instant;
import java.util.*;

@Service @Transactional(readOnly=true)
public class StaffApplicationService {
    public record Input(@NotBlank @Size(max=100) String fullName,
        @NotBlank @Pattern(regexp="[+0-9 ()-]{8,20}") String phone,
        @NotBlank @Size(max=1000) String introduction) {}
    public record Result(String id,String shopId,String shopName,String email,String fullName,String phone,String introduction,String status,Instant submittedAt) {}
    private final StaffApplicationRepository applications;
    private final ShopRepository shops;
    private final UserRepository users;
    private final ShopStaffRepository staff;
    private final AuthSessionRepository sessions;
    private final org.springframework.context.ApplicationEventPublisher events;
    public StaffApplicationService(StaffApplicationRepository applications,ShopRepository shops,UserRepository users,ShopStaffRepository staff,AuthSessionRepository sessions,org.springframework.context.ApplicationEventPublisher events) {
        this.applications=applications;this.shops=shops;this.users=users;this.staff=staff;this.sessions=sessions;this.events=events;
    }
    private Result result(StaffApplication a) {return new Result(a.getId(),a.getShop().getId(),a.getShop().getName(),a.getUser().getEmail(),a.getFullName(),a.getPhone(),a.getIntroduction(),a.getStatus(),a.getSubmittedAt());}
    private static ResponseStatusException error(HttpStatus status,String message) {return new ResponseStatusException(status,message);}
    private void eligible(User u) {
        if(u.getRole()!=UserRole.CUSTOMER || u.getStatus()!=UserStatus.ACTIVE || !Boolean.TRUE.equals(u.getIsEmailVerified()))
            throw error(HttpStatus.CONFLICT,"Tài khoản phải là Customer đang hoạt động và đã xác thực email.");
    }
    public List<Result> mine(String actor) {return applications.findByUserIdOrderBySubmittedAtDesc(actor).stream().map(this::result).toList();}
    public List<Result> list(String shopId,String actor) {
        var shop=shops.findById(shopId).orElseThrow(()->error(HttpStatus.NOT_FOUND,"Không tìm thấy shop."));
        if(!shop.getOwner().getId().equals(actor)) throw error(HttpStatus.NOT_FOUND,"Không tìm thấy shop.");
        return applications.findByShopIdOrderBySubmittedAtDesc(shopId).stream().map(this::result).toList();
    }
    @Transactional public Result apply(String shopId,String actor,Input input) {
        var user=users.findByIdForUpdate(actor).orElseThrow(); eligible(user);
        var shop=shops.findForUpdate(shopId).orElseThrow(()->error(HttpStatus.NOT_FOUND,"Không tìm thấy shop."));
        if(shop.getStatus()!=ShopStatus.ACTIVE) throw error(HttpStatus.CONFLICT,"Shop chưa hoạt động.");
        if(staff.findByShopIdAndUserId(shopId,actor).isPresent()) throw error(HttpStatus.CONFLICT,"Bạn đã thuộc shop này.");
        var a=applications.findByShopIdAndUserId(shopId,actor).orElseGet(StaffApplication::new);
        if(a.getId()!=null && !"REJECTED".equals(a.getStatus())) throw error(HttpStatus.CONFLICT,"Bạn đã gửi đơn cho shop này.");
        if(a.getId()==null) a.setId(UUID.randomUUID().toString());
        a.setShop(shop);a.setUser(user);a.setFullName(input.fullName().strip());a.setPhone(input.phone().strip());a.setIntroduction(input.introduction().strip());a.setStatus("PENDING");a.setSubmittedAt(Instant.now());
        var saved=applications.saveAndFlush(a);
        events.publishEvent(new AuthMailEvent(shop.getOwner().getId(),shop.getOwner().getEmail(),AuthMailEvent.Kind.STAFF_APPLICATION_NEW,shop.getName()));
        return result(saved);
    }
    @Transactional public Result decide(String shopId,String id,String actor,boolean approve) {
        // Lock applicant before shop, matching invitation acceptance and application submission.
        var initial=applications.findById(id).orElseThrow(()->error(HttpStatus.NOT_FOUND,"Không tìm thấy đơn."));
        String applicant=initial.getUser().getId();
        var user=users.findByIdForUpdate(applicant).orElseThrow();
        var shop=shops.findForUpdate(shopId).orElseThrow(()->error(HttpStatus.NOT_FOUND,"Không tìm thấy shop."));
        if(!shop.getOwner().getId().equals(actor) || !initial.getShop().getId().equals(shopId)) throw error(HttpStatus.NOT_FOUND,"Không tìm thấy đơn.");
        // Refresh managed row after locks so repeated decisions cannot reuse stale status.
        refresh(initial);
        if(shop.getStatus()!=ShopStatus.ACTIVE || !"PENDING".equals(initial.getStatus())) throw error(HttpStatus.CONFLICT,"Shop hoặc đơn không còn đủ điều kiện xử lý.");
        if(approve) {
            eligible(user);
            if(staff.findByShopIdAndUserId(shopId,applicant).isPresent()) throw error(HttpStatus.CONFLICT,"Đã có nhân viên này.");
            var member=new ShopStaff();member.setId(UUID.randomUUID().toString());member.setShop(shop);member.setUser(user);member.setActive(true);staff.save(member);
            user.setRole(UserRole.SHOP_STAFF);user.setLastModifyBy(actor);sessions.revokeAll(applicant);
        }
        initial.setStatus(approve?"APPROVED":"REJECTED");
        events.publishEvent(new AuthMailEvent(user.getId(),user.getEmail(),approve?AuthMailEvent.Kind.STAFF_APPLICATION_APPROVED:AuthMailEvent.Kind.STAFF_APPLICATION_REJECTED,shop.getName()));
        return result(initial);
    }
    @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager entityManager;
    private void refresh(StaffApplication a) {entityManager.refresh(a,jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);}
}
