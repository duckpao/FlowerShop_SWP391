# DESIGN.md

Hướng thiết kế cho FlowerShop (client React tại `flower-shop-client/`). Phần "Hiện trạng" được rút từ code đang chạy (`src/index.css`, `components/ui/*`), không phải phong cách mới. Phần "Cần bạn xác nhận" là chỗ chỉ chủ dự án quyết được; cho đến khi bạn điền, giao diện mới bám theo hiện trạng.

## Dial

Dial: ENERGY 1 / RHYTHM 1 / MOTION 1

Lý do: đây là ứng dụng làm việc (quản trị, đơn hàng, chat), không phải trang giới thiệu. Người dùng cần đọc nhanh và thao tác đúng, nên màn hình yên, bố cục đều, chuyển động chỉ ở trạng thái hover và các thay đổi trạng thái. Trang cửa hàng công khai có thể nâng ENERGY lên 2 nếu bạn muốn, hiện chưa đổi.

## Hiện trạng (từ code)

### Màu
- **Màu thương hiệu (brand)**: dải hồng đỏ, `brand-500 #f43f5e` làm màu nhấn, `brand-600 #e11d48` cho nút chính và liên kết, `brand-50 #fff1f2` cho nền nhẹ.
- **Trung tính**: dải `gray` (nền trang `gray-50 #f9fafb`, chữ chính `gray-900 #101828`, chữ phụ `gray-500 #667085`, viền `gray-200 #e4e7ec`).
- **Trạng thái**: `success` (xanh lá), `error` (đỏ), `warning` (vàng cam). Dùng cho nhãn trạng thái đơn, thông báo thành công và lỗi.
- Quy tắc dùng: màu nhấn chỉ ở hành động chính của màn hình. Trạng thái dùng màu trạng thái, không dùng màu thương hiệu.
- Chế độ tối có sẵn (`ThemeContext`, class `dark`). Mọi màn mới phải chạy đủ cả sáng và tối.

### Chữ
- Phông duy nhất: **Outfit** (Google Fonts), khai báo là `font-outfit`.
- Cỡ chữ thường dùng: `text-theme-sm` 14/20 cho nội dung và bảng, `text-theme-xs` 12/18 cho chú thích, `text-theme-xl` 20/30 cho tiêu đề thẻ.
- Tiêu đề thẻ dùng `font-medium` hoặc `font-semibold`, không in hoa toàn bộ, không giãn chữ rộng.

### Hình khối
- Bo góc: nút và ô nhập `rounded-lg`, thẻ `rounded-xl` hoặc `rounded-2xl`, hộp thoại `rounded-3xl`, chỉ biểu tượng tròn và avatar dùng `rounded-full`. Không bo tròn mọi thứ.
- Viền mảnh `gray-200` (tối: `white/5` hoặc `gray-800`) thay cho đổ bóng. Bóng chỉ cho lớp nổi (menu thả xuống, hộp thoại).
- Nền thẻ `white` (tối: `white/3`).

### Thành phần dùng lại
`ComponentCard`, `Button` (primary hoặc outline), `Badge` (light, theo màu trạng thái), `Table`, `Modal`, `PageBreadCrumb`, `PageMeta`, `ConfirmProvider` (`useConfirm`). Màn mới ưu tiên dùng lại các thành phần này trước khi viết kiểu mới.

### Điểm ngắt
`sm 640`, `md 768`, `lg 1024`, `xl 1280`, `2xl 1536`. Màn hai cột hiện chia ở `xl`; dưới đó xếp dọc.

### Ngôn ngữ
Toàn bộ chữ trên giao diện bằng tiếng Việt, giọng ngắn gọn, nói thẳng việc cần làm ("Lưu thay đổi", "Xóa danh mục"), không dùng lời quảng cáo.

## Đề xuất cho màn chat (đang chờ bạn xác nhận)

Đây là gợi ý để làm tiếp, bám theo hiện trạng ở trên. Không có gì ở đây đã được xem là chốt.
- Tin của mình: nền `brand-600`, chữ trắng. Tin của người kia: nền `gray-100` (tối: `white/5`), chữ `gray-900`. Cần kiểm tương phản chữ trắng trên `brand-600` đạt 4.5:1 trước khi dùng.
- Bố cục: danh sách hội thoại bên trái, luồng tin ở giữa, chia hai cột từ `lg`; dưới `lg` hiện từng khung một có nút quay lại.
- Thẻ ngữ cảnh (đơn custom hoặc sản phẩm): viền `gray-200`, `rounded-xl`, nền `white`, có ảnh và tên; bấm được và hiện dấu hiệu focus bằng bàn phím.
- Số tin chưa đọc: một `Badge` đếm duy nhất, màu `brand`, chỉ hiện khi lớn hơn 0.
- Chuyển động: tin mới xuất hiện không cần hiệu ứng; cuộn xuống cuối khi đang ở cuối luồng. Không có hiệu ứng lặp.
- Đủ ba trạng thái: danh sách rỗng, đang tải, lỗi kết nối (nói rõ đang mất kết nối và đang thử nối lại).

## Cần bạn xác nhận

Điền hoặc sửa các mục dưới đây. Nếu để trống, mình giữ đúng hiện trạng.
- **Tính cách thương hiệu**: ______ (hiện chưa có mô tả nào trong code)
- **Biểu tượng và logo**: giữ bộ icon đang dùng trong `src/icons`? Logo hiện chỉ là chữ "FlowerShop"; chưa có logo riêng.
- **Màu nhấn cho chat**: dùng `brand-600` như trên, hay màu khác?
- **Trang cửa hàng công khai**: giữ ENERGY 1 hay muốn sống động hơn?
- **Ảnh minh họa và ảnh sản phẩm**: chỉ dùng ảnh thật của shop, không dùng ảnh minh họa có sẵn?
