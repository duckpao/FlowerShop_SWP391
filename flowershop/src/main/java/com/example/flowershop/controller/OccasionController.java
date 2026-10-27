package com.example.flowershop.controller;

import com.example.flowershop.dto.auth.CurrentUser;
import com.example.flowershop.service.OccasionService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Dịp đặc biệt của khách (sinh nhật, kỷ niệm...) để hệ thống nhắc qua email. */
@RestController
@RequestMapping("/api/customer/occasions")
@PreAuthorize("hasRole('CUSTOMER')")
@SecurityRequirement(name = "bearerAuth")
public class OccasionController {

    private final OccasionService occasions;

    public OccasionController(OccasionService occasions) {
        this.occasions = occasions;
    }

    /** GET /api/customer/occasions: các dịp của khách, sắp theo lần diễn ra gần nhất. */
    @GetMapping
    public List<OccasionService.OccasionResponse> mine(@AuthenticationPrincipal CurrentUser user) {
        return occasions.mine(user.id());
    }

    /** POST /api/customer/occasions */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Parameter(name = "X-CSRF-TOKEN", in = ParameterIn.HEADER, required = true)
    public OccasionService.OccasionResponse create(@AuthenticationPrincipal CurrentUser user,
                                                   @Valid @RequestBody OccasionService.OccasionRequest request) {
        return occasions.create(user.id(), request);
    }

    /** PUT /api/customer/occasions/{id} */
    @PutMapping("/{id}")
    @Parameter(name = "X-CSRF-TOKEN", in = ParameterIn.HEADER, required = true)
    public OccasionService.OccasionResponse update(@AuthenticationPrincipal CurrentUser user, @PathVariable String id,
                                                   @Valid @RequestBody OccasionService.OccasionRequest request) {
        return occasions.update(user.id(), id, request);
    }

    /** DELETE /api/customer/occasions/{id} */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Parameter(name = "X-CSRF-TOKEN", in = ParameterIn.HEADER, required = true)
    public void delete(@AuthenticationPrincipal CurrentUser user, @PathVariable String id) {
        occasions.delete(user.id(), id);
    }
}
