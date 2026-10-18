# Hướng dẫn thiết kế màn hình bằng Stitch (bản đơn giản, đơn sắc)

Phạm vi: 8 màn quản lý đơn hàng và dashboard. Nguyên tắc cho cả tài liệu:

- **Đơn sắc:** chỉ đen, trắng và các mức xám. Không màu thương hiệu, không màu trạng thái.
- **Chỉ dữ liệu cần để ra quyết định.** Mỗi màn có một việc chính; thứ gì không phục vụ việc đó thì bỏ.
- **Không số liệu bịa.** Thiết kế mẫu dùng nhãn `[dữ liệu mẫu]` hoặc để trống. Không biểu đồ, không thẻ thống kê trang trí.
- **Không emoji, không gradient, không đổ bóng, không minh họa.**

## 0. Cách dùng

1. Dán **Prompt nền** (mục 1) vào Stitch trước, rồi dán prompt từng màn trong cùng một dự án.
2. Thiếu gì thì nhắc từng ý ("remove the chart", "make the primary button solid black"), đừng viết lại cả prompt.
3. Đối chiếu kết quả với bảng **Dữ liệu và chức năng** của màn đó. Thứ gì Stitch thêm mà bảng không có thì xóa.

## 1. Prompt nền

```
Design a desktop web app for "FLOWER MARKETPLACE", a Vietnamese multi-shop flower delivery platform.
Strictly monochrome: white background, black text, light-gray 1px borders, mid-gray secondary text.
No brand color, no status colors, no gradients, no shadows, no emoji, no illustrations, no icons unless
a label would be unclear. Corner radius 4px everywhere. One neutral sans-serif font, two sizes of body
text and one heading size. Generous whitespace, left-aligned content, a single column where possible.
Buttons: primary = solid black with white text; secondary = white with black 1px border;
destructive = white with black 2px border and the label in bold (never red).
Status is always a text label: needs-action = solid black badge with white text, in-progress = black outline badge,
finished or cancelled = plain gray text. Never rely on color alone.
All UI text in Vietnamese. Money is VND with dots ("350.000 đ"). Show only the data listed in each prompt.
Every list has an empty state that says why it is empty and what to do, a one-line loading state,
and an error state with a "Thử lại" button. Number the key areas with small circled numbers (1), (2), (3)
like a specification wireframe.
```

## 2. Quy ước dùng chung

**Trạng thái đơn hàng**

| Nhãn | Mã | Kiểu badge |
|---|---|---|
| Chờ xác nhận | PENDING | đặc (đen) |
| Chờ đặt cọc | AWAITING_DEPOSIT | viền |
| Đã cọc | DEPOSIT_PAID | viền |
| Chờ giao hàng | PROCESSING | đặc (đen) |
| Đang giao | DELIVERING | viền |
| Giao thành công | COMPLETED | chữ xám |
| Đã hủy | CANCELLED | chữ xám |
| Hoàn hàng | REFUNDED | chữ xám |

"Đặc" nghĩa là shop cần làm gì đó với đơn này.

**Chặng vận đơn GHN (chỉ đọc):** Lấy hàng, Luân chuyển, Giao hàng, Hoàn hàng. Trạng thái chi tiết hiển thị bằng nhãn tiếng Việt, ví dụ `delivering` thành "Đang giao hàng", `delivery_fail` thành "Giao thất bại", `cancel` thành "Đã hủy vận đơn".

**Chỉ 3 thao tác shop gửi sang GHN**

| Nút | Hiện khi trạng thái GHN là |
|---|---|
| Hủy vận đơn | ready_to_pick, picking |
| Yêu cầu giao lại | storing, waiting_to_return |
| Yêu cầu trả hàng | delivery_fail, storing, waiting_to_return, return |

Các chặng lấy hàng, vận chuyển, giao thành công do GHN cập nhật. Shop chỉ có nút "Đồng bộ từ GHN". Không có nút "giả lập giao thành công".

**Hộp xác nhận:** tiêu đề, một câu nói rõ hậu quả, hai nút "Quay lại" (phụ) và nút hành động. Đóng bằng Esc.

---

## 3. Màn 1: Quản lý đơn hàng (shop)

**Actors:** Shop. **Việc chính:** tìm đơn cần xử lý và mở nó.

**Prompt**

```
Screen "Quản lý đơn hàng". Title on the left with the total order count in gray. Below it a row of plain
text tabs with a small count after each label: "Tất cả", "Chờ xác nhận", "Chờ giao hàng", "Đang giao",
"Giao thành công", "Đã hủy", "Hoàn hàng"; the active tab has a black underline.
A simple table with only these columns: "Mã đơn" (8 characters, monospace), "Khách hàng" (name, phone in gray below),
"Tổng tiền", "Trạng thái" (text badge), and an actions column. Row actions: secondary "Chi tiết" on every row;
solid "Xác nhận đơn hàng" on Chờ xác nhận rows; solid "Tạo vận đơn GHN" on Chờ giao hàng rows.
Pagination at the bottom: "Trang trước", "Trang 1 / 3", "Trang sau".
Empty state: "Chưa có đơn nào ở mục này." Numbered circles (1) to (6).
```

**Dữ liệu và chức năng**

| # | Dữ liệu | Chức năng |
|---|---|---|
| 1 | Tab trạng thái + số đơn | Lọc danh sách, đổi tab về trang 1 |
| 2 | Mã đơn | Nhận diện đơn, chỉ đọc |
| 3 | Khách hàng (tên, SĐT) | Biết ai nhận hàng |
| 4 | Tổng tiền | Biết giá trị đơn |
| 5 | Trạng thái | Biết đơn cần làm gì |
| 6 | Nút hành động theo trạng thái | Chi tiết, Xác nhận, Tạo vận đơn |

Phân trang: 20 đơn mỗi trang, giữ nguyên tab khi chuyển trang.

---

## 4. Màn 2: Đơn đang chờ xử lý (Order Queue)

**Actors:** Shop. **Việc chính:** xác nhận hoặc hủy từng đơn, đơn cũ nhất trước.

**Prompt**

```
Screen "Đơn chờ xử lý". Only a list, no summary tiles. Each order is one row separated by a gray line:
order code, customer name and phone, first product name followed by "và 2 sản phẩm khác" when there are more,
total in VND, and "Đã chờ 3 giờ" in gray. On the right of each row: solid black "Xác nhận đơn hàng"
and a bold-outline "Hủy đơn". Rows are sorted oldest first.
Clicking "Hủy đơn" opens a small modal titled "Hủy đơn hàng 3f9a1c2b" with one textarea
"Lý do hủy (bắt buộc)", the sentence "Không thể hoàn tác.", and buttons "Quay lại" and "Xác nhận hủy"
(disabled until the textarea has text). Empty state: "Không có đơn nào cần xử lý." Numbered circles (1) to (6).
```

**Dữ liệu và chức năng**

| # | Dữ liệu | Chức năng |
|---|---|---|
| 1 | Mã đơn, khách, sản phẩm đầu tiên, tổng tiền | Đủ để quyết định xác nhận hay hủy |
| 2 | Thời gian đã chờ | Ưu tiên đơn chờ lâu |
| 3 | Xác nhận đơn hàng | Đổi đơn sang Chờ giao hàng |
| 4 | Hủy đơn | Mở hộp nhập lý do |
| 5 | Lý do hủy (bắt buộc) | Lưu vào đơn, khóa nút khi trống |
| 6 | Phân trang | 20 đơn mỗi trang |

---

## 5. Màn 3: Chi tiết đơn (chủ shop)

**Actors:** Shop. **Việc chính:** biết đơn gửi đi đâu, ở chặng nào, và làm bước tiếp theo.

**Prompt**

```
Screen "Chi tiết đơn 3f9a1c2b" as a modal 640px wide. Top: status text badge and order date.
Then three plain sections separated by gray lines, no cards:
1. "Gửi từ" (shop name, full address) and "Giao đến" (recipient name, phone, full address), side by side.
2. "Vận đơn GHN": tracking code, current GHN status as text, a four-step horizontal strip
"Lấy hàng, Luân chuyển, Giao hàng, Hoàn hàng" with the current step in bold and finished steps in gray,
a secondary button "Đồng bộ từ GHN", and only the allowed action buttons
("Hủy vận đơn", "Yêu cầu giao lại", "Yêu cầu trả hàng") in bold outline. If no action is allowed,
one gray sentence: "Ở trạng thái này GHN không nhận thao tác từ shop."
3. "Sản phẩm": name x quantity and price per line, then "Tổng cộng" in bold.
Footer: "Đóng" and one primary button that depends on the order status.
Also "Thanh toán: Đã thanh toán / Chưa thanh toán" as a single line. Numbered circles (1) to (8).
```

**Dữ liệu và chức năng**

| # | Dữ liệu | Chức năng |
|---|---|---|
| 1 | Trạng thái đơn, ngày đặt | Biết đơn đang ở đâu |
| 2 | Địa chỉ gửi và địa chỉ nhận (đủ phường/xã, quận/huyện, tỉnh/thành) | Kiểm tra trước khi tạo vận đơn |
| 3 | Mã vận đơn, trạng thái GHN | Theo dõi, "Chưa có" khi chưa tạo |
| 4 | Dải 4 chặng | Thấy vận đơn đi tới đâu |
| 5 | Đồng bộ từ GHN | Đọc trạng thái mới nhất |
| 6 | Nút thao tác GHN (theo bảng mục 2) | Gửi hủy, giao lại, trả hàng; lỗi GHN hiện nguyên văn dưới nút |
| 7 | Sản phẩm, tổng cộng, trạng thái thanh toán | Đối chiếu số tiền |
| 8 | Nút chân trang | Chờ xác nhận: "Xác nhận đơn hàng". Chờ giao hàng: "Tạo vận đơn GHN & Giao". Đang giao: "Sửa vận đơn GHN". Còn lại: chỉ "Đóng" |

---

## 6. Màn 4: Chi tiết đơn (khách hàng)

**Actors:** Customer. **Việc chính:** biết đơn của tôi đang ở đâu.

**Prompt**

```
Screen "Đơn #3f9a1c2b" for a customer, one centered column 640px wide, no cards.
Top: shop name, order date, status text badge.
Then one large sentence in plain words describing where the order is, e.g. "Đơn vị vận chuyển đang giao hàng đến bạn."
with a secondary button "Cập nhật trạng thái" on its right.
Under it a vertical list of three steps, each with a text label and a state word on the right:
"Cửa hàng đang chuẩn bị đơn hàng", "Cửa hàng đã giao cho đơn vị vận chuyển",
"Đơn vị vận chuyển đã giao đến bạn"; state words are "Hoàn tất", "Đang diễn ra", "Chưa tới";
the current step is bold, finished steps are gray, no icons.
Then a plain table "Sản phẩm, Đơn giá, Số lượng, Thành tiền" and three summary lines:
"Tạm tính", "Phí ship", "Tổng cộng" (bold). One gray line: "Mã vận đơn: … · GHN".
Link at the bottom: "Danh sách đơn hàng". Show also two variants: a cancelled order and a returned order
("Đơn vị vận chuyển đã hoàn đơn hàng về cửa hàng."). Numbered circles (1) to (6).
```

**Dữ liệu và chức năng**

| # | Dữ liệu | Chức năng |
|---|---|---|
| 1 | Trạng thái, cửa hàng, ngày đặt | Nhận diện đơn |
| 2 | Câu trạng thái giao hàng | Trả lời "đơn đang ở đâu" bằng một câu |
| 3 | Cập nhật trạng thái | Gọi GHN đọc trạng thái mới; ẩn khi chưa có vận đơn hoặc đơn đã hủy |
| 4 | 3 bước theo dõi, mỗi bước có nhãn trạng thái bằng chữ | Thấy tiến độ |
| 5 | Sản phẩm, tạm tính, phí ship, tổng cộng | Đối chiếu số tiền đã chốt |
| 6 | Mã vận đơn | Tra cứu với GHN |

**Danh sách đơn của khách (đi kèm):** cùng 7 tab trạng thái, cột "Mã đơn, Cửa hàng, Tổng tiền, Trạng thái, Ngày đặt, Xem", 20 đơn mỗi trang, trạng thái rỗng "Bạn chưa có đơn nào." hoặc "Không có đơn nào ở mục này."

---

## 7. Màn 5: Cập nhật trạng thái đơn

Không phải trang riêng. Đây là bộ hộp xác nhận và quy tắc chuyển trạng thái của các màn 1, 2, 3.

**Prompt (hộp xác nhận)**

```
Five small confirmation modals, each 420px wide, white, 1px gray border, same layout:
a bold title, one sentence stating the consequence, and right-aligned buttons.
Left button "Quay lại" (secondary). Right button is solid black except for the two destructive ones,
which use a bold 2px outline.
1. "Xác nhận đơn hàng": "Đơn chuyển sang Chờ giao hàng." Button "Xác nhận".
2. "Tạo vận đơn GHN": "Tạo mã vận đơn và chuyển đơn sang Đang giao." Button "Tạo vận đơn".
3. "Hủy vận đơn": "Gửi yêu cầu hủy sang GHN." Destructive button "Gửi sang GHN".
4. "Yêu cầu giao lại": "Gửi yêu cầu giao lại sang GHN." Button "Gửi sang GHN".
5. "Yêu cầu trả hàng": "Gửi yêu cầu trả hàng sang GHN." Destructive button "Gửi sang GHN".
While sending, the action button is disabled and reads "Đang gửi…". If GHN refuses, one line of gray text
under the sentence shows GHN's reason and the modal stays open.
```

**Sơ đồ chuyển trạng thái**

Mỗi mũi tên ghi **ai kích hoạt** và **điều kiện**. Đơn Giao thành công, Hoàn hàng, Đã hủy là trạng thái cuối.

```
                       shop xác nhận                 shop tạo vận đơn GHN
  Chờ xác nhận  ───────────────────────►  Chờ giao hàng  ───────────────────────►  Đang giao
       │                                       │                                      │  │  │
       │ shop hủy (bắt buộc lý do)             │ shop hủy (bắt buộc lý do)            │  │  │ GHN báo delivered
       ▼                                       ▼                                      │  │  ▼
     Đã hủy ◄──────────────────────────────────┘                                      │  │  Giao thành công
        ▲                                                                              │  │   (thanh toán → thành công)
        │ GHN báo cancel ("Vận đơn bị hủy trên GHN.")─────────────────────────────────┘  │
                                                                                          │ GHN báo returned
                                                                                          ▼
                                                                                       Hoàn hàng
```

**Prompt (sơ đồ cho Stitch)**

```
A monochrome state diagram, left to right. Boxes with 1px black borders and white fill:
"Chờ xác nhận", "Chờ giao hàng", "Đang giao", and three terminal boxes "Giao thành công",
"Hoàn hàng", "Đã hủy" drawn with a double border. Every arrow is labeled with who triggers it:
"shop xác nhận" (Chờ xác nhận to Chờ giao hàng), "shop tạo vận đơn GHN" (to Đang giao),
"GHN báo delivered" (to Giao thành công), "GHN báo returned" (to Hoàn hàng),
"GHN báo cancel" (Đang giao to Đã hủy), "shop hủy, cần lý do" (from the first two boxes to Đã hủy).
Shop-triggered arrows are solid lines, GHN-triggered arrows are dashed lines. No colors.
```

| Từ | Sang | Ai kích hoạt | Điều kiện |
|---|---|---|---|
| Chờ xác nhận | Chờ giao hàng | Shop | Không |
| Chờ xác nhận, Chờ đặt cọc, Chờ giao hàng | Đã hủy | Shop | Bắt buộc có lý do |
| Chờ giao hàng | Đang giao | Shop | Shop có địa chỉ lấy hàng, người nhận và chủ shop có số điện thoại |
| Đang giao | Giao thành công | GHN (`delivered`) | Thanh toán chuyển sang thành công |
| Đang giao | Hoàn hàng | GHN (`returned`) | Không |
| Đang giao | Đã hủy | GHN (`cancel`) | Lý do ghi "Vận đơn bị hủy trên GHN." |

---

## 8. Màn 6: Dashboard Admin

**Việc chính:** "Có gì đang chờ tôi duyệt?" Không biểu đồ, không thẻ thống kê.

**Prompt**

```
Screen "Tổng quan quản trị". Left sidebar with five plain text links: "Tổng quan", "Cửa hàng", "Khách hàng",
"Danh mục", "Đơn mở shop". Main area has one section "Cần xử lý": a list of rows, each row a sentence
with a number and a button on the right, e.g. "Đơn mở shop chờ duyệt: 2" with a secondary button "Xem",
and "Cửa hàng chờ kích hoạt: 1" with "Xem". Rows with zero count are not shown; when nothing is left,
the section reads "Không có việc nào đang chờ." Below it a second section "Cửa hàng mới nhất":
a table with "Tên cửa hàng", "Chủ shop", "Trạng thái" (text badge), "Ngày tạo", 5 rows.
Mark any sample value with "[dữ liệu mẫu]". Loading, empty and error state per section. Numbered circles (1) to (5).
```

| # | Dữ liệu | Chức năng |
|---|---|---|
| 1 | Menu bên trái | Chuyển giữa các khu quản trị |
| 2 | Việc cần xử lý + số lượng | Dẫn thẳng tới màn duyệt |
| 3 | Nút "Xem" | Mở danh sách đã lọc theo việc đó |
| 4 | 5 cửa hàng mới nhất (tên, chủ shop, trạng thái, ngày tạo) | Kiểm tra cửa hàng mới, bấm để mở chi tiết |
| 5 | Trạng thái rỗng, lỗi riêng từng khối | Một khối lỗi không làm hỏng cả trang |

---

## 9. Màn 7: Dashboard Shop

**Việc chính:** "Hôm nay tôi cần làm gì với đơn hàng?"

**Prompt**

```
Screen "Tổng quan cửa hàng". First block, the largest text on the page: one sentence
"3 đơn đang chờ xác nhận" with, in gray below it, "Đơn chờ lâu nhất: 5 giờ" and a solid black button
"Xử lý ngay". When the count is 0 the sentence is "Không có đơn nào cần xác nhận." and the button is hidden.
Second block "Đơn theo trạng thái": six plain text cells in a row, each a label with a number below it
("Chờ xác nhận", "Chờ giao hàng", "Đang giao", "Giao thành công", "Hoàn hàng", "Đã hủy"); each cell is a link to the
order list with that tab selected. Third block "Đơn gần đây": a table with "Mã đơn", "Khách hàng", "Tổng tiền",
"Trạng thái", "Thời gian", 5 rows. No charts. Mark sample values "[dữ liệu mẫu]".
Loading, empty and error state per block. Numbered circles (1) to (6).
```

| # | Dữ liệu | Chức năng |
|---|---|---|
| 1 | Số đơn chờ xác nhận, thời gian chờ lâu nhất | Việc ưu tiên số một |
| 2 | "Xử lý ngay" | Mở Order Queue (màn 2) |
| 3 | Số đơn theo 6 trạng thái | Liên kết sang màn 1 đúng tab |
| 4 | 5 đơn gần đây | Mở nhanh chi tiết đơn |
| 5 | Trạng thái rỗng khi chưa có đơn | Giải thích và hướng dẫn việc đầu tiên |
| 6 | Lỗi riêng từng khối | Không che hết trang |

Doanh thu và tồn kho có thể thêm sau khi hệ thống có API trả đúng số liệu; chưa đưa vào để tránh số liệu bịa.

---

## 10. Màn 8: Dashboard Customer

**Việc chính:** "Đơn của tôi đang ở đâu?"

**Prompt**

```
Screen "Tài khoản của tôi". A one-line greeting with the customer's name.
First block "Đơn đang giao": up to 3 rows, each with order code, shop name, the current delivery sentence
("Đơn vị vận chuyển đang giao hàng đến bạn.") and a link "Theo dõi" on the right.
Empty state: "Bạn chưa có đơn nào đang giao." with a secondary button "Xem sản phẩm".
Second block "Đơn gần đây": a table with "Mã đơn", "Cửa hàng", "Tổng tiền", "Trạng thái", "Ngày đặt", 5 rows,
and a link "Xem tất cả đơn hàng". Third block "Địa chỉ giao hàng": the default address (street, ward, district, province)
and a link "Quản lý địa chỉ"; if there is none, the sentence "Bạn chưa có địa chỉ. Thêm địa chỉ để đặt hàng."
Everything single column, no cards, no charts. Mark sample values "[dữ liệu mẫu]".
Loading, empty and error state per block. Numbered circles (1) to (6).
```

| # | Dữ liệu | Chức năng |
|---|---|---|
| 1 | Tên khách | Xác nhận đúng tài khoản |
| 2 | Tối đa 3 đơn Đang giao + câu trạng thái | Biết đơn đang ở đâu, "Theo dõi" mở màn 4 |
| 3 | Trạng thái rỗng đơn đang giao | Hướng khách tới "Xem sản phẩm" |
| 4 | 5 đơn gần đây | Xem lại và mở chi tiết |
| 5 | Địa chỉ mặc định (đủ quận/huyện) | Kiểm tra nơi nhận trước khi đặt, liên kết quản lý địa chỉ |
| 6 | Lỗi riêng từng khối | Không che hết trang |

---

## 11. Lưu ý trước khi lập trình theo thiết kế

- **Dashboard.** Ba dashboard trong tài liệu này được thiết kế quanh một quyết định mỗi màn. Mình chưa đối chiếu với `AdminDashboardView`, `ManagerDashboardView`, `CustomerDashboardView` hiện có để biết API trả gì. Ô nào chưa có API thì hiện trạng thái rỗng, không điền số.
- **GHN chưa kiểm chứng với dịch vụ thật.** Tạo vận đơn, đồng bộ trạng thái và ba thao tác mới được kiểm thử bằng máy chủ giả. Thiết kế phải có chỗ hiện lỗi GHN nguyên văn.
- **Phí ship.** Dùng phí đã chốt lúc đặt; cần chỗ cho trạng thái "Chưa có phí" khi GHN không trả lời.
- **Đơn sắc và truy cập.** Trạng thái luôn có nhãn chữ; nút nguy hiểm khác nút thường bằng viền đậm và hộp xác nhận, không bằng màu. Hộp thoại đóng được bằng Esc, mọi điều khiển dùng được bằng bàn phím.
- **Nhãn tiếng Việt** khớp với `models/orderStatus.js`; đổi nhãn ở đó để thiết kế và code đồng bộ.
