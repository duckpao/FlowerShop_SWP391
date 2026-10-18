# Requirement Specification: Order Management and Dashboards

Section numbers (6 and 7) follow the payment specification and can be renumbered. Screen markers (1), (2)... match the numbered circles in `stitch-design-guide.md`.

## 6. Order Management

### 6.0 Order and Delivery Statuses

An order has one of eight statuses. The UI label is what users see.

| Status | UI Label | Meaning |
|---|---|---|
| Pending | Chờ xác nhận | The customer placed the order; the shop has not confirmed it. |
| Awaiting Deposit | Chờ đặt cọc | A custom order waiting for its deposit. |
| Deposit Paid | Đã cọc | The deposit is paid. |
| Processing | Chờ giao hàng | The shop confirmed the order and is preparing the flowers. |
| Delivering | Đang giao | A GHN waybill exists and the parcel is with the carrier. |
| Completed | Giao thành công | GHN reported the parcel as delivered. Final. |
| Cancelled | Đã hủy | Cancelled by the shop, or the GHN waybill was cancelled. Final. |
| Refunded | Hoàn hàng | GHN returned the parcel to the shop. Final. |

The delivery (GHN waybill) has its own status, read from GHN. The system groups GHN statuses into four stages:

| Stage | GHN statuses |
|---|---|
| Pickup | ready_to_pick, picking, picked |
| Transit | storing, sorting, transporting |
| Delivery | delivering, delivery_fail, delivered |
| Return | waiting_to_return, returning, returned |

GHN staff update pickup, transit and delivery. The shop cannot set these statuses.

### 6.1 Order List (Shop)

[Content #1: UI layout (Mockup screen prototype), numbered markers (1)-(6), as in the table below]

The shop owner opens this screen from the shop management menu.

This screen allows the Shop owner to:
●        View Orders: see every order of the shop, 20 per page, newest first.
●        Filter Orders: view the orders of one status by choosing a tab.
●        Open Order Details: open one order to see its products, addresses and waybill.
●        Process an Order: confirm a pending order, or create the GHN waybill for an order that is ready to ship.

On the screen, s/he can also
●        Cancel an order that has not been handed to GHN (see 6.4).
●        Reload the list by clicking the Reload button.

Field Description

| Field Name | Description |
|---|---|
| (1) Status Tabs | All, Pending, Processing, Delivering, Completed, Cancelled, Refunded. Default: All. Each tab shows the number of orders in that status. Changing the tab returns to page 1. |
| (2) Order Code | First 8 characters of the order ID, read-only. |
| (3) Customer | Customer name with the phone number under it. If the name is missing, "Khách ẩn danh" is shown. Read-only. |
| (4) Total Amount | Order total in VND (products + shipping fee − discount), with thousands separators, read-only. |
| (5) Status | Text badge with the UI label from 6.0. Orders that need an action from the shop (Pending, Processing) use a filled badge; Delivering uses an outlined badge; Completed, Cancelled and Refunded use plain gray text. Status is never shown by color alone. |
| (6) Row Actions | Details: always shown. Confirm Order: Pending orders only. Create GHN Waybill: Processing orders only. Cancel Order: Pending, Awaiting Deposit and Processing orders only. |
| Pagination | 20 orders per page. Previous is disabled on the first page and Next on the last page. The selected tab is kept when the page changes. |

Processing:
The system returns only orders of the shop that belongs to the signed-in owner. An owner who requests another shop's orders gets a "forbidden" error. If no order matches the tab, the screen shows "Chưa có đơn nào ở mục này." If loading fails, the screen shows the error and a Retry button.

### 6.2 Order Queue (Shop)

[Content #1: UI layout (Mockup screen prototype), numbered markers (1)-(6), as in the table below]

The Order Queue lists the orders that wait for a decision from the shop. The owner opens it from the menu or by clicking the badge that shows the number of waiting orders.

This screen allows the Shop owner to:
●        Confirm Order: accept a pending order and start preparing it.
●        Cancel Order: reject an order and give the reason.

Field Description

| Field Name | Description |
|---|---|
| (1) Queue List | Orders in Pending, Awaiting Deposit or Processing status, oldest first. Each row shows the order code, customer name and phone, the first product name followed by "và N sản phẩm khác" when the order has more products, the total, and the waiting time. Read-only. |
| (2) Waiting Time | Time since the order was placed, shown as text ("Đã chờ 3 giờ"). Read-only. |
| (3) Confirm Order | Button, Pending orders only. Moves the order to Processing. |
| (4) Cancel Order | Button. Opens the cancel dialog. |
| (5) Cancel Reason | Free text, required. The Confirm Cancel button stays disabled while the reason is empty. The dialog states that cancelling cannot be undone. |
| (6) Waiting Order Badge | Number of orders in Pending and Awaiting Deposit status, shown on the menu item. |
| Pagination | 20 orders per page. |

Processing:
Confirming moves Pending to Processing and shows "Đã xác nhận đơn hàng, bắt đầu chuẩn bị hoa." Cancelling moves the order to Cancelled and stores the reason. Both actions remove the order from the queue and update the badge. If an action fails, the order keeps its status and the screen shows the server error. If the queue is empty, the screen shows "Không có đơn nào cần xử lý."

### 6.3 Order Details (Shop)

[Content #1: UI layout (Mockup screen prototype), numbered markers (1)-(8), as in the table below]

This screen allows the Shop owner to:
●        View Order Details: see the products, the sender and receiver addresses, the payment status and the order total.
●        Track the Shipment: see the GHN waybill code, the current GHN status and the stage it belongs to.
●        Synchronize with GHN: read the latest waybill status by clicking the Sync from GHN button.
●        Send an Action to GHN: cancel the waybill, request redelivery or request a return (see 6.4).

On the screen, s/he can also
●        Edit the GHN Waybill: change the receiver name, receiver phone, weight and delivery note of an order in Delivering status.
●        Confirm the order or create the waybill from the footer button, depending on the order status.

Field Description

| Field Name | Description |
|---|---|
| (1) Status and Order Date | Status badge from 6.0 and the date the order was placed. Read-only. |
| (2) Sender and Receiver | Sender: shop name and pickup address (street, ward, district, province). Receiver: name, phone and full address. The receiver name and phone come from the order; if they are empty, the customer's account values are used. Read-only. |
| (3) Waybill Code and GHN Status | The GHN tracking code and the Vietnamese label of the current GHN status. "Chưa có" if no waybill exists. Read-only. |
| (4) Stage Strip | Four stages: Pickup, Transit, Delivery, Return. The current stage is bold and finished stages are gray. Shown only when a waybill exists. |
| (5) Sync from GHN | Button. Reads the current status from GHN and updates the order. While the request is pending, the button is disabled and reads "Đang gọi GHN…". |
| (6) GHN Actions | Buttons shown only when GHN accepts them for the current GHN status (see 6.4). If no action is allowed, the text "Ở trạng thái này GHN không nhận thao tác từ shop." is shown. A GHN refusal is shown as received, under the buttons. |
| (7) Products, Total and Payment | Product name, quantity and unit price per line, the order total, and the payment status (Đã thanh toán if a payment succeeded, otherwise Chưa thanh toán). Read-only. |
| (8) Footer Button | Pending: Confirm Order. Processing: Create GHN Waybill and Ship. Delivering: Edit GHN Waybill. Other statuses: Close only. |

Processing:
The system returns the order only if it belongs to the shop; otherwise it returns "not found". The Edit GHN Waybill action sends the changed fields to GHN. Changing the COD amount is not supported, because GHN requires a one-time code for it.

### 6.4 Update Order Status

[Content #1: UI layout (Mockup screen prototype), confirmation dialogs, numbered markers (1)-(3), as in the table below]

Every status change by the shop needs a confirmation dialog. The dialog has a title, one sentence that states the result, a Back button and an action button. Pressing Escape closes it. While the request is pending, the action button is disabled to prevent a second submission. If the request fails, the dialog stays open and shows the reason.

Field Description

| Field Name | Description |
|---|---|
| (1) Confirmation Dialog | Confirm Order; Create GHN Waybill; Cancel Waybill; Request Redelivery; Request Return; Cancel Order (with the reason field from 6.2). |
| (2) Allowed GHN Actions | Cancel Waybill: GHN status ready_to_pick or picking. Request Redelivery: storing or waiting_to_return. Request Return: delivery_fail, storing, waiting_to_return or return. These actions are available only while the order is Delivering. |
| (3) Result Message | Success shows a short message and reloads the list. A GHN refusal shows GHN's reason and changes nothing. |

Status transitions

| From | To | Triggered by | Condition |
|---|---|---|---|
| Pending | Processing | Shop: Confirm Order | None. |
| Pending, Awaiting Deposit, Processing | Cancelled | Shop: Cancel Order | A reason is entered. |
| Processing | Delivering | Shop: Create GHN Waybill | The shop has a pickup address, the receiver has a phone number and the shop owner has a phone number. |
| Delivering | Completed | GHN reports delivered | The payments of the order are marked Success. |
| Delivering | Refunded | GHN reports returned | None. |
| Delivering | Cancelled | GHN reports cancel | The cancel reason is "Vận đơn bị hủy trên GHN." |

Creating the waybill:
The system sends the receiver and sender names, phones and addresses (province, district and ward by name), the product, the weight and the dimensions to GHN. The COD amount is the order total minus the successful payments, so an order that is already paid online has a COD amount of 0. The return address is the shop's pickup address. The system stores the waybill code and a Delivery record with GHN status ready_to_pick, then sets the order to Delivering.

Statuses are read from GHN when the owner or the customer clicks a sync button. The system does not receive GHN callbacks in the current release, so the order status changes only after a sync.

### 6.5 Order Details (Customer)

[Content #1: UI layout (Mockup screen prototype), numbered markers (1)-(6), as in the table below]

This screen allows the Customer to:
●        View Order Details: see the shop, the products, the amounts and the order status.
●        Track the Order: read where the order is in one sentence and in three steps.

On the screen, s/he can also
●        Update the Tracking: click the Update Status button to read the latest status from GHN.
●        Go back to My Orders by clicking the link at the bottom.

Field Description

| Field Name | Description |
|---|---|
| (1) Status and Order Date | Shop name, status badge from 6.0 and order date. Read-only. |
| (2) Delivery Sentence | One sentence about the order, for example "Đơn vị vận chuyển đang giao hàng đến bạn." Cancelled and Refunded orders show an explanation ("Đơn hàng đã bị hủy.", "Đơn vị vận chuyển đã hoàn đơn hàng về cửa hàng."). Read-only. |
| (3) Update Status | Button. Shown only when the order has a waybill and is not Cancelled or Refunded. If GHN cannot be reached, the error appears under the button. |
| (4) Tracking Steps | 1. Cửa hàng đang chuẩn bị đơn hàng. 2. Cửa hàng đã giao cho đơn vị vận chuyển. 3. Đơn vị vận chuyển đã giao đến bạn. Each step has a text state: Hoàn tất, Đang diễn ra or Chưa tới. |
| (5) Products and Amounts | Product, unit price, quantity and line total, then Subtotal, Shipping Fee and Total. The shipping fee is the fee fixed when the order was placed. If GHN's current quote differs, a note says the order total does not change. Read-only. |
| (6) Waybill | Waybill code, carrier (GHN) and GHN status label. Hidden until a waybill exists. |

Processing:
The system returns the order only to its owner. This screen has no edit or cancel button. If loading fails, the screen shows the error and a link back to My Orders.

### 6.6 My Orders (Customer)

This screen allows the Customer to:
●        View Orders: see their own orders, 20 per page, newest first.
●        Filter Orders: choose a status tab (All, Pending, Processing, Delivering, Completed, Cancelled, Refunded).
●        Open Order Details (6.5) by clicking View.

Columns: Order Code, Shop, Total Amount, Status, Order Date. If the customer has no orders, the screen shows "Bạn chưa có đơn nào." If a tab has no orders, it shows "Không có đơn nào ở mục này."

## 7. Dashboards

[Design proposal. These screens are specified from the design in `stitch-design-guide.md`. The data sources were not checked against the dashboard views of the current release, so each item below needs to be matched to an existing API or a new one before development. Where no data is available, the block shows its empty state. The screens never show invented values.]

Each dashboard answers one question and is read-only: every action opens another screen. Each block has its own loading, empty and error state, so one failed block does not hide the page.

### 7.1 Admin Dashboard

Question: what is waiting for my decision?

This screen allows the Admin to:
●        See Pending Work: view the number of shop applications and shops that wait for a decision, with a View button for each.
●        Review New Shops: see the five newest shops.

Field Description

| Field Name | Description |
|---|---|
| (1) Menu | Overview, Shops, Customers, Categories, Shop Applications. |
| (2) Pending Work | One row per kind of work with its count, for example "Đơn mở shop chờ duyệt" and "Cửa hàng chờ kích hoạt". A row with a count of 0 is hidden. When nothing is left, the text "Không có việc nào đang chờ." is shown. |
| (3) View Button | Opens the matching list already filtered. |
| (4) Newest Shops | Shop name, owner, status and creation date of the five newest shops. Clicking a row opens the shop details. |

### 7.2 Shop Dashboard

Question: what do I have to do with my orders today?

This screen allows the Shop owner to:
●        See the Next Action: view how many orders wait for confirmation and how long the oldest one has waited, and open the Order Queue (6.2) with the Process Now button.
●        Check the Order Pipeline: see the number of orders in each status and open the Order List (6.1) on that tab.
●        Review Recent Orders: see the five newest orders.

Field Description

| Field Name | Description |
|---|---|
| (1) Pending Count | Number of Pending orders and the waiting time of the oldest. At 0 the screen shows "Không có đơn nào cần xác nhận." and hides the button. |
| (2) Process Now | Button. Opens the Order Queue. |
| (3) Orders by Status | Pending, Processing, Delivering, Completed, Refunded, Cancelled, each with its count and a link to the Order List. |
| (4) Recent Orders | Order code, customer, total, status and time of the five newest orders. |

Revenue and stock levels are not part of this version. They can be added once the system provides figures that can be traced to orders and products.

### 7.3 Customer Dashboard

Question: where are my orders?

This screen allows the Customer to:
●        Track Orders in Delivery: see up to three orders in Delivering status with their delivery sentence, and open each one with the Track link (6.5).
●        Review Recent Orders: see the five newest orders and open the full list (6.6).
●        Check the Delivery Address: see the default address (street, ward, district, province).

Field Description

| Field Name | Description |
|---|---|
| (1) Greeting | Customer name, read-only. |
| (2) Orders in Delivery | Order code, shop, delivery sentence and a Track link. With no such order the screen shows "Bạn chưa có đơn nào đang giao." and a View Products button. |
| (3) Recent Orders | Order code, shop, total, status and order date of the five newest orders. |
| (4) Default Address | The saved default address with a Manage Addresses link. With no address the screen shows "Bạn chưa có địa chỉ. Thêm địa chỉ để đặt hàng." |

## Differences from the Current Release

This list is for the team. Remove it from the final document.

- The current Order Queue screen is a list with status tabs and newest first. This specification asks for oldest first, a waiting time per row and a separate counter. That needs a change to the queue sorting.
- The Order List shows a count for the selected tab only. A count on every tab needs an extra query.
- The Cancel Order endpoint accepts an empty reason; only the screen requires one. If the rule must hold on the server, the server needs the same check.
- GHN actions (cancel, return, redelivery) and waybill creation have been tested only against a local fake server. No real GHN call has been made, so the allowed-status table in 6.4 still needs a check against GHN.
- The three dashboards in section 7 have not been compared with the existing dashboard views.
