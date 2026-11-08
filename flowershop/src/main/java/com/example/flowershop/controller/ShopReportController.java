package com.example.flowershop.controller;

import com.example.flowershop.dto.auth.CurrentUser;
import com.example.flowershop.service.ShopReportService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/shop/mine/{shopId}/reports")
@PreAuthorize("hasRole('SHOP')")
@SecurityRequirement(name = "bearerAuth")
public class ShopReportController {
    private final ShopReportService reports;

    public ShopReportController(ShopReportService reports) {
        this.reports = reports;
    }

    @GetMapping
    public ShopReportService.ReportResult report(@PathVariable(name = "shopId") String shopId,
            @AuthenticationPrincipal CurrentUser user,
            @RequestParam(name = "period", defaultValue = "MONTH") String period,
            @RequestParam(name = "value", required = false) String value) {
        return reports.report(shopId, user.id(), period, value);
    }
}
