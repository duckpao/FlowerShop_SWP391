# Hồ sơ cá nhân và địa chỉ giao hàng

## Phạm vi

- Admin, Customer, Shop Manager (`SHOP`) và Shop Staff được xem/sửa hồ sơ của chính mình.
- Chỉ Customer quản lý địa chỉ giao hàng. API này không sửa địa chỉ cửa hàng.
- Hồ sơ cho sửa họ tên và điện thoại; email/role/trạng thái/mật khẩu không được sửa qua API này.
- Các nghiệp vụ đặt hoa, thanh toán, chat, quản lý nhân viên và duyệt shop chưa nằm trong hai chức năng này.

## API

Tất cả API yêu cầu `Authorization: Bearer <accessToken>`.
Với POST/PUT/DELETE, lấy CSRF mới qua GET `/api/auth/csrf` và gửi header
`X-CSRF-TOKEN` trong cùng phiên trình duyệt. Frontend tự thực hiện.

| Method | URL | Chức năng |
| --- | --- | --- |
| GET | `/api/account/profile` | Xem hồ sơ cá nhân |
| PUT | `/api/account/profile` | Sửa họ tên, điện thoại |
| GET | `/api/account/delivery-areas` | Danh sách thành phố được hỗ trợ |
| GET | `/api/account/addresses` | Danh sách địa chỉ của Customer hiện tại |
| POST | `/api/account/addresses` | Thêm địa chỉ, trả 201 |
| PUT | `/api/account/addresses/{id}` | Sửa địa chỉ |
| PUT | `/api/account/addresses/{id}/default` | Chọn địa chỉ mặc định; không cần body |
| DELETE | `/api/account/addresses/{id}` | Xóa địa chỉ, trả 204 |

PUT hồ sơ:

```json
{"fullName":"Nguyễn Văn An","phone":"0912345678"}
```

Họ tên bắt buộc, tối đa 100 ký tự. Điện thoại có thể là chuỗi rỗng;
nếu nhập thì 9–15 chữ số, cho phép dấu `+` ở đầu.

POST/PUT địa chỉ (ví dụ dữ liệu nhập, không phải địa chỉ được xác minh):

```json
{
  "addressLine":"Số nhà và tên đường của bạn",
  "city":"Hà Nội",
  "district":"Khu vực của bạn",
  "ward":"Phường/xã của bạn",
  "isDefault":false
}
```

Backend lấy chủ sở hữu từ JWT, không nhận userId từ client.
ID thuộc người khác hoặc địa chỉ shop trả 404; sai role trả 403.
Giới hạn tối đa 10 địa chỉ/tài khoản. Địa chỉ đầu tiên tự làm mặc định.
Khi chọn mặc định mới, mặc định cũ bị bỏ trong cùng transaction; các thao tác
địa chỉ của cùng tài khoản được tuần tự hóa bằng khóa bản ghi User.
Muốn bỏ mặc định của địa chỉ hiện tại, chọn một địa chỉ khác làm mặc định.
Xóa địa chỉ mặc định sẽ chọn địa chỉ còn lại đầu tiên theo ngày tạo và ID.
Địa chỉ đang được tham chiếu bởi Orders không được sửa/xóa (409); có thể thêm địa chỉ mới.

## Giới hạn Hà Nội

`flowershop/src/main/resources/application.properties` có:

```properties
app.delivery.allowed-cities=\u0048\u00e0 \u004e\u1ed9\u0069
```

Đây là Unicode escape của `Hà Nội`. Nhiều thành phố cấu hình cách nhau bằng `;`.
Frontend lấy danh sách từ API để hiển thị dropdown; backend kiểm tra lại thành phố.
Tên phải khớp danh sách (sau khi bỏ khoảng trắng đầu/cuối).
Phường/xã, quận/huyện dùng các trường có sẵn trong DB và hiện nhập thủ công.
Chức năng này giới hạn thành phố được khai báo, chưa kiểm chứng địa chỉ thực,
quan hệ phường–thành phố hoặc tọa độ. Checkout/tính phí giao hàng sau này phải
kiểm tra lại khu vực và tích hợp danh mục địa chỉ/đơn vị giao hàng nếu cần.

## Test trên giao diện

1. Khởi động lại backend: trong `flowershop`, chạy `./gradlew.bat bootRun`.
2. Trong `flower-shop-client`, chạy `npm.cmd run dev` và mở URL Vite.
3. Đăng nhập tài khoản đã xác thực. Trang tài khoản hiển thị hồ sơ.
4. Sửa tên/điện thoại, lưu rồi reload để kiểm tra dữ liệu được lưu.
5. Với Customer: thêm hai địa chỉ ở Hà Nội, đổi mặc định, sửa và xóa.
6. Thử gửi city khác bằng Swagger: phải trả 400.
7. Dùng tài khoản Customer khác thử sửa ID địa chỉ của tài khoản đầu: phải trả 404.

Không cần migration mới cho hai chức năng này: dùng bảng Users, Addresses, Orders hiện có.
Không chạy lại database.sql trên DB đang dùng.

## Test tự động

```powershell
.\gradlew.bat test --tests com.example.flowershop.AccountTests --tests com.example.flowershop.JwtAuthenticationTests --tests com.example.flowershop.EmailVerificationTests
```

Test dùng H2 và mail giả lập. Bao gồm quyền sở hữu, role, validation, CSRF,
không sửa được role/email/password bằng hồ sơ, giới hạn Hà Nội, số lượng địa chỉ,
địa chỉ đã dùng trong đơn hàng và hai yêu cầu tạo địa chỉ mặc định đồng thời.

Frontend MVC: `models/accountModel.js`, `controllers/useAccountController.js`,
`views/AccountView.jsx`, `services/accountService.js`, `styles/account.css`.
