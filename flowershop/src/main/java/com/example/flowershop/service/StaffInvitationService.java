package com.example.flowershop.service;

import com.example.flowershop.entity.*;
import com.example.flowershop.entity.enums.*;
import com.example.flowershop.repository.*;
import jakarta.validation.constraints.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.time.Instant;
import java.security.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service @Transactional(readOnly=true)
public class StaffInvitationService {
    public record SendRequest(@NotBlank @Email @Size(max=50) String email) {}
    public record AcceptRequest(@NotBlank @Size(max=100) String code) {
        @Override public String toString() { return "AcceptRequest[redacted]"; }
    }
    public record InvitationResponse(String id,String shopId,String shopName,String email,String status,Instant expiresAt) {}
    private final ShopRepository shops; private final UserRepository users;
    private final StaffInvitationRepository invitations; private final ShopStaffRepository staff;
    private final AuthSessionRepository sessions; private final ApplicationEventPublisher events;
    private final SecureRandom random=new SecureRandom();
    public StaffInvitationService(ShopRepository shops,UserRepository users,StaffInvitationRepository invitations,ShopStaffRepository staff,AuthSessionRepository sessions,ApplicationEventPublisher events) {
        this.shops=shops;this.users=users;this.invitations=invitations;this.staff=staff;this.sessions=sessions;this.events=events;
    }
    private Shop owned(String shopId,String actor,boolean write) {
        var shop=(write?shops.findForUpdate(shopId):shops.findById(shopId)).orElseThrow(StaffInvitationService::missing);
        if(!shop.getOwner().getId().equals(actor)) throw missing();
        if(write && shop.getStatus()!=ShopStatus.ACTIVE) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Chỉ shop đang hoạt động mới quản lý lời mời.");
        return shop;
    }
    public List<InvitationResponse> list(String shopId,String actor) {
        owned(shopId,actor,false); return invitations.findByShopIdOrderByIssuedAtDesc(shopId).stream().map(this::response).toList();
    }
    @Transactional public InvitationResponse send(String shopId,String actor,String email) {
        var shop=owned(shopId,actor,true); String normalized=email.strip().toLowerCase(Locale.ROOT); Instant now=Instant.now();
        var user=users.findByEmail(normalized).orElse(null);
        if(user!=null && (user.getStatus()!=UserStatus.ACTIVE || (user.getRole()!=UserRole.CUSTOMER && user.getRole()!=UserRole.SHOP_STAFF)))
            throw new IllegalArgumentException("Email không đủ điều kiện nhận lời mời nhân viên.");
        if(user!=null && staff.findByShopIdAndUserId(shopId,user.getId()).isPresent())
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Tài khoản đã được liên kết với shop; hãy dùng mục quản lý nhân viên.");
        var invite=invitations.findByShopIdAndEmail(shopId,normalized).orElseGet(()->{
            var fresh=new StaffInvitation();fresh.setId(UUID.randomUUID().toString());fresh.setShop(shop);fresh.setEmail(normalized);fresh.setWindowStart(now);return fresh;
        });
        if(invite.getIssuedAt()!=null && now.isBefore(invite.getIssuedAt().plusSeconds(60))) throw rate();
        if(!now.isBefore(invite.getWindowStart().plusSeconds(3600))) { invite.setWindowStart(now);invite.setSendCount(0); }
        if(invite.getSendCount()>=3 || invitations.countByShopIdAndIssuedAtAfter(shopId,now.minusSeconds(3600))>=20) throw rate();
        byte[] bytes=new byte[32]; random.nextBytes(bytes); String raw=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        invite.setTokenHash(hash(raw));invite.setIssuedAt(now);invite.setExpiresAt(now.plusSeconds(86400));invite.setSendCount(invite.getSendCount()+1);invite.setStatus("PENDING");
        invitations.saveAndFlush(invite);
        events.publishEvent(new AuthMailEvent(actor,normalized,AuthMailEvent.Kind.STAFF_INVITATION,shop.getName()+"\nMã lời mời: "+invite.getId()+"."+raw));
        return response(invite);
    }
    @Transactional public void cancel(String shopId,String actor,String id) {
        owned(shopId,actor,true);
        var invite=invitations.findById(id).filter(i->i.getShop().getId().equals(shopId)).orElseThrow(StaffInvitationService::missing);
        if(!invite.getStatus().equals("PENDING")) throw new ResponseStatusException(HttpStatus.CONFLICT,"Lời mời không còn chờ xác nhận.");
        invite.setStatus("CANCELLED");
    }
    @Transactional public void accept(String actor,String code) {
        var user=users.findByIdForUpdate(actor).orElseThrow(StaffInvitationService::missing);
        String[] parts=code.strip().split("\\.",-1);
        if(parts.length!=2) throw invalid();
        String shopId=invitations.findShopId(parts[0]).orElseThrow(StaffInvitationService::invalid);
        var shop=shops.findForUpdate(shopId).orElseThrow(StaffInvitationService::invalid);
        // Refresh after acquiring shop lock: cancel/resend/accept serialize on the same shop.
        var invite=invitations.findByShopIdAndEmail(shopId,user.getEmail()).orElseThrow(StaffInvitationService::invalid);
        if(!invite.getId().equals(parts[0]) || !invite.getEmail().equals(user.getEmail()) || !invite.getStatus().equals("PENDING")
                || !Instant.now().isBefore(invite.getExpiresAt()) || !MessageDigest.isEqual(hash(parts[1]).getBytes(StandardCharsets.US_ASCII),invite.getTokenHash().getBytes(StandardCharsets.US_ASCII))) throw invalid();
        if(shop.getStatus()!=ShopStatus.ACTIVE || user.getStatus()!=UserStatus.ACTIVE || !Boolean.TRUE.equals(user.getIsEmailVerified())
                || (user.getRole()!=UserRole.CUSTOMER && user.getRole()!=UserRole.SHOP_STAFF)) throw invalid();
        if(staff.findByShopIdAndUserId(shopId,actor).isPresent()) throw new ResponseStatusException(HttpStatus.CONFLICT,"Bạn đã có liên kết với shop này.");
        var member=new ShopStaff();member.setId(UUID.randomUUID().toString());member.setShop(shop);member.setUser(user);member.setActive(true);staff.save(member);
        user.setRole(UserRole.SHOP_STAFF);user.setLastModifyBy(actor);invite.setStatus("ACCEPTED");
        sessions.revokeAll(actor);
    }
    private InvitationResponse response(StaffInvitation i) {
        String status=i.getStatus().equals("PENDING") && !Instant.now().isBefore(i.getExpiresAt())?"EXPIRED":i.getStatus();
        return new InvitationResponse(i.getId(),i.getShop().getId(),i.getShop().getName(),i.getEmail(),status,i.getExpiresAt());
    }
    private static String hash(String raw) { try {return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8)));}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);} }
    private static IllegalArgumentException invalid(){return new IllegalArgumentException("Lời mời không hợp lệ, đã hết hạn hoặc không dành cho tài khoản này.");}
    private static ResponseStatusException missing(){return new ResponseStatusException(HttpStatus.NOT_FOUND,"Không tìm thấy shop hoặc lời mời.");}
    private static ResponseStatusException rate(){return new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Chờ ít nhất 60 giây; tối đa 3 thư/email và 20 email/shop trong một giờ.");}
}
