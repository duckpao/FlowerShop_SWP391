# Đăng ký và quên mật khẩu bằng OTP

## Giao diện đăng ký mới

Trong `flower-shop-client`, chạy `npm.cmd install` và `npm.cmd run dev`.
Mở http://localhost:5173, điền họ tên/email/mật khẩu và xác nhận mật khẩu.
Bấm Đăng ký → hiện ô Nhập mã OTP → nhập đúng mã trong email → quay về Sign in.
Giao diện tự lấy CSRF token và gửi cookie; không cần thao tác Swagger.
Trang Sign in hiện mới có giao diện, nút đăng nhập chưa mở vì API login chưa được triển khai.

### Quên mật khẩu trên giao diện

Tại Sign in, bấm **Quên mật khẩu?** → nhập email tài khoản đã xác thực →
**Gửi mã OTP**. Giao diện mở form mật khẩu mới, xác nhận mật khẩu và OTP 6 số.
Điền mã nhận trong email rồi bấm **Đổi mật khẩu**. Khi thành công, giao diện xóa
mật khẩu/OTP khỏi form và trở lại Sign in; backend gửi email thông báo đổi mật khẩu.
Nút **Gửi lại mã OTP** gọi forgot-password, không gọi resend-verification của đăng ký.
Mã sai/hết hạn và mật khẩu xác nhận không khớp hiển thị lỗi, không chuyển trang.
Các tài khoản chưa xác thực, bị khóa hoặc email không tồn tại không nhận OTP reset.

Để không phải nhập SMTP mỗi lần mở terminal, copy
`application-mail-local.properties.example` thành `application-mail-local.properties`
ngay trong thư mục `flowershop`. Điền Gmail gửi đi và Google App Password thật vào
file local trên máy của bạn. File này đã được git-ignore và backend tự đọc khi khởi động
từ thư mục `flowershop`. Không gửi file hoặc mật khẩu vào chat.
Nếu không có credential SMTP hợp lệ, thay đổi code không thể tự gửi email qua Gmail.

Luồng mới thay thế hoàn toàn xác thực bằng link. Không cần mở link, không cần
App Password của khách hàng. Chỉ tài khoản Gmail gửi thư của hệ thống cần App Password.

## Các file chính và cách hoạt động

Đường dẫn Java tính từ `src/main/java/com/example/flowershop/`:

| File | Vai trò |
|---|---|
| `service/AuthService.java` | Lưu đăng ký tạm, gửi/kiểm tra OTP; chỉ tạo User sau OTP đúng |
| `entity/PendingRegistration.java` | Email, họ tên, hash mật khẩu và hash OTP đăng ký, hạn và bộ đếm |
| `repository/PendingRegistrationRepository.java` | Khóa đăng ký tạm theo email khi gửi lại hoặc xác thực |
| `entity/OtpChallenge.java` | OTP quên mật khẩu của tài khoản đã tồn tại |
| `repository/OtpChallengeRepository.java` | Khóa bản ghi trong transaction để chống dùng mã đồng thời |
| `service/OtpService.java` | Sinh mã 6 chữ số bằng SecureRandom, BCrypt hash gắn user/mục đích, kiểm tra/tiêu thụ mã |
| `service/AuthMailEvent.java` | Sự kiện chứa OTP tạm trong bộ nhớ, không ghi log |
| `service/VerificationMailSender.java` | Gửi OTP, thư chào mừng hoặc thông báo đổi mật khẩu sau commit |
| `dto/auth/VerifyEmailRequest.java` | email + otp |
| `dto/auth/ResetPasswordRequest.java` | email + otp + newPassword + confirmPassword |
| `controller/AuthController.java` | Các endpoint và ô nhập CSRF trong Swagger |

**Chưa xác thực OTP thì không có bản ghi mới trong Users.** Thông tin nằm ở
Pending_Registrations; mật khẩu và OTP chỉ được lưu dưới dạng hash. Sau OTP đúng,
transaction tạo User ACTIVE, isEmailVerified=true và xóa đăng ký tạm.
Chưa có API login trong project. Khi triển khai login,
bắt buộc kiểm tra cả status và isEmailVerified trước khi tạo session.

## Database

Migration bổ sung: `sql/003_pending_registrations.sql` (đã áp dụng trên máy local).
`sql/002_otp_challenges.sql` vẫn cần cho quên mật khẩu.
Không chạy lại `database.sql` vì script khởi tạo đó xóa database ở đầu.
Bảng link token cũ được giữ để tránh xóa dữ liệu, nhưng code mới không đọc/chấp nhận nó.
Tài khoản có sẵn trong Users, kể cả tài khoản chưa xác thực từ phiên bản cũ, được giữ
nguyên và không bị chuyển/xóa tự động. Để test luồng mới, dùng email chưa có trong Users.

## Gmail gửi thư

Bật xác minh hai bước rồi tạo App Password ở https://myaccount.google.com/apppasswords.
Nhập riêng trên máy, không đưa mật khẩu vào file, ảnh hoặc chat.

Trong PowerShell tại thư mục `flowershop`, dừng backend cũ rồi chạy:

```powershell
$env:MAIL_USERNAME = Read-Host 'Gmail gui thu'
$gmailSecret = Read-Host 'Google App Password' -AsSecureString
$env:MAIL_PASSWORD = ([System.Net.NetworkCredential]::new('', $gmailSecret).Password) -replace '\s', ''
$env:MAIL_FROM = $env:MAIL_USERNAME
$env:MAIL_HOST = 'smtp.gmail.com'
$env:MAIL_PORT = '587'
.\gradlew.bat bootRun --no-configuration-cache
```

Read-Host hiển thị câu hỏi rồi CHỜ bạn nhập giá trị. Không đặt mật khẩu trong câu hỏi.
Có thể dùng `run-gmail.ps1` nếu máy cho chạy script. Đóng terminal sau khi dừng backend
để bỏ các biến môi trường mật khẩu. SMTP dùng STARTTLS và kiểm tra chứng chỉ.

## Test trên Swagger

Mở http://localhost:8080/swagger-ui/index.html. Gọi GET `/api/auth/csrf`, copy token
vào header `X-CSRF-TOKEN` của mỗi POST. CSRF token KHÁC mã OTP trong email.

### 1. Đăng ký

POST `/api/auth/register`:

```json
{"email":"email-ban-so-huu@gmail.com","password":"FlowerShop!2026-Test","fullName":"Nguyễn Văn An"}
```

202 = tiếp nhận, chưa đăng ký thành công. Mở email để lấy OTP gồm 6 chữ số.
Không dùng địa chỉ `example.com` để test nhận thư thật.
Nếu email có đăng ký tạm, dùng bước gửi lại. Nếu email đã có trong Users thì không
tạo mới hoặc ghi đè. Đăng ký lại sau cooldown cập nhật thông tin tạm và phát mã mới;
mã cũ không thể xác thực thông tin vừa thay đổi.

### 2. Xác thực đăng ký

POST `/api/auth/verify-email`:

```json
{"email":"email-ban-so-huu@gmail.com","otp":"123456"}
```

Thay 123456 bằng mã trong email. Nhập đúng → 200 và thông báo đăng ký thành công;
database tạo User với `is_email_verified=1`, xóa đăng ký tạm, gửi thư chào mừng sau commit.
Nhập sai/hết hạn/đã sử dụng → 400. Giữ OTP là chuỗi để không mất số 0 ở đầu.

### 3. Gửi lại OTP đăng ký

POST `/api/auth/resend-verification`:

```json
{"email":"email-ban-so-huu@gmail.com"}
```

202 với thông báo chung. Mã mới thay mã cũ; không xóa số lần đoán sai.

### 4. Quên mật khẩu

POST `/api/auth/forgot-password`, body như bước 3. Chỉ tài khoản ACTIVE, đã xác thực
và có mật khẩu mới nhận mã đặt lại. Email không tồn tại cũng trả cùng thông báo 202.
Gọi lại endpoint này để yêu cầu gửi lại mã đặt lại mật khẩu.

### 5. Đặt lại mật khẩu

POST `/api/auth/reset-password`:

```json
{
  "email":"email-ban-so-huu@gmail.com",
  "otp":"123456",
  "newPassword":"AnotherFlower!2026",
  "confirmPassword":"AnotherFlower!2026"
}
```

Mã phải là mã QUÊN MẬT KHẨU. OTP đăng ký không cấp quyền đặt lại mật khẩu.
200 → mật khẩu được băm lại, OTP đã dùng, gửi thông báo thay đổi mật khẩu.
Không tự đăng nhập sau reset.

## Giới hạn bảo vệ

- OTP sống 5 phút và dùng một lần.
- Mỗi user/mục đích: tối đa 5 lần sai, 3 lần gửi trong cửa sổ 15 phút.
- Hai lần gửi cách nhau ít nhất 60 giây; gửi lại không reset bộ đếm đoán sai.
- Sau khi vượt giới hạn: chờ hết cửa sổ 15 phút, yêu cầu mã mới.
- Đăng ký khóa bản ghi tạm; quên mật khẩu khóa user rồi challenge. Lần nhập sai được COMMIT bằng
  `noRollbackFor=InvalidOtpException.class`; nếu rollback thì bộ đếm sẽ không tăng.
- Account không bị khóa chỉ vì ai đó gửi yêu cầu quên mật khẩu.
- Đăng ký lưu BCrypt hash OTP trong bản ghi tạm; reset lưu hash user+purpose+OTP.
  Hai luồng dùng bảng và truy vấn riêng, không chấp nhận mã của nhau.
- CSRF vẫn bật cho tất cả POST, role do backend quyết định.

## Kiểm tra database

```sql
SELECT email, role, status, is_email_verified FROM Users WHERE email='email-ban-so-huu@gmail.com';
SELECT email, issued_at, expires_at, failed_attempts, send_count FROM Pending_Registrations;
SELECT user_id, purpose, issued_at, expires_at, used_at, failed_attempts, send_count FROM Otp_Challenges;
```

## Phạm vi kiểm thử và phần còn lại

Test chạy trên H2 riêng với SMTP giả lập, không gửi email tới người thật:

```powershell
.\gradlew.bat test --tests com.example.flowershop.EmailVerificationTests
```

Gmail thực tế cần cấu hình App Password trên máy của bạn; 202 không khẳng định thư đã giao.
Nếu SMTP lỗi, chỉ có đăng ký tạm, chưa tạo user; sửa SMTP rồi yêu cầu gửi lại sau cooldown.
Log chỉ ghi loại thư và user ID, không ghi OTP/mật khẩu.

Đã có giao diện React đăng ký/nhập OTP; chưa có API login. Khi thêm login cần chặn
tài khoản chưa xác thực và thu hồi các session đang có sau reset password.
Trước production còn cần giới hạn request theo IP/toàn hệ thống và hàng đợi mail
bền vững để retry (hiện gửi sau commit, thư thất bại không tự retry).
Response chung chưa bảo đảm thời gian phản hồi giống nhau giữa email có/không tồn tại.

Tham khảo thiết kế OTP: https://cheatsheetseries.owasp.org/cheatsheets/Forgot_Password_Cheat_Sheet.html
