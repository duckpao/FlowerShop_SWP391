package com.example.flowershop.controller;
import com.example.flowershop.service.StaffInvitationService;
import com.example.flowershop.dto.auth.CurrentUser;
import com.example.flowershop.dto.common.MessageResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import java.util.List;
@RestController @SecurityRequirement(name="bearerAuth")
public class StaffInvitationController {
    private final StaffInvitationService service;
    public StaffInvitationController(StaffInvitationService service){this.service=service;}
    @GetMapping("/api/shop/mine/{id}/invitations") @PreAuthorize("hasRole('SHOP')")
    public List<StaffInvitationService.InvitationResponse> list(@PathVariable String id,@AuthenticationPrincipal CurrentUser u){return service.list(id,u.id());}
    @PostMapping("/api/shop/mine/{id}/invitations") @PreAuthorize("hasRole('SHOP')") @ResponseStatus(org.springframework.http.HttpStatus.ACCEPTED)
    @Parameter(name="X-CSRF-TOKEN",in=ParameterIn.HEADER,required=true)
    public StaffInvitationService.InvitationResponse send(@PathVariable String id,@AuthenticationPrincipal CurrentUser u,@Valid @RequestBody StaffInvitationService.SendRequest body){return service.send(id,u.id(),body.email());}
    @DeleteMapping("/api/shop/mine/{id}/invitations/{invitationId}") @PreAuthorize("hasRole('SHOP')") @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    @Parameter(name="X-CSRF-TOKEN",in=ParameterIn.HEADER,required=true)
    public void cancel(@PathVariable String id,@PathVariable String invitationId,@AuthenticationPrincipal CurrentUser u){service.cancel(id,u.id(),invitationId);}
    @PostMapping("/api/account/staff-invitations/accept") @PreAuthorize("hasAnyRole('CUSTOMER','SHOP_STAFF')")
    @Parameter(name="X-CSRF-TOKEN",in=ParameterIn.HEADER,required=true)
    public MessageResponse accept(@AuthenticationPrincipal CurrentUser u,@Valid @RequestBody StaffInvitationService.AcceptRequest body){
        service.accept(u.id(),body.code());return new MessageResponse("Đã tham gia cửa hàng. Hãy đăng nhập lại để dùng quyền nhân viên.");
    }
}
