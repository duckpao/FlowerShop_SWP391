package com.example.flowershop.controller;

import com.example.flowershop.dto.auth.CurrentUser;
import com.example.flowershop.service.StaffRefundService;
import com.example.flowershop.service.StaffRefundService.RefundResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Staff xem đơn hủy cần hoàn tiền của shop mình và cập nhật tiến độ xử lý. */
@RestController
// Hai đường dẫn: staff (/api/staff) và chủ shop (/api/shop/mine); SecurityConfig chặn role theo đường dẫn, guard kiểm tra quyền với shop
@RequestMapping({"/api/staff/shops/{shopId}/refunds", "/api/shop/mine/{shopId}/refunds"})
@PreAuthorize("hasAnyRole('SHOP', 'SHOP_STAFF')")
@SecurityRequirement(name = "bearerAuth")
public class StaffRefundController {

    public record StatusRequest(String status, String note) {}

    private final StaffRefundService service;

    public StaffRefundController(StaffRefundService service) {
        this.service = service;
    }

    @GetMapping
    public List<RefundResponse> list(@PathVariable(name = "shopId") String shopId, @AuthenticationPrincipal CurrentUser u) {
        return service.list(shopId, u.id());
    }

    @PostMapping("/{id}/status")
    public RefundResponse updateStatus(@PathVariable(name = "shopId") String shopId, @PathVariable(name = "id") String id,
                                       @RequestBody StatusRequest body, @AuthenticationPrincipal CurrentUser u) {
        return service.updateStatus(shopId, u.id(), id, body.status(), body.note());
    }
}
