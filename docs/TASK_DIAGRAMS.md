# Class and sequence diagrams per task

Mỗi task có một class diagram và một sequence diagram, vẽ theo code trong `flowershop/src/main/java`.
File nguồn PlantUML nằm trong [sds/](sds/), cạnh ảnh PNG. Màu class: tím là View (React), xanh lá là Controller, xanh dương là Service, vàng là Repository, xanh lá nhạt là Entity, đỏ nhạt là hệ thống ngoài.

Render lại: `java -Dfile.encoding=UTF-8 -jar plantuml.jar -charset UTF-8 -tpng sds/*.puml`.

## 1. Home Page

Trang chính sau khi user đăng nhập. Lớp chính: `HomeView`, `PublicCatalogController`, `PublicShopController`, `NotificationController`.

**Class diagram** ([nguồn](sds/2.7-home-page-class.puml))

![Home Page class](sds/2.7-home-page-class.png)

**Sequence diagram** ([nguồn](sds/2.7-home-page-sequence.puml))

![Home Page sequence](sds/2.7-home-page-sequence.png)

## 2. Order Management

Shop quản lý và xử lý đơn hàng (danh sách và filter, thêm mới, chỉnh sửa). Lớp chính: `ManagerOrderController`, `ManagerOrderService`.

**Class diagram** ([nguồn](sds/3.9-order-management-class.puml))

![Order Management class](sds/3.9-order-management-class.png)

**Sequence diagram** ([nguồn](sds/3.9-order-management-sequence.puml))

![Order Management sequence](sds/3.9-order-management-sequence.png)

## 3. Order Queue

Danh sách đơn đang chờ xử lý. Lớp chính: `ManagerOrderService.queueCount`, `list(status)`.

**Class diagram** ([nguồn](sds/3.2-order-queue-alt-class.puml))

![Order Queue class](sds/3.2-order-queue-alt-class.png)

**Sequence diagram** ([nguồn](sds/3.2-order-queue-alt-sequence.puml))

![Order Queue sequence](sds/3.2-order-queue-alt-sequence.png)

## 4. Update Order Status

Cập nhật trạng thái đơn. Lớp chính: `ManagerOrderService.confirm`, `changeStatus`, `OrderStatusListener`.

**Class diagram** ([nguồn](sds/3.7-update-order-status-class.puml))

![Update Order Status class](sds/3.7-update-order-status-class.png)

**Sequence diagram** ([nguồn](sds/3.7-update-order-status-alt-sequence.puml))

![Update Order Status sequence](sds/3.7-update-order-status-alt-sequence.png)

## 5. View Order Details

Xem chi tiết đơn. Lớp chính: `ManagerOrderService.detail`, `buildDetail`.

**Class diagram** ([nguồn](sds/3.1-order-details-alt-class.puml))

![View Order Details class](sds/3.1-order-details-alt-class.png)

**Sequence diagram** ([nguồn](sds/3.1-order-details-alt-sequence.puml))

![View Order Details sequence](sds/3.1-order-details-alt-sequence.png)

## 6. Assign Order to Delivery

Tích hợp giao hàng. Lớp chính: `ManagerOrderService.ship`, `refreshStatus`, `GhnService`, `DeliverySync`.

**Class diagram** ([nguồn](sds/3.3-create-ghn-waybill-alt-class.puml))

![Assign Order to Delivery class](sds/3.3-create-ghn-waybill-alt-class.png)

**Sequence diagram** ([nguồn](sds/3.3-create-ghn-waybill-alt-sequence.puml))

![Assign Order to Delivery sequence](sds/3.3-create-ghn-waybill-alt-sequence.png)

## 7. Notify User Confirmation

Xác nhận đơn hàng qua mail. Lớp chính: `OrderMailSender.onPlaced`, `onStatusChanged`, `HtmlMailSender`.

**Class diagram** ([nguồn](sds/13.3-notify-user-confirmation-class.puml))

![Notify User Confirmation class](sds/13.3-notify-user-confirmation-class.png)

**Sequence diagram** ([nguồn](sds/13.3-notify-user-confirmation-sequence.puml))

![Notify User Confirmation sequence](sds/13.3-notify-user-confirmation-sequence.png)

## 8. Refund management for shop

Shop quản lý hoàn tiền cho user. Lớp chính: `StaffRefundController`, `StaffRefundService`, `NotificationService`.

**Class diagram** ([nguồn](sds/5.4-refund-management-class.puml))

![Refund management for shop class](sds/5.4-refund-management-class.png)

**Sequence diagram** ([nguồn](sds/5.4-refund-management-sequence.puml))

![Refund management for shop sequence](sds/5.4-refund-management-sequence.png)

## 9. Notify Upcoming Occasion via Email

Nhắc khách về sinh nhật, ngày lễ, kỷ niệm. Lớp chính: `OccasionReminderJob`, `OccasionReminderService`.

**Class diagram** ([nguồn](sds/13.2-notify-upcoming-occasion-class.puml))

![Notify Upcoming Occasion via Email class](sds/13.2-notify-upcoming-occasion-class.png)

**Sequence diagram** ([nguồn](sds/13.2-notify-upcoming-occasion-sequence.puml))

![Notify Upcoming Occasion via Email sequence](sds/13.2-notify-upcoming-occasion-sequence.png)

## 10. Notify Order Delivered via Email

Thông báo đơn đã giao. Lớp chính: `DeliverySync`, `OrderStatusListener`, `OrderMailSender`.

**Class diagram** ([nguồn](sds/13.4-notify-order-delivered-class.puml))

![Notify Order Delivered via Email class](sds/13.4-notify-order-delivered-class.png)

**Sequence diagram** ([nguồn](sds/13.4-notify-order-delivered-sequence.puml))

![Notify Order Delivered via Email sequence](sds/13.4-notify-order-delivered-sequence.png)

## Ghi chú khớp với code

- **Order Management, thêm mới:** shop không tạo đơn. Đơn mới đến từ checkout của customer (`OrderService.makeOrders`) rồi hiện trong danh sách của shop. Phần "chỉnh sửa" được vẽ là hủy đơn (`cancel`, bắt buộc có lý do) và cập nhật vận đơn GHN (`updateGhn`).
- **Order Queue:** hàng đợi đếm đơn `PENDING`, `AWAITING_DEPOSIT`, `AWAITING_QUOTE` (`queueCount`); danh sách `queue` mặc định gồm thêm `DEPOSIT_PAID`, `PROCESSING`, `QUOTED`, và màn hình có thể lọc theo từng trạng thái.
- **Refund:** yêu cầu hoàn tiền được tạo khi shop hủy đơn đã thanh toán. Việc cập nhật trạng thái do role `SHOP_STAFF` làm qua `StaffRefundController`, theo luồng REQUESTED → APPROVED → PROCESSED, hoặc REJECTED. Khách cũng có thể tự gửi yêu cầu hoàn tiền (`CustomerRefundService`) và hủy yêu cầu đó (trạng thái CANCELLED).
- **Mail đơn hàng:** mọi mail đổi trạng thái đi qua `OrderStatusListener` (JPA `@PostUpdate`) và `OrderMailSender` chạy `@Async` sau khi commit. Vì vậy xác nhận đơn và đơn đã giao dùng chung một cơ chế.
- **Nhắc dịp đặc biệt:** `OccasionReminderJob` chạy với `fixedDelay` 15 phút, lần đầu sau 1 phút. Mail lỗi thì không đánh dấu đã nhắc để lần quét sau thử lại.
- **Home Page:** các endpoint được gọi từ `HomeView` và controller của nó. Thông báo chuông dùng `/api/account/notifications`.
