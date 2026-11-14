# SDS, mục II Detailed Code Design: phân đoạn (segment)

Mục II được chia thành **14 segment**, mỗi segment là một nhóm chức năng của hệ thống tương ứng với một nhóm controller và service ở backend. Mỗi segment gồm các mục con. Mỗi mục con là một chức năng cụ thể và có hai sơ đồ: class diagram và sequence diagram (PlantUML, quy ước vẽ trong [README.md](README.md)).

Giữ nguyên các số đang dùng trong SDS hiện tại (1 Authentication, 2 Product, 3 Order, 4 Cart, 5 Payment, 6 Favourite shop, 7 Voucher, 8 Pro, 10 Custom order) và lấp chỗ trống: số 9 được dùng, thêm 11 đến 14. Tiêu đề hiện bị gộp trong SDS ("4. Cart && Checkout 4.1 Add product to cart", "45. Payment && Refund", "10.Order for requirement") được tách lại.

Ký hiệu trạng thái:
- ✔ đã có PlantUML đúng với code (tên file trong [docs/sds](.))
- ◐ sơ đồ cũ trong file Word đã được đối chiếu và đúng với code, nhưng chưa có bản PlantUML (cần vẽ lại để đồng bộ)
- ✗ chưa có sơ đồ

Class diagram của các mục con dùng chung controller và service (ví dụ 3.4 đến 3.9) có thể dùng lại class diagram của mục đầu nhóm, ghi là "dùng chung".

## Segment 1. Authentication & Account (UC01 đến UC05)
Backend: `AuthController`, `SessionController`, `AccountController`; `AuthService`, `TokenAuthService`, `OtpService`, `GoogleAuthService`, `AccountService`, `VerificationMailSender`, `CloudinaryService`; package `security`.

| Mục | Chức năng | Class | Sequence |
|---|---|---|---|
| 1.1 | Mô hình bảo mật và token (JWT, CSRF, rate limit, CAPTCHA): `SecurityConfig`, `JwtAuthenticationFilter`, `JwtService`, `LoginRateLimiter`, `CaptchaService` | ✗ | ✗ |
| 1.2 | Phân quyền theo vai trò: `@PreAuthorize`, `ShopOperationGuard`, kiểm tra chủ shop hoặc nhân viên | ✗ | ✗ |
| 1.3 | Đăng ký và xác thực email (OTP) | ◐ | ✔ `1.3-registration-sequence` |
| 1.4 | Đăng nhập, làm mới phiên, đăng xuất | ◐ | ✔ `1.4-login-sequence` |
| 1.5 | Quên mật khẩu và gửi lại OTP | ◐ | ✔ `1.5-password-recovery-sequence` |
| 1.6 | Hồ sơ cá nhân và avatar | ✔ `1.6-profile-avatar-class` | ✔ `1.6-profile-avatar-sequence` |
| 1.7 | Địa chỉ cá nhân (chọn tỉnh, quận, phường GHN) | ◐ | ✔ `1.7-address-sequence` |
| 1.8 | Admin khóa và mở khóa khách hàng (UC06) | ◐ | ✔ `1.8-customer-lock-sequence` |
| 1.9 | Đăng nhập bằng Google | ✗ | ✗ |

## Segment 2. Product & Catalog (UC08 đến UC14)
Backend: `PublicCatalogController`, `PublicProductController`, `PublicShopController`, `ManagerProductController`, `FavoriteController`, `AdminCatalogController`; `CatalogService`, `ProductCardAssembler`, `ManagerProductService`, `FavoriteService`, `AdminCatalogService`, `ProductQuotaService`.

| Mục | Chức năng | Class | Sequence |
|---|---|---|---|
| 2.1 | Danh sách sản phẩm (lọc, sắp xếp, phân trang) | ✔ `2.1-product-list-class` | ✔ `2.1-product-list-sequence` |
| 2.2 | Chi tiết sản phẩm và đánh giá | ✔ `2.2-product-details-class` | ✔ `2.2-product-details-sequence` |
| 2.3 | Shop quản lý sản phẩm (thêm, sửa, ẩn, xóa mềm) | ✔ `2.3-product-crud-class` | ✔ `2.3-product-crud-sequence` |
| 2.4 | Lưu sản phẩm yêu thích | ✔ `2.4-save-favorite-class` | ◐ |
| 2.5 | Danh sách sản phẩm yêu thích | ◐ | ✔ `2.5-favorite-list-sequence` |
| 2.6 | Danh mục sản phẩm | ✔ `2.6-categories-class` | ✔ `2.6-categories-sequence` |
| 2.7 | Trang chủ | ✔ `2.7-home-page-class` | ✔ `2.7-home-page-sequence` |
| 2.8 | Admin kiểm duyệt sản phẩm (ẩn và hiện) | ✗ | ✗ |

## Segment 3. Order Management cho shop, gồm GHN (UC25)
Backend: `ManagerOrderController`, `PublicGhnController`; `ManagerOrderService`, `GhnService`, `DeliverySync`, `OrderStatusListener`.

| Mục | Chức năng | Class | Sequence |
|---|---|---|---|
| 3.1 | Chi tiết đơn của shop | ✔ `3.1-order-details-class` (và `3.1-order-details-alt-class`) | ✔ `3.1-order-details-sequence` (và `3.1-order-details-alt-sequence`) |
| 3.2 | Hàng đợi, xác nhận đơn, hủy đơn | ✔ `3.2-order-queue-class` (và `3.2-order-queue-alt-class`) | ✔ `3.2-order-queue-sequence` (và `3.2-order-queue-alt-sequence`) |
| 3.3 | Tạo vận đơn GHN | ✔ `3.3-create-ghn-waybill-class` (và `3.3-create-ghn-waybill-alt-class`) | ✔ `3.3-create-ghn-waybill-sequence` (và `3.3-create-ghn-waybill-alt-sequence`) |
| 3.4 | Đồng bộ trạng thái vận đơn GHN | dùng chung 3.3 | ✔ `3.4-sync-ghn-status-shop-sequence` |
| 3.5 | Gửi thao tác hủy, trả hàng, giao lại sang GHN | dùng chung 3.3 | ✔ `3.5-push-ghn-action-sequence` |
| 3.6 | Sửa vận đơn GHN | dùng chung 3.3 | ✔ `3.6-update-ghn-waybill-sequence` |
| 3.7 | Đổi trạng thái đơn thủ công (không gọi GHN) | ✔ `3.7-update-order-status-class` | ✔ `3.7-manual-status-change-sequence` (và `3.7-update-order-status-alt-sequence`) |
| 3.8 | Danh mục địa chỉ GHN và tính phí vận chuyển | dùng chung 3.3 | ✔ `3.8-ghn-master-data-sequence` |
| 3.9 | Danh sách đơn và bộ lọc trạng thái | ✔ `3.9-order-management-class` | ✔ `3.9-order-management-sequence` |

## Segment 4. Cart & Checkout (UC15 đến UC17)
Backend: `CartController`, `OrderController`; `CartService`, `OrderService`, `CouponService`, `GhnService`.

| Mục | Chức năng | Class | Sequence |
|---|---|---|---|
| 4.1 | Giỏ hàng: thêm, sửa số lượng, xóa, gộp giỏ của khách vãng lai | ✔ `4.1-shopping-cart-class` | ✔ `4.1-shopping-cart-sequence` |
| 4.2 | Tính phí vận chuyển khi chọn địa chỉ | ✔ `4.2-shipping-fee-class` | ✔ `4.2-shipping-fee-sequence` |
| 4.3 | Đặt hàng nhiều shop (`POST /api/orders/{userId}/batch`) | ✔ `4.3-multi-shop-checkout-class` | ✔ `4.3-multi-shop-checkout-sequence` |
| 4.4 | Đặt hàng COD và trang xác nhận | ✔ `4.4-cod-order-class` | ✔ `4.4-cod-order-sequence` |

## Segment 5. Payment & Refund (UC21 đến UC23)
Backend: `PaymentController`, `PaymentReturnController`, `CustomerOrderController`, `StaffRefundController`; `PaymentService`, `SePayGatewayService`, `CustomerRefundService`, `StaffRefundService`, `PaymentTimeoutScheduler`, `NotificationService`.

| Mục | Chức năng | Class | Sequence |
|---|---|---|---|
| 5.1 | Tạo thanh toán SePay (đơn lẻ và gộp nhiều đơn) | ✔ `5.1-create-sepay-payment-class` | ✔ `5.1-create-sepay-payment-sequence` |
| 5.2 | Quay lại từ SePay và đối soát trạng thái (không có webhook) | ✔ `5.2-sepay-payment-return-class` | ✔ `5.2-sepay-payment-return-sequence` |
| 5.3 | Tự hủy đơn quá hạn thanh toán | ✔ `5.3-expire-unpaid-order-class` | ✔ `5.3-expire-unpaid-order-sequence` |
| 5.4 | Khách yêu cầu hoàn tiền và hủy yêu cầu | ✔ `5.4-refund-management-class` | ✔ `5.4-refund-management-sequence` |
| 5.5 | Shop xử lý hoàn tiền | dùng chung 5.4 | dùng chung 5.4 |

## Segment 6. Favourite Shop và theo dõi shop (UC26, UC27)
Backend: `FavoriteShopController`; `FavoriteShopService`, `ShopProductMailJob`, `ShopProductMailDelivery`, `ShopProductNotificationService`.

| Mục | Chức năng | Class | Sequence |
|---|---|---|---|
| 6.1 | Danh sách shop đang theo dõi | ✔ `6.1-followed-shops-class` | ✔ `6.1-followed-shops-sequence` |
| 6.2 | Theo dõi shop (hiện đang dùng lại ảnh của 6.1 trong Word, cần ảnh riêng) | ✔ `6.2-follow-shop-class` | ✔ `6.2-follow-shop-sequence` |
| 6.3 | Bỏ theo dõi shop | ✔ `6.3-unfollow-shop-class` | ✔ `6.3-unfollow-shop-sequence` |
| 6.4 | Gửi email khi shop đăng sản phẩm mới (có thử lại) | ✔ `6.4-new-product-email-class` | ✔ `6.4-new-product-email-sequence` |

## Segment 7. Voucher (UC39)
Backend: `CouponController`; `CouponService`, `CouponRepository`, `OrderService`.

| Mục | Chức năng | Class | Sequence |
|---|---|---|---|
| 7.1 | Xem voucher | ✔ `7.1-view-vouchers-class` | ✔ `7.1-view-vouchers-sequence` |
| 7.2 | Tạo voucher | ✔ `7.2-create-voucher-class` | ✔ `7.2-create-voucher-sequence` |
| 7.3 | Sửa voucher | dùng chung 7.2 | ✔ `7.3-update-voucher-sequence` |
| 7.4 | Xóa voucher (xóa mềm) | ✔ `7.4-delete-voucher-class` | ✔ `7.4-delete-voucher-sequence` |
| 7.5 | Xem voucher dùng được khi thanh toán | ✔ `7.5-checkout-vouchers-class` | ✔ `7.5-checkout-vouchers-sequence` |
| 7.6 | Áp voucher khi đặt hàng | ✔ `7.6-apply-voucher-class` | ✔ `7.6-apply-voucher-sequence` |
| 7.7 | Bỏ voucher đã áp | ✔ `7.7-remove-voucher-class` | ✔ `7.7-remove-voucher-sequence` |
| 7.8 | Gợi ý voucher tốt nhất | ✔ `7.8-best-voucher-class` | ✔ `7.8-best-voucher-sequence` |

## Segment 8. Pro Subscription (UC40)
Backend: `ShopPlanController`, `AdminShopPlanController`; `ShopPlanService`, `SubscriptionReminderService`, `ProductQuotaService`, `ShopPlanClock`; `ShopSubscriptionJob`.

| Mục | Chức năng | Class | Sequence |
|---|---|---|---|
| 8.1 | Xem các gói Pro | ✔ `8.1-view-plans-class` | ✔ `8.1-view-plans-sequence` |
| 8.2 | Tạo gói Pro | ✔ `8.2-create-plan-class` | ✔ `8.2-create-plan-sequence` |
| 8.3 | Sửa gói Pro | ✔ `8.3-update-plan-class` | ✔ `8.3-update-plan-sequence` |
| 8.4 | Vô hiệu hóa gói Pro | ✔ `8.4-deactivate-plan-class` | ✔ `8.4-deactivate-plan-sequence` |
| 8.5 | Xem gói của shop | ✔ `8.5-view-subscription-class` | ✔ `8.5-view-subscription-sequence` |
| 8.6 | Đăng ký Pro | ✔ `8.6-register-pro-class` | ✔ `8.6-register-pro-sequence` |
| 8.7 | Gia hạn Pro | dùng chung 8.6 | ✔ `8.7-renew-pro-sequence` |
| 8.8 | Admin cấp Pro | ✔ `8.8-admin-grant-pro-class` | ✔ `8.8-admin-grant-pro-sequence` |
| 8.9 | Admin xác nhận thanh toán Pro | ✔ `8.9-admin-confirm-pro-payment-class` | ✔ `8.9-admin-confirm-pro-payment-sequence` |
| 8.10 | Quản lý sản phẩm nổi bật | ✔ `8.10-featured-products-class` | ✔ `8.10-featured-products-sequence` |
| 8.11 | Nhắc gói Pro sắp hết hạn | ✔ `8.11-pro-expiry-reminder-class` | ✔ `8.11-pro-expiry-reminder-sequence` |
| 8.12 | Áp dụng giới hạn theo gói | ✔ `8.12-enforce-limits-class` | ✔ `8.12-enforce-limits-sequence` |

## Segment 9. Customer Orders & Tracking (UC35, UC22) (segment mới)
Backend: `CustomerOrderController`, `OrderController`; `CustomerOrderService`, `OrderService`, `DeliverySync`, `GhnService`.

| Mục | Chức năng | Class | Sequence |
|---|---|---|---|
| 9.1 | Danh sách đơn của khách (lọc trạng thái, nhóm theo lần đặt) | ✔ `9.1-customer-order-list-class` | ✔ `9.1-customer-order-list-sequence` |
| 9.2 | Chi tiết đơn và theo dõi vận đơn (ba bước, phí ship, tự đồng bộ) | ✔ `9.2-customer-tracking-class` | ✔ `9.2-customer-tracking-sequence` |
| 9.3 | Khách tự hủy đơn | ✔ `9.3-customer-cancel-order-class` | ✔ `9.3-customer-cancel-order-sequence` |
| 9.4 | Hóa đơn và lịch sử giao dịch | ✔ `9.4-invoice-transactions-class` | ✔ `9.4-invoice-transactions-sequence` |
| 9.5 | Dashboard khách hàng | ✔ `9.5-customer-dashboard-class` | ✔ `9.5-customer-dashboard-sequence` |

## Segment 10. Custom Flower Order (UC18, UC19)
Backend: `CustomFlowerOrderController`, `CustomOrderWorkflowController`; `CustomFlowerOrderService`, `CustomOrderWorkflowService`, `CustomFlowerCatalogService`, `CustomOrderPricing`, `OrderService`.

| Mục | Chức năng | Class | Sequence |
|---|---|---|---|
| 10.1 | Chấp nhận báo giá (chọn cọc hoặc trả toàn bộ) | ✔ `10.1-accept-custom-quote-class` | ✔ `10.1-accept-custom-quote-sequence` |
| 10.2 | Đơn cọc và thanh toán phần còn lại | ✔ `10.2-custom-deposit-order-class` | ✔ `10.2-custom-deposit-order-sequence` |
| 10.3 | Khách tạo yêu cầu hoa riêng (có tùy chọn hoa, dịp, gói) | ✔ `10.3-create-custom-request-class` | ✔ `10.3-create-custom-request-sequence` |
| 10.4 | Shop báo giá, từ chối hoặc phản hồi | ✔ `10.4-shop-quote-custom-request-class` | ✔ `10.4-shop-quote-custom-request-sequence` |

## Segment 11. Reviews, Blog & Chat (UC20, UC24, UC28 đến UC30)
Backend: `ProductReviewController`, `PublicBlogController`, `BlogInteractionController`, `ManagerBlogController`, `chat` (`CustomerChatController`, `ManagerChatController`); `ProductReviewService`, `BlogService`, `BlogInteractionService`, `ChatService`, `ChatBroadcaster`.

| Mục | Chức năng | Class | Sequence |
|---|---|---|---|
| 11.1 | Đánh giá sản phẩm và phản hồi của shop | ✔ `11.1-product-review-class` | ✔ `11.1-product-review-sequence` |
| 11.2 | Bài viết chăm sóc hoa: xem, thích, lưu, bình luận | ✔ `11.2-customer-blog-class` | ✔ `11.2-customer-blog-sequence` |
| 11.3 | Shop quản lý bài viết | ✔ `11.3-shop-blog-management-class` | ✔ `11.3-shop-blog-management-sequence` |
| 11.4 | Chat khách và shop (REST và STOMP `/ws`) | ✔ `11.4-chat-class` | ✔ `11.4-chat-sequence` |

## Segment 12. Shop & Staff (UC36 đến UC38)
Backend: `ManagerApplicationController`, `ManagerShopController`, `StaffShopController`, `StaffApplicationController`, `StaffInvitationController`; `ManagerApplicationService`, `ManagerShopService`, `StaffApplicationService`, `StaffInvitationService`, `ShopOperationGuard`.

| Mục | Chức năng | Class | Sequence |
|---|---|---|---|
| 12.1 | Đăng ký làm chủ shop và duyệt | ✔ `12.1-shop-owner-registration-class` | ✔ `12.1-shop-owner-registration-sequence` |
| 12.2 | Quản lý thông tin shop (logo, mô tả, địa chỉ lấy hàng) | ✔ `12.2-manage-shop-info-class` | ✔ `12.2-manage-shop-info-sequence` |
| 12.3 | Khách ứng tuyển làm nhân viên | ✔ `12.3-staff-application-class` | ✔ `12.3-staff-application-sequence` |
| 12.4 | Chủ shop mời nhân viên | ✔ `12.4-staff-invitation-class` | ✔ `12.4-staff-invitation-sequence` |
| 12.5 | Quản lý nhân viên và quyền | ✔ `12.5-staff-management-class` | ✔ `12.5-staff-management-sequence` |

## Segment 13. Notifications, Occasions & Emails (UC31, UC32)
Backend: `OccasionController`, `NotificationController`; `OccasionService`, `OccasionSchedule`, `OccasionReminderService`, `NotificationService`, `OrderMailSender`, `HtmlMailSender`, `VerificationMailSender`; `OccasionReminderJob`.

| Mục | Chức năng | Class | Sequence |
|---|---|---|---|
| 13.1 | Quản lý dịp đặc biệt của khách | ✔ `13.1-manage-occasions-class` | ✔ `13.1-manage-occasions-sequence` |
| 13.2 | Nhắc dịp đặc biệt (email và thông báo chuông) | ✔ `13.2-notify-upcoming-occasion-class` | ✔ `13.2-notify-upcoming-occasion-sequence` |
| 13.3 | Email biên lai và đổi trạng thái đơn | ✔ `13.3-notify-user-confirmation-class` | ✔ `13.3-notify-user-confirmation-sequence` |
| 13.4 | Email giao thành công và lời nhắc đánh giá | ✔ `13.4-notify-order-delivered-class` | ✔ `13.4-notify-order-delivered-sequence` |
| 13.5 | Thông báo trong app (chuông, đánh dấu đã đọc) | ✔ `13.5-in-app-notifications-class` | ✔ `13.5-in-app-notifications-sequence` |
| 13.6 | Email tài khoản (OTP, chào mừng, đổi mật khẩu, nhân viên) | ✔ `13.6-account-emails-class` | ✔ `13.6-account-emails-sequence` |

## Segment 14. Administration, Dashboards & Reports (UC07, UC33, UC34)
Backend: `AdminShopController`, `AdminCatalogController`, `AdminOversightController`, `AdminDashboardController`, `ShopReportController`; `AdminShopService`, `AdminCatalogService`, `AdminOversightService`, `AdminDashboardService`, `ShopReportService`.

| Mục | Chức năng | Class | Sequence |
|---|---|---|---|
| 14.1 | Quản lý shop (khóa, mở khóa, duyệt đơn đăng ký) | ✗ | ✗ |
| 14.2 | Quản lý danh mục sản phẩm | ✗ | ✗ |
| 14.3 | Admin xem đơn, hoàn tiền, đánh giá | ✗ | ✗ |
| 14.4 | Dashboard của Admin | ✗ | ✗ |
| 14.5 | Báo cáo và dashboard của shop | ✗ | ✗ |

## Quy ước đặt tên file
Mỗi sơ đồ đặt tên `<số mục>-<tên>-class` hoặc `<số mục>-<tên>-sequence`, nên số trong tên file luôn khớp số mục trong SDS. Tiêu đề trong sơ đồ cũng bắt đầu bằng số mục (ví dụ `3.7 Manual Status Change ...`). Toàn bộ sơ đồ nằm trong [docs/sds](.); thư mục `docs/diagrams` cũ đã được gộp vào đây. File có hậu tố `-alt` là bản vẽ thứ hai (rút gọn) của cùng một mục, giữ lại để đối chiếu.

Đã đổi tên: `9.2-customer-tracking` → `9.2-...`; `5.2-sepay-payment-return` → `5.2-...`; `3.7-manual-status-change` → `3.7-...`; `3.8-ghn-master-data` → `3.8-...`; `01` đến `10` trong `docs/diagrams` chuyển sang số mục tương ứng (2.7, 3.9, 3.2-alt, 3.7, 3.1-alt, 3.3-alt, 13.3, 5.4, 13.2, 13.4).

## Thống kê
- 88 mục con trong 14 segment, mỗi mục cần class và sequence (tổng 176 sơ đồ, một số dùng chung).
- ✔ đã có PlantUML đúng với code: 143 sơ đồ.
- ◐ chỉ có ảnh cũ trong Word (đã đối chiếu, đúng): 7 sơ đồ.
- Dùng chung với mục khác: 8 sơ đồ. Có một phần trong sơ đồ khác: 0 sơ đồ.
- ✗ chưa có: 18 sơ đồ.

Còn thiếu: segment 14 và một số sơ đồ cũ của segment 1 đến 3 (xem các dòng ◐ và ✗ ở trên). Segment 4 đến 13 đã có đủ class và sequence.
