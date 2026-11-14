# SDS: sơ đồ PlantUML đã sửa theo code

Phân đoạn mục II (Detailed Code Design) thành 14 segment và danh sách sơ đồ còn thiếu: [DETAILED_DESIGN_SEGMENTS.md](DETAILED_DESIGN_SEGMENTS.md).

Mỗi sơ đồ có file nguồn `.puml` và ảnh `.png` cùng tên trong thư mục này. Tên file dạng `<số mục>-<tên>-class|sequence`, số mục là số trong [DETAILED_DESIGN_SEGMENTS.md](DETAILED_DESIGN_SEGMENTS.md) và cũng nằm đầu tiêu đề sơ đồ. Thư mục `docs/diagrams` cũ đã gộp vào đây. Tất cả vẽ theo code hiện tại của `flowershop` và `flower-shop-client`. Bảng đánh số đầy đủ ở cuối file.

Render lại: `java -Dfile.encoding=UTF-8 -jar plantuml.jar -charset UTF-8 -tpng docs/sds/*.puml`

## Sơ đồ thay thế sơ đồ cũ trong SDS (SDS (1).docx)

| Mục SDS | Sơ đồ cũ | File mới | Đã sửa gì |
|---|---|---|---|
| I.1 Architecture | image73 | `1-architecture` | Giữ nguyên (đúng). Chỉ sửa chữ ở bảng thành phần, xem bên dưới |
| I.2 Package | image32 | `2-package-diagram` | Thêm package `chat`, `event`, `security`; `scheduler` liệt kê job |
| 1.3 Đăng ký | image58 | `1.3-registration-sequence` | Gửi mail qua `AuthMailEvent`; nhánh 409, 429; các bước sau xác thực |
| 1.4 Đăng nhập | image63 | `1.4-login-sequence` | `SessionController` → `TokenAuthService`; thứ tự rate limit, CAPTCHA, mật khẩu |
| 1.5 Quên mật khẩu | image28 | `1.5-password-recovery-sequence` | `AuthController` gọi thẳng `OtpService`; không có endpoint gửi lại riêng; thu hồi phiên, mail đổi mật khẩu |
| 1.6 Hồ sơ, avatar | image2, image3 | `1.6-profile-avatar-class`, `1.6-profile-avatar-sequence` | Bỏ `AvatarController`; `AccountService.uploadAvatar`; endpoint `/api/account/profile/avatar` |
| 1.7 Địa chỉ | image33 | `1.7-address-sequence` | `GhnAddressPicker` qua `/api/public/ghn/*`; nhánh 409 khi địa chỉ đã dùng trong đơn |
| 1.8 Khóa/mở khóa | image45 | `1.8-customer-lock-sequence` | Dùng `useConfirm()`; nhánh 409 khi mở khóa tài khoản `INACTIVE` |
| 2.1 Danh sách sản phẩm | image20, image76 | `2.1-product-list-class`, `2.1-product-list-sequence` | Thêm lọc hạn mức gói Pro |
| 2.2 Chi tiết, đánh giá | image35, image27 | `2.2-product-details-class`, `2.2-product-details-sequence` | Bỏ nhánh 401 (đánh giá xem công khai) |
| 2.3 Quản lý sản phẩm | image75, image25 | `2.3-product-crud-class`, `2.3-product-crud-sequence` | Thêm ảnh/video, xóa mềm, ẩn/hiện, Admin 404 |
| 2.4 Lưu yêu thích | image17 | `2.4-save-favorite-class` | Sửa stereotype repository |
| 2.5 Danh sách yêu thích | image79 | `2.5-favorite-list-sequence` | Sản phẩm ẩn vẫn nằm trong danh sách (`available = false`) |
| 2.6 Danh mục | image14, image36 | `2.6-categories-class`, `2.6-categories-sequence` | Đếm bằng Java, trường `group`, xử lý lỗi |
| 3.1 Chi tiết đơn (shop) | image49, image78 | `3.1-order-details-class`, `3.1-order-details-sequence` | Cho nhân viên đang làm việc; `CustomOrderWorkflowService.design` |
| 3.2 Hàng đợi đơn | image46, image69 | `3.2-order-queue-class`, `3.2-order-queue-sequence` | Endpoint `/queue`, `/queue/count`; hủy đơn hoàn kho, voucher, hoàn tiền; bắt buộc lý do |
| 3.3 Tạo vận đơn GHN | image37, image60 | `3.3-create-ghn-waybill-class`, `3.3-create-ghn-waybill-sequence` | Bỏ `resolveGhnAddress`; cho `DEPOSIT_PAID`; 409, 400; COD; lỗi GHN 502 |
| 7.2 Tạo voucher | image57, image11 | `7.2-create-voucher-class`, `7.2-create-voucher-sequence` | 11 trường; nhánh `productId` |
| 7.3 Sửa voucher | image10 | `7.3-update-voucher-sequence` | Nhánh `productId` |
| 7.6 Áp voucher | image47, image68 | `7.6-apply-voucher-class`, `7.6-apply-voucher-sequence` | Endpoint `/batch`; `save` thay `saveAndFlush` |
| 8.5 Xem gói của shop | image55 | `8.5-view-subscription-sequence` | Trả đủ sản phẩm nổi bật, không cắt theo hạn mức |
| 8.6 Đăng ký Pro | image66, image56 | `8.6-register-pro-class`, `8.6-register-pro-sequence` | `reconcile()` kiểm tra trạng thái trước khi gọi SePay |
| 8.7 Gia hạn Pro | image42 | `8.7-renew-pro-sequence` | Sửa thứ tự nhánh "đã SUCCESS" |
| 8.11 Nhắc hết hạn Pro | image64, image24 | `8.11-pro-expiry-reminder-class`, `8.11-pro-expiry-reminder-sequence` | Thêm bước đối soát hóa đơn SePay, phụ thuộc `ShopPlanService` |
| 8.12 Giới hạn gói | image80 | `8.12-enforce-limits-sequence` | Sửa nhãn nhánh; `assertCanCreate` nằm ở `ShopPlanService` |

## Sơ đồ mới (SDS chưa có)

| File | Luồng |
|---|---|
| `3.4-sync-ghn-status-shop-sequence` | Shop đồng bộ trạng thái GHN |
| `3.5-push-ghn-action-sequence` | Shop gửi hủy, trả hàng, giao lại sang GHN |
| `3.6-update-ghn-waybill-sequence` | Sửa vận đơn GHN |
| `9.2-customer-tracking-sequence` | Khách theo dõi đơn (3 bước), phí ship, tự đồng bộ khi mở trang |
| `3.7-manual-status-change-sequence` | Shop đổi trạng thái thủ công (không gọi GHN) |
| `3.8-ghn-master-data-sequence` | Danh mục địa chỉ GHN và tính phí khi đặt hàng |
| `10.1-accept-custom-quote-sequence` | Khách chấp nhận báo giá: FULL (COD/ONLINE) hoặc DEPOSIT (ONLINE); xem trước voucher (`CustomPaymentPreview`); các nhánh 404/409 |
| `10.2-custom-deposit-order-sequence` | Tạo đơn cọc từ yêu cầu đã chấp nhận (`makeCustomOrder`), AWAITING_DEPOSIT, cọc và phần còn lại qua SePay hoặc COD khi giao, hết hạn 24 giờ |
| `4.1-shopping-cart-class`, `4.1-shopping-cart-sequence` | Giỏ hàng: thêm, xem, sửa số lượng, xóa; giỏ khách trong trình duyệt và gộp vào tài khoản khi đăng nhập; job dọn giỏ bỏ quên 30 ngày |
| `4.2-shipping-fee-class`, `4.2-shipping-fee-sequence` | Tải địa chỉ đã lưu, tính phí ship GHN cho từng shop ở trang checkout; lỗi GHN 502 hiện theo từng shop |
| `4.3-multi-shop-checkout-class`, `4.3-multi-shop-checkout-sequence` | Chọn món ở giỏ, đặt hàng nhiều shop trong một transaction (`/batch`), tách đơn theo shop, `checkoutGroup`, kiểm tra `expectedTotal` |
| `4.4-cod-order-class`, `4.4-cod-order-sequence` | Đơn COD (Payment COD PENDING, đơn ở PENDING chờ shop xác nhận, không hết hạn), thông báo, mail biên lai, trang xác nhận `/payment/cod-success` |
| `5.1-create-sepay-payment-class`, `5.1-create-sepay-payment-sequence` | Tạo thanh toán SePay: đơn lẻ (FULL, DEPOSIT, FINAL) và gộp nhiều đơn; kiểm tra chủ đơn, trạng thái, hạn; server tự tính số tiền; ký HMAC; chuyển trình duyệt sang cổng |
| `5.2-sepay-payment-return-class` | Class diagram cho 5.2 (trước chỉ có sequence) |
| `5.3-expire-unpaid-order-class`, `5.3-expire-unpaid-order-sequence` | Job mỗi 5 giây hủy đơn quá hạn (PENDING 10 phút, AWAITING_DEPOSIT 24 giờ): hủy invoice ở SePay, đối soát lần cuối, hoàn kho, voucher, giỏ; bỏ qua đơn COD; đồng hồ đếm ngược ở `/payment/pending` |
| `5.2-sepay-payment-return-sequence` | Khách quay lại từ SePay (`/payment/{success\|error\|cancel}`), đối soát bằng REST (không webhook), hủy tại SePay theo từng trạng thái |

## Đã cập nhật sau merge

- `7.6-apply-voucher-sequence`: bỏ bước "hủy đơn PENDING cũ cùng sản phẩm" (đã xóa khỏi `OrderService.makeOrder`); kiểm tra thêm shop ACTIVE khi xem trước voucher.
- `10.1`, `10.2`, `10.3`: mới, vẽ theo code sau merge.
- Các sơ đồ 7.2, 7.3, 8.6, 8.7, 3.1 đến 3.8: đã đối chiếu tên method và thông báo lỗi với code, không cần sửa.

Mọi nhánh lỗi GHN (HTTP khác 200, body không phải JSON, mã GHN khác 200, mất kết nối) đều trả 502 với thông báo nêu rõ lỗi của GHN và được hiện trên màn hình.

## Chữ cần sửa trong SDS (bảng thành phần, mục I.1 và I.2)

| Mục | Sửa thành |
|---|---|
| 05 Order & Delivery Services | Vòng đời đơn: Awaiting Quote → Quoted → Pending → Awaiting Deposit → Deposit Paid → Processing → Delivering → Completed, hoặc Cancelled / Refunded. Hệ thống không nhận callback từ GHN: trạng thái chỉ đổi khi shop hoặc khách bấm đồng bộ (khách còn được đồng bộ tự động một lần khi mở trang đơn đang giao). Bỏ "Simulating completion" |
| 08 Cart & Payment Services | Thanh toán qua SePay (Full, Deposit, Final) hoặc COD; số tiền do server tính |
| 09 GhnService | Danh mục tỉnh, quận, phường (`provinces`, `districts`, `wards`), `calculateFee`, `createOrder`, `track`, `switchStatus` (hủy, trả hàng, giao lại), `updateOrder`. Địa chỉ gửi bằng tên phường, quận, tỉnh. Mọi lỗi GHN trả 502 kèm thông báo nêu rõ lỗi của GHN. Bỏ "tự phân giải mã địa chỉ 3 cấp" |
| 11 SePayGatewayService | Hệ thống không nhận webhook của SePay; backend hỏi SePay kết quả khi khách quay lại hoặc khi kiểm tra trạng thái |
| 14 Relational Database | Chỉ MySQL 8 (`flower_shop_db`), bỏ PostgreSQL |
| Package 09 `service` | Thêm GHN vào danh sách tích hợp (SePay, Cloudinary, SMTP, GHN) |
| Package (thêm) | `chat` (REST và STOMP `/ws`), `event` (sự kiện sau commit, gửi mail), `security` (JWT, CSRF, rate limit, CAPTCHA) |
| Package 13 `scheduler` | `PaymentTimeoutScheduler`, `MaintenanceJob`, `OccasionReminderJob`, `ShopProductMailJob`, `ShopSubscriptionJob` |
| Mục 1 | Đánh số lại (hiện bắt đầu từ 1.3) |
| Mục 6.2 | Dùng riêng sơ đồ Follow Shop (hiện dùng lại ảnh 6.1) |
| Mục "4", "45", "10" | Tiêu đề bị gộp hoặc trống: Cart & Checkout, Payment & Refund, Order for requirement chưa có sơ đồ |

## Chưa làm

- ERD (mục I.3) chưa vẽ lại; nguồn là [../database.mmd](../database.mmd), khớp 44 bảng trong entity (chỉ `Blog_Images` không thấy entity).
- Chưa có sơ đồ cho segment 14 và một số mục của segment 1 đến 3 (xem [DETAILED_DESIGN_SEGMENTS.md](DETAILED_DESIGN_SEGMENTS.md)). Segment 4 đến 13 đã có đủ.
- File `SDS (1).docx` chưa được sửa. Các ảnh ở bảng trên cần được thay trong Word bằng PNG mới.

## Bảng đánh số đầy đủ

| Mục | File | Loại | Tiêu đề |
|---|---|---|---|
| - | `1-architecture` | khác | FlowerShop - System Architecture |
| - | `2-package-diagram` | khác | FlowerShop - Package diagram |
| 1.3 | `1.3-registration-sequence` | sequence | 1.3 Register and Verify Email - sequence diagram |
| 1.4 | `1.4-login-sequence` | sequence | 1.4 Login and Session Refresh - sequence diagram |
| 1.5 | `1.5-password-recovery-sequence` | sequence | 1.5 Password Recovery - sequence diagram |
| 1.6 | `1.6-profile-avatar-class` | class | 1.6 Personal Profile and Avatar - class diagram |
| 1.6 | `1.6-profile-avatar-sequence` | sequence | 1.6 Upload and Remove Avatar - sequence diagram |
| 1.7 | `1.7-address-sequence` | sequence | 1.7 Manage Customer Address - sequence diagram |
| 1.8 | `1.8-customer-lock-sequence` | sequence | 1.8 Lock or Unlock Customer Account - sequence diagram |
| 2.1 | `2.1-product-list-class` | class | 2.1 Display Product List - class diagram |
| 2.1 | `2.1-product-list-sequence` | sequence | 2.1 Display Product List - sequence diagram |
| 2.2 | `2.2-product-details-class` | class | 2.2 Product Details and Reviews - class diagram |
| 2.2 | `2.2-product-details-sequence` | sequence | 2.2 Product Details and Reviews - sequence diagram |
| 2.3 | `2.3-product-crud-class` | class | 2.3 Product Management - class diagram |
| 2.3 | `2.3-product-crud-sequence` | sequence | 2.3 Shop/Admin Product Management - sequence diagram |
| 2.4 | `2.4-save-favorite-class` | class | 2.4 Save Favorite Product - class diagram |
| 2.5 | `2.5-favorite-list-sequence` | sequence | 2.5 Favorite Product List - sequence diagram |
| 2.6 | `2.6-categories-class` | class | 2.6 Display Categories - class diagram |
| 2.6 | `2.6-categories-sequence` | sequence | 2.6 Display Categories - sequence diagram |
| 2.7 | `2.7-home-page-class` | class | 2.7 Home Page - class diagram |
| 2.7 | `2.7-home-page-sequence` | sequence | 2.7 Home Page - sequence diagram |
| 3.1 | `3.1-order-details-alt-class` | class | 3.1 View Order Details (alternative view) - class diagram |
| 3.1 | `3.1-order-details-alt-sequence` | sequence | 3.1 View Order Details (alternative view) - sequence diagram |
| 3.1 | `3.1-order-details-class` | class | 3.1 Order Details (Shop Manager) - class diagram |
| 3.1 | `3.1-order-details-sequence` | sequence | 3.1 Order Details (Shop Manager) - sequence diagram |
| 3.2 | `3.2-order-queue-alt-class` | class | 3.2 Order Queue (alternative view) - class diagram |
| 3.2 | `3.2-order-queue-alt-sequence` | sequence | 3.2 Order Queue (alternative view) - sequence diagram |
| 3.2 | `3.2-order-queue-class` | class | 3.2 Order Queue and Cancel - class diagram |
| 3.2 | `3.2-order-queue-sequence` | sequence | 3.2 Order Queue and Cancel (Shop Manager) - sequence diagram |
| 3.3 | `3.3-create-ghn-waybill-alt-class` | class | 3.3 Assign Order to Delivery (alternative view) - class diagram |
| 3.3 | `3.3-create-ghn-waybill-alt-sequence` | sequence | 3.3 Assign Order to Delivery (alternative view) - sequence diagram |
| 3.3 | `3.3-create-ghn-waybill-class` | class | 3.3 Create GHN Waybill - class diagram |
| 3.3 | `3.3-create-ghn-waybill-sequence` | sequence | 3.3 Create GHN Waybill - sequence diagram |
| 3.4 | `3.4-sync-ghn-status-shop-sequence` | sequence | 3.4 Sync GHN Status (Shop) - sequence diagram |
| 3.5 | `3.5-push-ghn-action-sequence` | sequence | 3.5 Push GHN Action (cancel / return / storing) - sequence diagram |
| 3.6 | `3.6-update-ghn-waybill-sequence` | sequence | 3.6 Update GHN Waybill - sequence diagram |
| 3.7 | `3.7-manual-status-change-sequence` | sequence | 3.7 Manual Status Change (DB only, no GHN call) - sequence diagram |
| 3.7 | `3.7-update-order-status-alt-sequence` | sequence | 3.7 Update Order Status (alternative view) - sequence diagram |
| 3.7 | `3.7-update-order-status-class` | class | 3.7 Update Order Status - class diagram |
| 3.8 | `3.8-ghn-master-data-sequence` | sequence | 3.8 GHN Master Data and Shipping Fee Quote - sequence diagram |
| 3.9 | `3.9-order-management-class` | class | 3.9 Order Management - class diagram |
| 3.9 | `3.9-order-management-sequence` | sequence | 3.9 Order Management - list, filter, edit - sequence diagram |
| 4.1 | `4.1-shopping-cart-class` | class | 4.1 Shopping Cart - class diagram |
| 4.1 | `4.1-shopping-cart-sequence` | sequence | 4.1 Shopping Cart - sequence diagram |
| 4.2 | `4.2-shipping-fee-class` | class | 4.2 Shipping Fee at Checkout - class diagram |
| 4.2 | `4.2-shipping-fee-sequence` | sequence | 4.2 Shipping Fee at Checkout - sequence diagram |
| 4.3 | `4.3-multi-shop-checkout-class` | class | 4.3 Multi-shop Checkout - class diagram |
| 4.3 | `4.3-multi-shop-checkout-sequence` | sequence | 4.3 Multi-shop Checkout - sequence diagram |
| 4.4 | `4.4-cod-order-class` | class | 4.4 COD Order and Confirmation Page - class diagram |
| 4.4 | `4.4-cod-order-sequence` | sequence | 4.4 COD Order and Confirmation Page - sequence diagram |
| 5.1 | `5.1-create-sepay-payment-class` | class | 5.1 Create SePay Payment - class diagram |
| 5.1 | `5.1-create-sepay-payment-sequence` | sequence | 5.1 Create SePay Payment - sequence diagram |
| 5.2 | `5.2-sepay-payment-return-class` | class | 5.2 SePay Return and Reconcile - class diagram |
| 5.2 | `5.2-sepay-payment-return-sequence` | sequence | 5.2 SePay Return, Reconcile and Cancel (no webhook) - sequence diagram |
| 5.3 | `5.3-expire-unpaid-order-class` | class | 5.3 Expire Unpaid Order - class diagram |
| 5.3 | `5.3-expire-unpaid-order-sequence` | sequence | 5.3 Expire Unpaid Order - sequence diagram |
| 5.4 | `5.4-refund-management-class` | class | 5.4 Refund Management - class diagram |
| 5.4 | `5.4-refund-management-sequence` | sequence | 5.4 Refund Management - sequence diagram |
| 6.1 | `6.1-followed-shops-class` | class | 6.1 Followed Shops - class diagram |
| 6.1 | `6.1-followed-shops-sequence` | sequence | 6.1 Followed Shops - sequence diagram |
| 6.2 | `6.2-follow-shop-class` | class | 6.2 Follow Shop - class diagram |
| 6.2 | `6.2-follow-shop-sequence` | sequence | 6.2 Follow Shop - sequence diagram |
| 6.3 | `6.3-unfollow-shop-class` | class | 6.3 Unfollow Shop - class diagram |
| 6.3 | `6.3-unfollow-shop-sequence` | sequence | 6.3 Unfollow Shop - sequence diagram |
| 6.4 | `6.4-new-product-email-class` | class | 6.4 New Product Email - class diagram |
| 6.4 | `6.4-new-product-email-sequence` | sequence | 6.4 New Product Email - sequence diagram |
| 7.1 | `7.1-view-vouchers-class` | class | 7.1 View Vouchers - class diagram |
| 7.1 | `7.1-view-vouchers-sequence` | sequence | 7.1 View Vouchers - sequence diagram |
| 7.2 | `7.2-create-voucher-class` | class | 7.2 Create Voucher - class diagram |
| 7.2 | `7.2-create-voucher-sequence` | sequence | 7.2 Create Voucher - sequence diagram |
| 7.3 | `7.3-update-voucher-sequence` | sequence | 7.3 Update Voucher - sequence diagram |
| 7.4 | `7.4-delete-voucher-class` | class | 7.4 Delete Voucher - class diagram |
| 7.4 | `7.4-delete-voucher-sequence` | sequence | 7.4 Delete Voucher - sequence diagram |
| 7.5 | `7.5-checkout-vouchers-class` | class | 7.5 Checkout Vouchers - class diagram |
| 7.5 | `7.5-checkout-vouchers-sequence` | sequence | 7.5 Checkout Vouchers - sequence diagram |
| 7.6 | `7.6-apply-voucher-class` | class | 7.6 Apply Voucher - class diagram |
| 7.6 | `7.6-apply-voucher-sequence` | sequence | 7.6 Apply Voucher - sequence diagram |
| 7.7 | `7.7-remove-voucher-class` | class | 7.7 Remove Voucher - class diagram |
| 7.7 | `7.7-remove-voucher-sequence` | sequence | 7.7 Remove Voucher - sequence diagram |
| 7.8 | `7.8-best-voucher-class` | class | 7.8 Best Voucher - class diagram |
| 7.8 | `7.8-best-voucher-sequence` | sequence | 7.8 Best Voucher - sequence diagram |
| 8.1 | `8.1-view-plans-class` | class | 8.1 View Pro Plans - class diagram |
| 8.1 | `8.1-view-plans-sequence` | sequence | 8.1 View Pro Plans - sequence diagram |
| 8.2 | `8.2-create-plan-class` | class | 8.2 Create Pro Plan - class diagram |
| 8.2 | `8.2-create-plan-sequence` | sequence | 8.2 Create Pro Plan - sequence diagram |
| 8.3 | `8.3-update-plan-class` | class | 8.3 Update Pro Plan - class diagram |
| 8.3 | `8.3-update-plan-sequence` | sequence | 8.3 Update Pro Plan - sequence diagram |
| 8.4 | `8.4-deactivate-plan-class` | class | 8.4 Deactivate Pro Plan - class diagram |
| 8.4 | `8.4-deactivate-plan-sequence` | sequence | 8.4 Deactivate Pro Plan - sequence diagram |
| 8.5 | `8.5-view-subscription-class` | class | 8.5 View Shop Subscription - class diagram |
| 8.5 | `8.5-view-subscription-sequence` | sequence | 8.5 View Shop Subscription - sequence diagram |
| 8.6 | `8.6-register-pro-class` | class | 8.6 Register Pro - class diagram |
| 8.6 | `8.6-register-pro-sequence` | sequence | 8.6 Register Pro - sequence diagram |
| 8.7 | `8.7-renew-pro-sequence` | sequence | 8.7 Renew Pro - sequence diagram |
| 8.8 | `8.8-admin-grant-pro-class` | class | 8.8 Admin Grants Pro - class diagram |
| 8.8 | `8.8-admin-grant-pro-sequence` | sequence | 8.8 Admin Grants Pro - sequence diagram |
| 8.9 | `8.9-admin-confirm-pro-payment-class` | class | 8.9 Admin Confirms Pro Payment - class diagram |
| 8.9 | `8.9-admin-confirm-pro-payment-sequence` | sequence | 8.9 Admin Confirms Pro Payment - sequence diagram |
| 8.10 | `8.10-featured-products-class` | class | 8.10 Featured Products - class diagram |
| 8.10 | `8.10-featured-products-sequence` | sequence | 8.10 Featured Products - sequence diagram |
| 8.11 | `8.11-pro-expiry-reminder-class` | class | 8.11 Pro Expiry Reminder - class diagram |
| 8.11 | `8.11-pro-expiry-reminder-sequence` | sequence | 8.11 Pro Expiry Reminder - sequence diagram |
| 8.12 | `8.12-enforce-limits-class` | class | 8.12 Enforce Subscription Limits - class diagram |
| 8.12 | `8.12-enforce-limits-sequence` | sequence | 8.12 Enforce Subscription Limits - sequence diagram |
| 9.1 | `9.1-customer-order-list-class` | class | 9.1 Customer Order List - class diagram |
| 9.1 | `9.1-customer-order-list-sequence` | sequence | 9.1 Customer Order List - sequence diagram |
| 9.2 | `9.2-customer-tracking-class` | class | 9.2 Customer Order Tracking - class diagram |
| 9.2 | `9.2-customer-tracking-sequence` | sequence | 9.2 Customer Order Tracking - sequence diagram |
| 9.3 | `9.3-customer-cancel-order-class` | class | 9.3 Customer Cancels Order - class diagram |
| 9.3 | `9.3-customer-cancel-order-sequence` | sequence | 9.3 Customer Cancels Order - sequence diagram |
| 9.4 | `9.4-invoice-transactions-class` | class | 9.4 Invoice and Transaction History - class diagram |
| 9.4 | `9.4-invoice-transactions-sequence` | sequence | 9.4 Invoice and Transaction History - sequence diagram |
| 9.5 | `9.5-customer-dashboard-class` | class | 9.5 Customer Dashboard - class diagram |
| 9.5 | `9.5-customer-dashboard-sequence` | sequence | 9.5 Customer Dashboard - sequence diagram |
| 10.1 | `10.1-accept-custom-quote-class` | class | 10.1 Accept Shop Quote - class diagram |
| 10.1 | `10.1-accept-custom-quote-sequence` | sequence | 10.1 Accept Shop Quote (custom order, FULL or DEPOSIT) - sequence diagram |
| 10.2 | `10.2-custom-deposit-order-class` | class | 10.2 Custom Deposit Order - class diagram |
| 10.2 | `10.2-custom-deposit-order-sequence` | sequence | 10.2 Custom Deposit Order (from accepted request) - sequence diagram |
| 10.3 | `10.3-create-custom-request-class` | class | 10.3 Create Custom Request - class diagram |
| 10.3 | `10.3-create-custom-request-sequence` | sequence | 10.3 Create Custom Request - sequence diagram |
| 10.4 | `10.4-shop-quote-custom-request-class` | class | 10.4 Shop Quote Custom Request - class diagram |
| 10.4 | `10.4-shop-quote-custom-request-sequence` | sequence | 10.4 Shop Quote Custom Request - sequence diagram |
| 11.1 | `11.1-product-review-class` | class | 11.1 Product Review and Shop Reply - class diagram |
| 11.1 | `11.1-product-review-sequence` | sequence | 11.1 Product Review and Shop Reply - sequence diagram |
| 11.2 | `11.2-customer-blog-class` | class | 11.2 Care Blog for Customers - class diagram |
| 11.2 | `11.2-customer-blog-sequence` | sequence | 11.2 Care Blog for Customers - sequence diagram |
| 11.3 | `11.3-shop-blog-management-class` | class | 11.3 Shop Manages Blog Posts - class diagram |
| 11.3 | `11.3-shop-blog-management-sequence` | sequence | 11.3 Shop Manages Blog Posts - sequence diagram |
| 11.4 | `11.4-chat-class` | class | 11.4 Chat between Customer and Shop - class diagram |
| 11.4 | `11.4-chat-sequence` | sequence | 11.4 Chat between Customer and Shop - sequence diagram |
| 12.1 | `12.1-shop-owner-registration-class` | class | 12.1 Shop Owner Registration - class diagram |
| 12.1 | `12.1-shop-owner-registration-sequence` | sequence | 12.1 Shop Owner Registration - sequence diagram |
| 12.2 | `12.2-manage-shop-info-class` | class | 12.2 Manage Shop Info - class diagram |
| 12.2 | `12.2-manage-shop-info-sequence` | sequence | 12.2 Manage Shop Info - sequence diagram |
| 12.3 | `12.3-staff-application-class` | class | 12.3 Staff Application - class diagram |
| 12.3 | `12.3-staff-application-sequence` | sequence | 12.3 Staff Application - sequence diagram |
| 12.4 | `12.4-staff-invitation-class` | class | 12.4 Staff Invitation - class diagram |
| 12.4 | `12.4-staff-invitation-sequence` | sequence | 12.4 Staff Invitation - sequence diagram |
| 12.5 | `12.5-staff-management-class` | class | 12.5 Staff Management - class diagram |
| 12.5 | `12.5-staff-management-sequence` | sequence | 12.5 Staff Management - sequence diagram |
| 13.1 | `13.1-manage-occasions-class` | class | 13.1 Manage Occasions - class diagram |
| 13.1 | `13.1-manage-occasions-sequence` | sequence | 13.1 Manage Occasions - sequence diagram |
| 13.2 | `13.2-notify-upcoming-occasion-class` | class | 13.2 Notify Upcoming Occasion via Email - class diagram |
| 13.2 | `13.2-notify-upcoming-occasion-sequence` | sequence | 13.2 Notify Upcoming Occasion via Email - sequence diagram |
| 13.3 | `13.3-notify-user-confirmation-class` | class | 13.3 Notify User Confirmation - class diagram |
| 13.3 | `13.3-notify-user-confirmation-sequence` | sequence | 13.3 Notify User Confirmation - sequence diagram |
| 13.4 | `13.4-notify-order-delivered-class` | class | 13.4 Notify Order Delivered via Email - class diagram |
| 13.4 | `13.4-notify-order-delivered-sequence` | sequence | 13.4 Notify Order Delivered via Email - sequence diagram |
| 13.5 | `13.5-in-app-notifications-class` | class | 13.5 In-App Notifications - class diagram |
| 13.5 | `13.5-in-app-notifications-sequence` | sequence | 13.5 In-App Notifications - sequence diagram |
| 13.6 | `13.6-account-emails-class` | class | 13.6 Account Emails - class diagram |
| 13.6 | `13.6-account-emails-sequence` | sequence | 13.6 Account Emails - sequence diagram |

Tổng cộng 152 sơ đồ (69 class, 81 sequence, 2 sơ đồ kiến trúc và package).
