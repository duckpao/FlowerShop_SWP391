package com.example.flowershop.controller;

import com.example.flowershop.dto.auth.CurrentUser;
import com.example.flowershop.service.CustomerRefundService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/** Khách gửi, hủy yêu cầu hoàn tiền cho đơn của mình và tải ảnh bằng chứng. */
@RestController
@RequestMapping("/api/customer/orders/{id}/refunds")
@PreAuthorize("hasRole('CUSTOMER')")
public class CustomerRefundController {
    private final CustomerRefundService service;

    public CustomerRefundController(CustomerRefundService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerRefundService.RefundResult request(@AuthenticationPrincipal CurrentUser user,
            @PathVariable(name = "id") String id, @Valid @RequestBody CustomerRefundService.RefundInput body) {
        return service.request(user.id(), id, body);
    }

    @PostMapping("/{refundId}/cancel")
    public CustomerRefundService.RefundResult cancel(@AuthenticationPrincipal CurrentUser user,
            @PathVariable(name = "id") String id, @PathVariable(name = "refundId") String refundId) {
        return service.cancel(user.id(), id, refundId);
    }

    @PostMapping("/images")
    public Map<String, String> uploadImage(@AuthenticationPrincipal CurrentUser user,
            @PathVariable(name = "id") String id, @RequestParam("file") MultipartFile file) {
        return Map.of("url", service.uploadEvidence(user.id(), id, file));
    }
}
