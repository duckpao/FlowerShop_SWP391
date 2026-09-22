# Đăng ký shop và lời mời Staff

## Đăng ký Manager thật

1. Chạy backend bình thường và frontend. Trên trang Đăng ký chọn **Shop Manager — đăng ký cửa hàng**.
2. Dùng email chưa có tài khoản, nhập tên người quản lý, mật khẩu, tên và mô tả shop.
3. Nhận OTP và xác nhận. Trước OTP đúng không tạo Users hoặc Shops; thông tin ở Pending_Registrations.
4. OTP đúng tạo User SHOP, ACTIVE, email verified và đúng một shop PENDING trong cùng transaction.
5. Đăng nhập: Manager xem shop, sửa hồ sơ/địa chỉ nhưng chưa quản lý nhân viên khi shop chưa được duyệt.
6. Admin duyệt shop trong mục Quản lý cửa hàng. Có thể dùng dev-admin để test duyệt, còn Manager đăng nhập thật.

API POST `/api/auth/register-shop` (công khai nhưng cần CSRF):

```json
{
  "account": {"email":"your-email@example.com","password":"FlowerShop123","fullName":"Tên Manager"},
  "shopName":"Tên cửa hàng",
  "description":"Mô tả cửa hàng"
}
```

Tiếp tục dùng POST `/api/auth/verify-email` và `/api/auth/resend-verification` như đăng ký Customer.
Đăng ký công khai không nhận role tùy ý. Request cùng email trong thời gian chờ gửi lại không thay thông tin
của OTP đã phát; khi phát mã mới, mã cũ không dùng được. Một Manager chỉ sở hữu một shop (unique owner_id).
Email đã có tài khoản không được đăng ký ghi đè hay tự chuyển thành Manager; dùng email mới.

## Manager mời nhân viên

1. Đăng nhập Manager thật có shop ACTIVE, mở **Cửa hàng của tôi → Mời nhân viên qua email**.
2. Nhập email, bấm gửi. Người nhận nhận thư chứa mã lời mời (không phải OTP 6 số).
3. Người nhận chưa có tài khoản: đăng ký loại Khách hàng/người nhận lời mời, tự đặt mật khẩu và xác thực OTP.
4. Đăng nhập bằng đúng email nhận thư. Tại **Lời mời nhân viên**, dán mã và chọn đồng ý tham gia.
5. Customer được chuyển thành SHOP_STAFF theo sự đồng ý trên form. Dữ liệu khách hàng cũ vẫn giữ,
   nhưng quyền API riêng Customer không còn; hệ thống hiện dùng một role trên tài khoản.
6. Mọi phiên cũ bị thu hồi. Đăng nhập lại để thấy shop được phân công.
7. Manager tải lại cửa hàng/danh sách nhân viên để thấy Staff mới; có thể ngừng/bật quyền tại shop.

Mã gồm ID và chuỗi ngẫu nhiên 256-bit, chỉ hash SHA-256 được lưu ở DB; raw code chỉ đi trong email,
không trả về API hoặc ghi log. Hết hạn 24 giờ, chỉ sử dụng một lần, ràng buộc với email đăng nhập.
Gửi lại cách tối thiểu 60 giây, tối đa 3 lần/email/shop trong một giờ; tối đa 20 email khác nhau/shop
được gửi trong một giờ. Gửi lại vô hiệu mã cũ. Manager có thể hủy lời mời chờ xác nhận.
Không nhận lời mời khi shop bị khóa hoặc tài khoản không ACTIVE/chưa xác thực.
ADMIN, SHOP, DELIVERY không được chuyển thành Staff qua lời mời.
Lời mời không tự cấp quyền; account hiện có không bị đổi mật khẩu. Người đã có liên kết (kể cả bị ngừng quyền)
phải dùng quản lý nhân viên, không dùng lời mời để tự khôi phục quyền.

| Method | URL | Quyền |
| --- | --- | --- |
| GET | `/api/shop/mine/{id}/invitations` | Manager sở hữu shop |
| POST | `/api/shop/mine/{id}/invitations` | Body email, shop ACTIVE |
| DELETE | `/api/shop/mine/{id}/invitations/{invitationId}` | Hủy lời mời |
| POST | `/api/account/staff-invitations/accept` | Customer/Staff đã đăng nhập, body code |

Tất cả thao tác ghi cần CSRF. JWT phải đúng role, chủ shop phải khớp; API không nhận actor/userId từ client.
Danh sách lời mời trả trạng thái/email/thời hạn, không trả mã/hash. Dev-manager vẫn chỉ dùng identity local
cho gửi/xem/hủy; chấp nhận lời mời luôn cần tài khoản thật đăng nhập.

## Chạy

Backend trong flowershop:

```powershell
.\gradlew.bat bootRun
```

Nếu chưa có tài khoản Admin để duyệt, chỉ bật Admin thử nghiệm:

```powershell
.\gradlew.bat bootRun --args="--spring.profiles.active=dev-admin"
```

Frontend: `npm.cmd run dev`. Manager và Staff dùng http://localhost:8080 ; Admin local dùng /dev-admin.
Không public profile thử nghiệm ra mạng. SMTP lấy từ application-mail-local.properties hiện có.
Mail gửi sau commit, chưa có outbox/retry bền vững: 202 là tiếp nhận, không đảm bảo thư đã tới hộp thư.
Nếu SMTP lỗi, kiểm tra cấu hình rồi gửi lại sau cooldown. Không cần gửi mật khẩu cho Manager/Admin.

Migration `flowershop/sql/008_shop_onboarding_invitations.sql` đã áp dụng local; máy khác chạy một lần.
Không chạy lại database.sql trên DB có dữ liệu. Chưa thay đổi role/mật khẩu của tài khoản đang có khi triển khai.

## Test tự động

```powershell
.\gradlew.bat test --tests com.example.flowershop.OnboardingTests
```

Kiểm tra OTP và tạo shop atomically, chờ duyệt, người nhận mới/cũ, sai email, hết hạn, hủy/gửi lại,
shop bị khóa, không chuyển role đặc quyền, thu hồi phiên và chấp nhận đồng thời. Mail dùng giả lập.
Catalog/đơn hàng là giai đoạn nghiệp vụ tiếp theo, không thuộc luồng cấp tài khoản này.
