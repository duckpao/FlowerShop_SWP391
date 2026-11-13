# SDS: sơ đồ PlantUML đã sửa theo code

Phân đoạn mục II (Detailed Code Design) thành 14 segment và danh sách sơ đồ còn thiếu: [DETAILED_DESIGN_SEGMENTS.md](DETAILED_DESIGN_SEGMENTS.md).

Mỗi sơ đồ có file nguồn `.puml` và ảnh `.png` cùng tên trong thư mục này. Tất cả vẽ theo code hiện tại của `flowershop` và `flower-shop-client`, style giống [../diagrams/](../diagrams/).

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
| `3.7-customer-tracking-sequence` | Khách theo dõi đơn (3 bước), phí ship, tự đồng bộ khi mở trang |
| `3.8-manual-status-change-sequence` | Shop đổi trạng thái thủ công (không gọi GHN) |
| `3.9-ghn-master-data-sequence` | Danh mục địa chỉ GHN và tính phí khi đặt hàng |
| `10.1-accept-custom-quote-sequence` | Khách chấp nhận báo giá: FULL (COD/ONLINE) hoặc DEPOSIT (ONLINE); xem trước voucher (`CustomPaymentPreview`); các nhánh 404/409 |
| `10.2-custom-deposit-order-sequence` | Tạo đơn cọc từ yêu cầu đã chấp nhận (`makeCustomOrder`), AWAITING_DEPOSIT, cọc và phần còn lại qua SePay hoặc COD khi giao, hết hạn 24 giờ |
| `10.3-sepay-payment-return-sequence` | Khách quay lại từ SePay (`/payment/{success\|error\|cancel}`), đối soát bằng REST (không webhook), hủy tại SePay theo từng trạng thái |

## Đã cập nhật sau merge

- `7.6-apply-voucher-sequence`: bỏ bước "hủy đơn PENDING cũ cùng sản phẩm" (đã xóa khỏi `OrderService.makeOrder`); kiểm tra thêm shop ACTIVE khi xem trước voucher.
- `10.1`, `10.2`, `10.3`: mới, vẽ theo code sau merge.
- Các sơ đồ 7.2, 7.3, 8.6, 8.7, 3.1 đến 3.9: đã đối chiếu tên method và thông báo lỗi với code, không cần sửa.

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
- Chưa có sơ đồ cho: giỏ hàng, checkout, thanh toán SePay, hoàn tiền, chat, blog, đánh giá, tuyển nhân viên, dashboard, nhắc dịp đặc biệt. Bản nháp một phần đã có trong [../diagrams/](../diagrams/) (`04` đến `10`).
- File `SDS (1).docx` chưa được sửa. Các ảnh ở bảng trên cần được thay trong Word bằng PNG mới.
