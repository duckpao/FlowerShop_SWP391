package com.example.flowershop.controller;

import com.example.flowershop.dto.auth.CurrentUser;
import com.example.flowershop.service.StaffRefundService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** Admin xem và xử lý yêu cầu hoàn tiền của mọi shop. */
@RestController
@RequestMapping("/api/admin/refunds")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
public class AdminRefundController {
    private final StaffRefundService service;

    public AdminRefundController(StaffRefundService service) {
        this.service = service;
    }

    @GetMapping
    public StaffRefundService.RefundPage list(@RequestParam(name = "status", required = false) String status,
                                              @RequestParam(name = "page", defaultValue = "0") int page) {
        return service.adminList(status, page);
    }

    @PostMapping("/{id}/status")
    public StaffRefundService.RefundResponse updateStatus(@PathVariable(name = "id") String id,
            @RequestBody StaffRefundController.StatusRequest body, @AuthenticationPrincipal CurrentUser admin) {
        return service.adminUpdateStatus(admin.id(), id, body.status(), body.note());
    }
}
