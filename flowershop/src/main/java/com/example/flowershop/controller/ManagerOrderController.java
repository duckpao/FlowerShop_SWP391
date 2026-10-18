package com.example.flowershop.controller;

import com.example.flowershop.dto.auth.CurrentUser;
import com.example.flowershop.service.ManagerOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Shop quản lý và xử lý đơn hàng", description = "Chủ shop xem, xác nhận, hủy đơn và tạo/theo dõi vận đơn GHN")
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
    @Operation(summary = "Danh sách đơn hàng của shop")
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
    @Operation(summary = "Xem chi tiết đơn")
    @GetMapping("/{id}")
    public ManagerOrderService.OrderDetailResponse detail(@PathVariable String shopId, @PathVariable String id,
                                                          @AuthenticationPrincipal CurrentUser u) {
        return service.detail(shopId, u.id(), id);
    }

    /**
     * Xác nhận đơn hàng (Chuyển trạng thái từ PENDING sang PROCESSING).
     * Lúc này shop chuẩn bị hoa, chưa tạo vận đơn GHN ngay.
     */
    @Operation(summary = "Xác nhận đơn (PENDING -> PROCESSING)")
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
    @Operation(summary = "Tạo vận đơn GHN và chuyển sang đang giao")
    @PostMapping("/{id}/ship")
    @Parameter(name = "X-CSRF-TOKEN", in = ParameterIn.HEADER, required = true)
    public ManagerOrderService.ShipResult ship(@PathVariable String shopId, @PathVariable String id,
                                               @AuthenticationPrincipal CurrentUser u) {
        return service.ship(shopId, u.id(), id);
    }

    /** Lấy trạng thái vận đơn mới nhất từ GHN rồi đồng bộ vào Delivery và Order. */
    @Operation(summary = "Đồng bộ trạng thái vận đơn từ GHN")
    @PostMapping("/{id}/refresh-status")
    @Parameter(name = "X-CSRF-TOKEN", in = ParameterIn.HEADER, required = true)
    public ManagerOrderService.DeliveryInfo refreshStatus(@PathVariable String shopId, @PathVariable String id,
                                                          @AuthenticationPrincipal CurrentUser u) {
        return service.refreshStatus(shopId, u.id(), id);
    }

    /** Gửi thao tác (cancel/return/storing) sang GHN staging, trả về trạng thái GHN báo lại. */
    @Operation(summary = "Gửi thao tác trạng thái sang GHN (hủy, trả hàng, giao lại)")
    @PostMapping("/{id}/ghn-status")
    @Parameter(name = "X-CSRF-TOKEN", in = ParameterIn.HEADER, required = true)
    public ManagerOrderService.DeliveryInfo pushGhnStatus(@PathVariable String shopId, @PathVariable String id,
                                                          @RequestBody java.util.Map<String, String> body,
                                                          @AuthenticationPrincipal CurrentUser u) {
        return service.pushGhnStatus(shopId, u.id(), id, body.get("status"));
    }

    /**
     * Lấy danh sách hàng đợi các đơn hàng cần xử lý gấp (PENDING, AWAITING_DEPOSIT, PROCESSING).
     */
    @Operation(summary = "Danh sách đơn đang chờ xử lý")
    @GetMapping("/queue")
    public ManagerOrderService.Results queue(@PathVariable String shopId, @AuthenticationPrincipal CurrentUser u,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(required = false) String status) {
        return service.queue(shopId, u.id(), page, status);
    }

    /**
     * Lấy số lượng đơn hàng đang chờ xác nhận (dùng để hiển thị badge thông báo đỏ trên thanh menu).
     */
    @Operation(summary = "Đếm số đơn đang chờ xử lý")
    @GetMapping("/queue/count")
    public ManagerOrderService.QueueCount queueCount(@PathVariable String shopId, @AuthenticationPrincipal CurrentUser u) {
        return service.queueCount(shopId, u.id());
    }

    /**
     * Hủy đơn hàng bởi chủ shop (chỉ cho phép khi đơn chưa chuyển sang trạng thái đang giao).
     * @param body Chứa lý do hủy đơn (reason)
     */
    @Operation(summary = "Hủy đơn hàng")
    @PostMapping("/{id}/cancel")
    @Parameter(name = "X-CSRF-TOKEN", in = ParameterIn.HEADER, required = true)
    public void cancel(@PathVariable String shopId, @PathVariable String id,
                       @RequestBody java.util.Map<String, String> body,
                       @AuthenticationPrincipal CurrentUser u) {
        service.cancel(shopId, u.id(), id, body.get("reason"));
    }

    /**
     * Cập nhật thông tin vận đơn trên GHN (chỉ dành cho các đơn hàng đã tạo vận đơn).
     * @param body Các trường cần cập nhật (ví dụ: note, weight, to_name, to_phone).
     */
    @Operation(summary = "Cập nhật thông tin vận đơn GHN")
    @PostMapping("/{id}/update-ghn")
    @Parameter(name = "X-CSRF-TOKEN", in = ParameterIn.HEADER, required = true)
    public void updateGhn(@PathVariable String shopId, @PathVariable String id,
                          @RequestBody java.util.Map<String, Object> body,
                          @AuthenticationPrincipal CurrentUser u) {
        service.updateGhn(shopId, u.id(), id, body);
    }
}
