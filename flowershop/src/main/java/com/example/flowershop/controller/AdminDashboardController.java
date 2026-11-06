package com.example.flowershop.controller;

import com.example.flowershop.service.AdminDashboardService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Locale;

@RestController
@RequestMapping("/api/admin/dashboard")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name="bearerAuth")
public class AdminDashboardController {
    private final AdminDashboardService dashboard;
    public AdminDashboardController(AdminDashboardService dashboard) { this.dashboard=dashboard; }

    @GetMapping
    public AdminDashboardService.Dashboard get(@RequestParam(name = "granularity", defaultValue="DAY") String granularity,
            @RequestParam(name = "from", required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(name = "to", required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to) {
        AdminDashboardService.Granularity g;
        try { g = AdminDashboardService.Granularity.valueOf(granularity.toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException e) { throw new IllegalArgumentException("Kiểu báo cáo phải là DAY, MONTH hoặc YEAR."); }
        return dashboard.dashboard(g, from, to);
    }
}
