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
@RequestMapping("/api/staff/shops/{shopId}/refunds")
@PreAuthorize("hasRole('SHOP_STAFF')")
@SecurityRequirement(name = "bearerAuth")
public class StaffRefundController {

    public record StatusRequest(String status) {}

    private final StaffRefundService service;

    public StaffRefundController(StaffRefundService service) {
        this.service = service;
    }

    @GetMapping
    public List<RefundResponse> list(@PathVariable String shopId, @AuthenticationPrincipal CurrentUser u) {
        return service.list(shopId, u.id());
    }

    @PostMapping("/{id}/status")
    public RefundResponse updateStatus(@PathVariable String shopId, @PathVariable String id,
                                       @RequestBody StatusRequest body, @AuthenticationPrincipal CurrentUser u) {
        return service.updateStatus(shopId, u.id(), id, body.status());
    }
}
