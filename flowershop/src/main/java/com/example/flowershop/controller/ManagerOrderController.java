package com.example.flowershop.controller;

import com.example.flowershop.dto.auth.CurrentUser;
import com.example.flowershop.service.ManagerOrderService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Controller quản lý đơn hàng dành riêng cho Chủ cửa hàng (Shop Manager).
 * Tất cả các endpoints đều yêu cầu quyền ROLE_SHOP và xác thực Bearer JWT token.
 * Các thao tác thay đổi dữ liệu (POST/PUT/DELETE) yêu cầu header X-CSRF-TOKEN.
 */
@RestController
@RequestMapping("/api/shop/mine/{shopId}/orders")
@PreAuthorize("hasRole('SHOP')")
@SecurityRequirement(name = "bearerAuth")
public class ManagerOrderController {
    private final ManagerOrderService service;

    public ManagerOrderController(ManagerOrderService service) { 
        this.service = service; 
    }

    /**
     * Lấy danh sách đơn hàng của shop theo phân trang và bộ lọc trạng thái tùy chọn.
     * @param shopId ID của cửa hàng cần lấy đơn hàng
     * @param u Thông tin người dùng hiện tại (chủ shop)
     * @param page Số trang (bắt đầu từ 0)
     * @param status Trạng thái đơn hàng cần lọc (PENDING, PROCESSING, DELIVERING, COMPLETED, CANCELLED...)
     * @return Danh sách đơn hàng phân trang
     */
    @GetMapping
    public ManagerOrderService.Results list(@PathVariable String shopId, @AuthenticationPrincipal CurrentUser u,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(required = false) String status) {
        return service.list(shopId, u.id(), page, status);
    }

    /**
     * Xem thông tin chi tiết một đơn hàng cụ thể của shop.
     * Bao gồm: danh sách sản phẩm, địa chỉ giao hàng, địa chỉ shop, vận đơn GHN, trạng thái thanh toán.
     */
    @GetMapping("/{id}")
    public ManagerOrderService.OrderDetailResponse detail(@PathVariable String shopId, @PathVariable String id,
                                                          @AuthenticationPrincipal CurrentUser u) {
        return service.detail(shopId, u.id(), id);
    }

    /**
     * Xác nhận đơn hàng (Chuyển trạng thái từ PENDING sang PROCESSING).
     * Lúc này shop chuẩn bị hoa, chưa tạo vận đơn GHN ngay.
     */
    @PostMapping("/{id}/confirm")
    @Parameter(name = "X-CSRF-TOKEN", in = ParameterIn.HEADER, required = true)
    public void confirm(@PathVariable String shopId, @PathVariable String id,
                        @AuthenticationPrincipal CurrentUser u) {
        service.confirm(shopId, u.id(), id);
    }

    /**
     * Tạo mã vận đơn trên hệ thống Giao Hàng Nhanh (GHN) và chuyển đơn sang DELIVERING (Đang giao).
     * Chỉ áp dụng cho đơn hàng đang ở trạng thái PROCESSING (Đang chuẩn bị).
     */
    @PostMapping("/{id}/ship")
    @Parameter(name = "X-CSRF-TOKEN", in = ParameterIn.HEADER, required = true)
    public ManagerOrderService.ShipResult ship(@PathVariable String shopId, @PathVariable String id,
                                               @AuthenticationPrincipal CurrentUser u) {
        return service.ship(shopId, u.id(), id);
    }

    /**
     * Đồng bộ / Cập nhật trạng thái giao hàng thực tế từ GHN về hệ thống.
     * Gọi API tra cứu của GHN theo tracking code; nếu GHN báo 'delivered', đơn tự chuyển sang COMPLETED.
     */
    @PostMapping("/{id}/refresh-status")
    @Parameter(name = "X-CSRF-TOKEN", in = ParameterIn.HEADER, required = true)
    public String refreshStatus(@PathVariable String shopId, @PathVariable String id,
                                 @AuthenticationPrincipal CurrentUser u) {
        return service.refreshStatus(shopId, u.id(), id).name();
    }

    /**
     * Giả lập GHN giao hàng thành công (dành cho môi trường test/sandbox).
     * Chuyển trạng thái vận đơn sang DELIVERED, đơn hàng sang COMPLETED, và thanh toán sang SUCCESS.
     */
    @PostMapping("/{id}/simulate-delivered")
    @Parameter(name = "X-CSRF-TOKEN", in = ParameterIn.HEADER, required = true)
    public String simulateDelivered(@PathVariable String shopId, @PathVariable String id,
                                    @AuthenticationPrincipal CurrentUser u) {
        return service.simulateDelivered(shopId, u.id(), id).name();
    }

    /**
     * Lấy danh sách hàng đợi các đơn hàng cần xử lý gấp (PENDING, AWAITING_DEPOSIT, PROCESSING).
     */
    @GetMapping("/queue")
    public ManagerOrderService.Results queue(@PathVariable String shopId, @AuthenticationPrincipal CurrentUser u,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(required = false) String status) {
        return service.queue(shopId, u.id(), page, status);
    }

    /**
     * Lấy số lượng đơn hàng đang chờ xác nhận (dùng để hiển thị badge thông báo đỏ trên thanh menu).
     */
    @GetMapping("/queue/count")
    public ManagerOrderService.QueueCount queueCount(@PathVariable String shopId, @AuthenticationPrincipal CurrentUser u) {
        return service.queueCount(shopId, u.id());
    }

    /**
     * Hủy đơn hàng bởi chủ shop (chỉ cho phép khi đơn chưa chuyển sang trạng thái đang giao).
     * @param body Chứa lý do hủy đơn (reason)
     */
    @PostMapping("/{id}/cancel")
    @Parameter(name = "X-CSRF-TOKEN", in = ParameterIn.HEADER, required = true)
    public void cancel(@PathVariable String shopId, @PathVariable String id,
                       @RequestBody java.util.Map<String, String> body,
                       @AuthenticationPrincipal CurrentUser u) {
        service.cancel(shopId, u.id(), id, body.get("reason"));
    }
}