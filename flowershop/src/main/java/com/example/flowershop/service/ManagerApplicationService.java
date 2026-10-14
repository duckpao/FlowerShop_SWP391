package com.example.flowershop.service;
import com.example.flowershop.entity.*;
import com.example.flowershop.entity.enums.*;
import com.example.flowershop.repository.*;
import jakarta.validation.constraints.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.*;

@Service @Transactional(readOnly=true)
public class ManagerApplicationService {
    public record Input(@NotBlank @Size(max=100) String fullName,
        @NotBlank @Pattern(regexp="[+]?[0-9]{9,15}") String phone,
        @NotBlank @Size(max=255) String shopName,@NotBlank @Size(max=5000) String description,
        @NotBlank @Size(max=255) String addressLine,@NotBlank @Size(max=100) String city,
        @NotBlank @Size(max=100) String district,@NotBlank @Size(max=100) String ward) {}
    public record Decision(@NotNull Boolean approve,@NotNull @Size(max=1000) String note) {}
    public record Result(String id,String email,String fullName,String phone,String shopName,String description,
        String addressLine,String city,String district,String ward,String status,String reviewNote,Instant submittedAt,String shopId) {}
    public record Results(List<Result> content,int page,int totalPages,long totalElements) {}
    private final ManagerApplicationRepository applications; private final UserRepository users;
    private final ShopRepository shops; private final AddressRepository addresses;
    private final AuthSessionRepository sessions;
    public ManagerApplicationService(ManagerApplicationRepository a,UserRepository u,ShopRepository s,AddressRepository ad,AuthSessionRepository se) {
        applications=a;users=u;shops=s;addresses=ad;sessions=se;
    }
    private Result response(ManagerApplication a) { return new Result(a.getId(),a.getUser().getEmail(),a.getFullName(),a.getPhone(),a.getShopName(),a.getDescription(),a.getAddressLine(),a.getCity(),a.getDistrict(),a.getWard(),a.getStatus(),a.getReviewNote(),a.getSubmittedAt(),a.getShopId()); }
    private static ResponseStatusException conflict(String message) {return new ResponseStatusException(HttpStatus.CONFLICT,message);}
    private static ResponseStatusException missing() {return new ResponseStatusException(HttpStatus.NOT_FOUND,"Không tìm thấy đơn.");}
    private void eligible(User user) {
        if(user.getRole()!=UserRole.CUSTOMER || user.getStatus()!=UserStatus.ACTIVE || !Boolean.TRUE.equals(user.getIsEmailVerified())) throw conflict("Chỉ Customer đang hoạt động, đã xác thực email được mở shop.");
        if(!shops.findByOwnerIdOrderByNameAsc(user.getId()).isEmpty()) throw conflict("Tài khoản đã sở hữu shop.");
    }
    public List<Result> mine(String actor) {return applications.findByUserId(actor).stream().map(this::response).toList();}
    public Results list(String status,int page) {
        if(!List.of("PENDING","APPROVED","REJECTED").contains(status) || page<0 || page>100000) throw new IllegalArgumentException("Bộ lọc không hợp lệ.");
        var result=applications.findByStatus(status,PageRequest.of(page,20,Sort.by(Sort.Order.desc("submittedAt"),Sort.Order.asc("id"))));
        return new Results(result.getContent().stream().map(this::response).toList(),page,result.getTotalPages(),result.getTotalElements());
    }
    @Transactional public Result submit(String actor,Input input) {
        var user=users.findByIdForUpdate(actor).orElseThrow(ManagerApplicationService::missing);eligible(user);
        var a=applications.findByUserId(actor).orElseGet(ManagerApplication::new);
        if(a.getId()!=null && !"REJECTED".equals(a.getStatus())) throw conflict("Bạn đã có đơn đang chờ hoặc đã được duyệt.");
        if(a.getId()==null) a.setId(UUID.randomUUID().toString());
        a.setUser(user);a.setFullName(input.fullName().strip());a.setPhone(input.phone().strip());a.setShopName(input.shopName().strip());a.setDescription(input.description().strip());
        a.setAddressLine(input.addressLine().strip());a.setCity(input.city().strip());a.setDistrict(input.district().strip());a.setWard(input.ward().strip());
        a.setStatus("PENDING");a.setSubmittedAt(Instant.now());a.setReviewedAt(null);a.setReviewedBy(null);a.setReviewNote(null);
        return response(applications.saveAndFlush(a));
    }
    @Transactional public Result decide(String id,String admin,Decision decision) {
        String applicant=applications.applicantId(id).orElseThrow(ManagerApplicationService::missing);
        var user=users.findByIdForUpdate(applicant).orElseThrow(ManagerApplicationService::missing);
        var a=applications.lock(id).orElseThrow(ManagerApplicationService::missing);
        if(!"PENDING".equals(a.getStatus())) throw conflict("Đơn đã được xử lý. Hãy tải lại.");
        if(decision.approve()) {
            eligible(user);
            var shop=Shop.builder().id(UUID.randomUUID().toString()).owner(user).name(a.getShopName()).description(a.getDescription()).status(ShopStatus.ACTIVE).createdBy(admin).build();
            shops.saveAndFlush(shop);
            addresses.save(Address.builder().id(UUID.randomUUID().toString()).shop(shop).addressLine(a.getAddressLine()).city(a.getCity()).district(a.getDistrict()).ward(a.getWard()).isDefault(true).createdBy(admin).build());
            user.setRole(UserRole.SHOP);user.setLastModifyBy(admin);sessions.revokeAll(applicant);a.setShopId(shop.getId());
        }
        a.setStatus(decision.approve()?"APPROVED":"REJECTED");a.setReviewNote(decision.note().strip());a.setReviewedBy(admin);a.setReviewedAt(Instant.now());return response(a);
    }
}
