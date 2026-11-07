# Tài khoản nhận tiền SePay

Tài khoản nhận tiền theo yêu cầu:

- Ngân hàng: MB Bank.
- Số tài khoản: `828112004`.
- Chủ tài khoản: Phạm Thanh Dũng.

Đã kiểm tra trên SePay: tài khoản thụ hưởng của Gateway Production là
`828112004 - PHAM THANH DUNG`, ngân hàng MBBank. Merchant ID: `SP-LIVE-PT779375`.
File local đã được tạo với merchant và endpoint Production tương ứng.
Secret Key Production đã được cấu hình trong file local được Git bỏ qua.
Khởi động lại backend để nạp cấu hình. Chưa thực hiện giao dịch thanh toán thật
để kiểm tra toàn bộ luồng.

## Kết nối với ứng dụng

Ứng dụng dùng SePay Payment Gateway cho thanh toán đơn hàng và gói Pro.
Ngân hàng nhận tiền do cấu hình merchant trên SePay quyết định. Thông tin trong
tài liệu này không tự thay đổi tài khoản nhận tiền của merchant đang dùng.

1. Đăng nhập https://my.sepay.vn và liên kết tài khoản MB Bank nêu trên.
2. Cấu hình phương thức QR chuyển khoản tại https://my.sepay.vn/pg/payment-methods
   và kích hoạt Production cho tài khoản nhận tiền này.
3. Lấy Merchant ID và Secret Key tương ứng; điền vào file local
   `flowershop/application-sepay-local.properties` (file đã được Git bỏ qua):

```properties
sepay.merchant-id=SP-LIVE-PT779375
sepay.secret-key=SECRET_KEY_TU_SEPAY
sepay.checkout-url=https://pay.sepay.vn/v1/checkout/init
sepay.api-base-url=https://pgapi.sepay.vn
```

Không commit file chứa Secret Key. Khởi động lại backend sau khi cập nhật.
Kiểm tra tài khoản nhận tiền trên màn hình thanh toán của SePay trước khi chuyển tiền.

Hướng dẫn chính thức: https://developer.sepay.vn/vi/cong-thanh-toan/bat-dau
