# FlowerShop UML Swimlane Diagrams

These UML activity diagrams are aligned with the implemented controllers,
services, status transitions, and integrations. The lanes are business actors
or the FlowerShop system. GHN and SePay are external systems, so each has its own
lane and the FlowerShop System lane calls them explicitly.

Open the diagrams in VS Code with a PlantUML extension and press `Alt+D`.

Layout rules used in every diagram:

- Both branches of a decision start with an action in the lane that holds the
  diamond, so no branch line crosses a lane border at the diamond. Each `else`
  re-declares that lane, otherwise PlantUML continues in the previous branch's lane.
- Every decision branch carries a label and an action. There are no empty
  diamonds.
- Each path ends in its own final node, so branches never merge into a blank
  diamond.
- Loops use a labelled `repeat` decision inside a single lane.

## 1. Standard Cart Checkout

Source flow: `POST /api/orders/{userId}/batch` in `OrderController` and
`OrderService.makeOrders`.

```plantuml
@startuml
title Business Flow - Standard Cart Checkout
skinparam shadowing false
skinparam defaultFontName Arial
skinparam wrapWidth 200
skinparam ArrowColor #555555
skinparam activity {
  BackgroundColor #FFFFFF
  BorderColor #333333
  DiamondBackgroundColor #FFF1C9
  DiamondBorderColor #D88916
  StartColor #111111
  EndColor #111111
}

|#BFE3FF|Customer|
|#D8F3DC|System|
|#D7F0F5|GHN|
|Customer|
start
:Browse active products;
:Add products to cart;
:Choose address, shop, payment method, and optional coupon;
|System|
:Validate cart, product, stock, price, coupon, and GHN address codes;
if (Validation failed?) then (Yes)
  :Return validation errors;
  |Customer|
  :View errors and resubmit checkout information;
  stop
else (No)
  |System|
  :Request shipping fee from GHN;
  |GHN|
  :Calculate and return shipping fee;
  |System|
  :Create PENDING Order and Order_Details per shop;
  :Decrease stock and delete selected Cart_Items;
  if (Online payment selected?) then (Yes)
    :Notify that payment is pending;
    |Customer|
    :Open online payment flow;
    stop
  else (COD)
    |System|
    :Create COD Payment as PENDING;
    |Customer|
    :View order placed confirmation;
    stop
  endif
endif
@enduml
```

## 2. Online Payment and Payment Expiry

Source flow: `PaymentController`, `SePayGatewayService`, and
`PaymentTimeoutScheduler`. `PaymentReturnController` sends the customer back to
the frontend `/payment/{success|error|cancel}` page. SePay has no webhook: the
system asks SePay when the customer returns or polls status, and the scheduler
(every 5 seconds) cancels standard orders still unpaid 10 minutes after creation.

```plantuml
@startuml
title Business Flow - Online Payment and Expiry
skinparam shadowing false
skinparam defaultFontName Arial
skinparam wrapWidth 200
skinparam ArrowColor #555555
skinparam activity {
  BackgroundColor #FFFFFF
  BorderColor #333333
  DiamondBackgroundColor #FFF1C9
  DiamondBorderColor #D88916
  StartColor #111111
  EndColor #111111
}

|#BFE3FF|Customer|
|#D8F3DC|System|
|#FFE0E0|SePay|
|Customer|
start
:Choose an unpaid online order;
|System|
:Check order ownership, status, expiry, and payment method;
if (Order can be paid online?) then (Yes)
  :Calculate payable amount server-side;
  :Create ONLINE Payment as PENDING;
  :Sign SePay checkout fields;
  |Customer|
  :Submit checkout form to SePay;
  |SePay|
  :Process the customer payment;
  |Customer|
  :Return to FlowerShop success, error, or cancel page;
  |System|
  :Query SePay order detail and reconcile the invoice;
  if (SePay reports CAPTURED and amount matches?) then (Yes)
    :Set Payment SUCCESS and standard Order PROCESSING;
    |Customer|
    :View payment success;
    stop
  else (No)
    |System|
    if (Still within 10-minute payment window?) then (Yes)
      :Keep Payment PENDING (amount mismatch sets it FAILED) and Order PENDING;
      |Customer|
      :View pending page and pay again;
      stop
    else (No)
      |System|
      :Scheduler cancels the invoice at SePay and cancels the order;
      :Set Payment FAILED and Order CANCELLED;
      :Release stock and coupon, restore items to cart;
      |Customer|
      :View cancelled order;
      stop
    endif
  endif
else (No)
  |System|
  :Reject the payment request;
  |Customer|
  :View payment rejection;
  stop
endif
@enduml
```

## 3. Shop Manager Order Processing and GHN Delivery

Source flow: `ManagerOrderController`, `ManagerOrderService`, `GhnService`,
and `DeliverySync`. Active shop staff use the same endpoints as the owner. GHN has
no webhook: a waybill status changes only when the shop refreshes it, when a
customer opens a DELIVERING order, or when the daily `MaintenanceJob` (02:00)
completes a DELIVERED order that has no refund and was delivered at least 3 days
earlier.

```plantuml
@startuml
title Business Flow - Shop Order Processing and Delivery
skinparam shadowing false
skinparam defaultFontName Arial
skinparam wrapWidth 200
skinparam ArrowColor #555555
skinparam activity {
  BackgroundColor #FFFFFF
  BorderColor #333333
  DiamondBackgroundColor #FFF1C9
  DiamondBorderColor #D88916
  StartColor #111111
  EndColor #111111
}

|#BFE3FF|Customer|
|#FDE8C8|Shop Manager|
|#D8F3DC|System|
|#D7F0F5|GHN|
|Shop Manager|
start
:Open the shop order queue;
|System|
:Return orders for the owned shop;
|Shop Manager|
:Review a PENDING order;
:Confirm the order;
|System|
:Change Order from PENDING to PROCESSING;
|Shop Manager|
:Prepare the flowers;
:Request shipment;
|System|
:Validate PROCESSING or DEPOSIT_PAID status and that no waybill exists;
:Read shop and recipient addresses;
:Record unpaid balance as COD if none exists;
:Request GHN createOrder;
|GHN|
:Create shipment and return tracking code;
|System|
if (Shipment created?) then (Yes)
  :Save Delivery with tracking code;
  :Change Order to DELIVERING;
  :Request GHN tracking when the shop refreshes status;
  |GHN|
  :Return GHN delivery status;
  |System|
  :Map GHN status to DeliveryStatus;
  if (Delivery still in transit?) then (Yes)
    :Keep Order DELIVERING;
    |Shop Manager|
    :Check tracking again later;
    stop
  else (No)
    |System|
    :Set Order COMPLETED, REFUNDED, or CANCELLED from the final GHN status;
    :Mark COD payment SUCCESS when delivered;
    |Customer|
    :Receive the delivery outcome;
    stop
  endif
else (No)
  |System|
  :Return the GHN error (HTTP 502); order stays PROCESSING or DEPOSIT_PAID;
  |Shop Manager|
  :Review the error, then retry shipment or cancel the order with a reason;
  stop
endif
@enduml
```

## 4. Custom Flower Order and Deposit

Source flow: `CustomFlowerOrderController`, `OrderController`,
`OrderService.makeCustomOrder`, and `SePayGatewayService`. Orders quoted on an
existing custom order (`AWAITING_QUOTE` to `QUOTED`) use
`CustomOrderWorkflowService.accept`, where the customer chooses FULL or DEPOSIT.
DEPOSIT requires ONLINE payment, its errors return HTTP 409, and the deposit comes
from `CustomOrderPricing.deposit`.

```plantuml
@startuml
title Business Flow - Custom Flower Order Deposit
skinparam shadowing false
skinparam defaultFontName Arial
skinparam wrapWidth 200
skinparam ArrowColor #555555
skinparam activity {
  BackgroundColor #FFFFFF
  BorderColor #333333
  DiamondBackgroundColor #FFF1C9
  DiamondBorderColor #D88916
  StartColor #111111
  EndColor #111111
}

|#BFE3FF|Customer|
|#FDE8C8|Shop Manager|
|#D8F3DC|System|
|#D7F0F5|GHN|
|#FFE0E0|SePay|
|Customer|
start
:Submit custom flower request;
|System|
:Create CustomOrderRequest;
|Shop Manager|
:Review request and provide a quote;
if (Shop declines request?) then (Yes)
  :Decline the request;
  |Customer|
  :View shop response;
  stop
else (No)
  |Shop Manager|
  if (Needs more discussion?) then (Yes)
    :Mark request NEGOTIATING with a note;
    |Customer|
    :View shop note and discuss in chat;
    stop
  else (No)
  |Shop Manager|
  :Accept the request with quoted price and deposit percent;
  |System|
  :Mark custom request ACCEPTED;
  |Customer|
  :Choose delivery address and request deposit order;
  |System|
  :Validate accepted request and quote;
  :Request shipping fee from GHN;
  |GHN|
  :Calculate and return shipping fee;
  |System|
  :Create CUSTOM Order as AWAITING_DEPOSIT;
  :Create order detail from custom materials;
  |Customer|
  :Start DEPOSIT payment in SePay;
  |SePay|
  :Process the deposit payment;
  |System|
  :Reconcile deposit invoice;
  if (Deposit captured?) then (Yes)
    :Set Payment SUCCESS and Order DEPOSIT_PAID;
    |Shop Manager|
    :Prepare the flowers;
    :Create GHN shipment from DEPOSIT_PAID; GHN collects the balance;
    stop
  else (No)
    |System|
    :Keep deposit pending until the 24-hour deadline;
    :Cancel expired deposit order;
    |Customer|
    :View cancelled custom order;
    stop
  endif
  endif
endif
@enduml
```

## 5. Manager Shop Application

Source flow: `ManagerApplicationService`. A REJECTED application can be submitted
again. Admin decisions must include a `note` field, which may be blank.

```plantuml
@startuml
title Business Flow - Shop Application
skinparam shadowing false
skinparam defaultFontName Arial
skinparam wrapWidth 200
skinparam ArrowColor #555555
skinparam activity {
  BackgroundColor #FFFFFF
  BorderColor #333333
  DiamondBackgroundColor #FFF1C9
  DiamondBorderColor #D88916
  StartColor #111111
  EndColor #111111
}

|#BFE3FF|Customer|
|#D8F3DC|System|
|#F3D9FF|Admin|
|Customer|
start
:Complete manager application;
|System|
:Check active CUSTOMER, verified email, and no existing shop;
if (Not eligible?) then (Yes)
  :Return eligibility error;
  |Customer|
  :View eligibility error;
  stop
else (No)
  |System|
  :Save Manager_Application as PENDING;
  |Admin|
  :Review manager application;
  if (Approve?) then (Yes)
    :Approve the application;
    |System|
    :Create Shop ACTIVE and Address;
    :Change User role to SHOP;
    :Revoke old sessions;
    :Set application APPROVED;
    |Customer|
    :View approved shop application;
    stop
  else (No)
    |Admin|
    :Reject the application with a note;
    |System|
    :Set application REJECTED;
    |Customer|
    :View rejected application;
    stop
  endif
endif
@enduml
```

## 6. Staff Application

Source flow: `StaffApplicationService`. Only the shop owner can review, and the
owner's review screen (`StaffApplicationsView`) is currently not mounted in the
UI, so reviews go through the API only. Shop staff cannot review applications.

```plantuml
@startuml
title Business Flow - Staff Application
skinparam shadowing false
skinparam defaultFontName Arial
skinparam wrapWidth 200
skinparam ArrowColor #555555
skinparam activity {
  BackgroundColor #FFFFFF
  BorderColor #333333
  DiamondBackgroundColor #FFF1C9
  DiamondBorderColor #D88916
  StartColor #111111
  EndColor #111111
}

|#BFE3FF|Customer|
|#D8F3DC|System|
|#FDE8C8|Shop Manager|
|Customer|
start
:Complete staff application for an active shop;
|System|
:Check active verified CUSTOMER, active shop, no current membership, and no pending application;
:Save Staff_Application as PENDING and email the shop owner;
|Shop Manager|
:Review staff application;
if (Approve?) then (Yes)
  :Approve the application;
  |System|
  :Create ShopStaff membership;
  :Change User role to SHOP_STAFF;
  :Revoke old sessions;
  :Set application APPROVED and email the applicant;
  |Customer|
  :View approved staff application;
  stop
else (No)
  |Shop Manager|
  :Reject the application;
  |System|
  :Set staff application REJECTED and email the applicant;
  |Customer|
  :View rejected staff application;
  stop
endif
@enduml
```

## 7. Staff Refund Handling

Source flow: `StaffRefundController` and `StaffRefundService`. The shop owner or an
active staff member can list refunds for the shop and update refund status.
Allowed moves: REQUESTED to APPROVED or REJECTED, and APPROVED to PROCESSED or
REJECTED; other moves return HTTP 400. Only PROCESSED and REJECTED notify the
customer. Active shop staff can also confirm orders and create GHN shipments
(section 3) and use shop chat (section 8).

```plantuml
@startuml
title Business Flow - Staff Refund Handling
skinparam shadowing false
skinparam defaultFontName Arial
skinparam wrapWidth 200
skinparam ArrowColor #555555
skinparam activity {
  BackgroundColor #FFFFFF
  BorderColor #333333
  DiamondBackgroundColor #FFF1C9
  DiamondBorderColor #D88916
  StartColor #111111
  EndColor #111111
}

|#FFF0D6|Shop Staff|
|#D8F3DC|System|
|#BFE3FF|Customer|
|Shop Staff|
start
:Open refunds for an assigned shop;
|System|
:Check shop owner or active staff membership;
:Return refund requests;
|Shop Staff|
:Review refund request;
:Submit refund status update;
|System|
:Validate refund, shop access, and status transition;
if (Status update invalid?) then (Yes)
  :Return validation error;
  |Shop Staff|
  :View validation error;
  stop
else (No)
  |System|
  :Save refund status and shop response;
  if (Final status PROCESSED or REJECTED?) then (Yes)
    :Send refund result notification;
    |Customer|
    :Receive refund result;
    stop
  else (No)
    :Keep refund open for the next status update;
    stop
  endif
endif
@enduml
```

## 8. Customer and Shop Manager Chat

Source flow: `CustomerChatController`, `ManagerChatController`, and
`ChatService`. Active shop staff use the same shop chat endpoints as the owner.

```plantuml
@startuml
title Business Flow - Customer and Shop Manager Chat
skinparam shadowing false
skinparam defaultFontName Arial
skinparam wrapWidth 200
skinparam ArrowColor #555555
skinparam activity {
  BackgroundColor #FFFFFF
  BorderColor #333333
  DiamondBackgroundColor #FFF1C9
  DiamondBorderColor #D88916
  StartColor #111111
  EndColor #111111
}

|#BFE3FF|Customer|
|#D8F3DC|System|
|#FDE8C8|Shop Manager or Staff|
|Customer|
start
:Choose an active shop;
|System|
:Find or create customer-shop ChatSession;
:Authorize customer side of session;
|Customer|
:Send message or image;
|System|
:Save ChatMessage;
:Update unread count and notify the recipient if no unread notice exists;
:Broadcast through STOMP;
|Shop Manager or Staff|
:Receive message in shop chat;
:Reply to customer;
|System|
:Authorize shop owner or active staff side of session;
:Save reply and broadcast through STOMP;
|Customer|
:Receive shop reply;
if (Continue conversation?) then (Yes)
  :Send another message;
  |System|
  :Save and broadcast next message;
  stop
else (No)
  |Customer|
  :Open the session to mark it read;
  |System|
  :Mark messages as read;
  stop
endif
@enduml
```
