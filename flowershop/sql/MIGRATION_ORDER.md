# Migration sau khi gộp master và feature/dung

Không chạy theo số prefix đơn thuần: các nhánh đã phát hành nhiều file trùng số. Giữ nguyên tên file lịch sử để đối chiếu database đã cập nhật; mỗi file dưới đây là một migration riêng, không chọn một file thay cho file còn lại chỉ vì trùng số.

## Database local đã có dữ liệu

Đã sao lưu ở `C:/Fall2026/SWP391/tmp/flower_shop_before_merge_20261106.sql`, rồi chạy:

1. `027_sync_schema.sql`: bổ sung an toàn các cột/index của blog, checkout nhóm, payment nhóm, notification, nhắc dịp và chat. Bao phủ các thay đổi schema của `022_payment_group_invoice.sql`, `023_order_checkout_group.sql`, `024_notifications.sql`, `025_occasion_reminders.sql`, `026_chat.sql`, `026_floral_care_blogs.sql` đã có trên master. Không chạy lại các ALTER một lần của chúng.
2. `029_merge_custom_order_schema.sql`: bổ sung có kiểm tra tồn tại cho `Orders.custom_request_id` và index (thay cho việc chạy lại `028_order_custom_request.sql`), giữ `payment_started_at` và hợp nhất toàn bộ trạng thái đơn thường/đặt cọc/báo giá; địa chỉ được nullable cho yêu cầu cũ cần khách bổ sung.

Các migration của feature/dung đã được áp dụng trước khi merge, không cần chạy lại:

| File | Vai trò | Cách đối chiếu |
| --- | --- | --- |
| `022_custom_order_workflow.sql` | Chuyển yêu cầu cũ sang Orders, luồng báo giá | Orders có AWAITING_QUOTE/QUOTED; không chạy lại chỉ vì thấy số 022 của master |
| `023_custom_flower_options.sql` | Danh mục loại hoa/dịp/giấy gói/phụ kiện | Bảng Custom_Flower_Options có 33 dòng |
| `024_voucher_management.sql` | Loại voucher, xóa mềm, giảm ship và snapshot trên Orders | Coupons.target/deleted và Orders.shipping_coupon_id/shipping_discount_amount |
| `025_seed_shop_vouchers.sql` | Bổ sung mỗi shop lên 3 mã | Bốn shop local đều có 3 mã; chạy lại không reset lượt dùng |

`027` và `029` chạy lại được. Không import lại toàn bộ `database.sql` lên database đang dùng.

## Database tạo mới

Dùng `database.sql` đã gộp: gồm schema master, danh mục đặt riêng, voucher, dữ liệu mẫu, trạng thái báo giá và thời điểm bắt đầu thanh toán. Không cần chạy lại chuỗi ALTER lịch sử sau khi import file này. Chỉ dùng `022_custom_order_workflow.sql` nếu chủ động muốn chuyển các yêu cầu cũ chưa chuyển; không cần cho yêu cầu mới tạo trực tiếp ở Orders.

## Nhóm số trùng cũ

Các cặp 016 (giao hàng/catalog), 017 (font/danh mục hoa), 018 (nhóm danh mục/loại sản phẩm), 019 (địa chỉ/khôi phục catalog), 020 (địa chỉ cá nhân/theo dõi shop), 021 (avatar/shop mẫu), 026 (chat/blog) là các thay đổi khác nhau. Không chạy lại hàng loạt trên database đã triển khai. Dùng schema thực tế và tên đầy đủ để chọn; `027_sync_schema.sql` là bước đối chiếu bổ sung cho các phần mới từ master.
