package com.example.flowershop.controller;

import com.example.flowershop.dto.auth.CurrentUser;
import com.example.flowershop.service.AdminOversightService;
import com.example.flowershop.service.ManagerOrderService;
import com.example.flowershop.service.StaffRefundService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** SRS 4.4: Admin xử lý hoàn tiền, xem đơn hàng và kiểm duyệt đánh giá. */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
public class AdminOversightController {

    private final StaffRefundService refunds;
    private final AdminOversightService oversight;
    private final ManagerOrderService orders;

    public AdminOversightController(StaffRefundService refunds, AdminOversightService oversight, ManagerOrderService orders) {
        this.refunds = refunds;
        this.oversight = oversight;
        this.orders = orders;
    }

    @GetMapping("/refunds")
    public StaffRefundService.RefundPage refunds(@RequestParam(name = "status", required = false) String status,
                                                 @RequestParam(name = "page", defaultValue = "0") int page) {
        return refunds.adminList(status, page);
    }

    @PostMapping("/refunds/{id}/status")
    public StaffRefundService.RefundResponse refundStatus(@PathVariable(name = "id") String id,
            @RequestBody StaffRefundController.StatusRequest body, @AuthenticationPrincipal CurrentUser admin) {
        return refunds.adminUpdateStatus(admin.id(), id, body.status(), body.note());
    }

    @GetMapping("/orders")
    public AdminOversightService.OrderPage orders(@RequestParam(name = "status", required = false) String status,
                                                  @RequestParam(name = "page", defaultValue = "0") int page) {
        return oversight.orders(status, page);
    }

    @GetMapping("/orders/{id}")
    public ManagerOrderService.OrderDetailResponse order(@PathVariable(name = "id") String id) {
        return orders.adminDetail(id);
    }

    @GetMapping("/reviews")
    public AdminOversightService.ReviewPage reviews(@RequestParam(name = "page", defaultValue = "0") int page) {
        return oversight.reviews(page);
    }

    @DeleteMapping("/reviews/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteReview(@PathVariable(name = "id") String id) {
        oversight.deleteReview(id);
    }
}
