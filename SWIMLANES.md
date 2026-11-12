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
`PaymentTimeoutScheduler`.

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
  |System|
  while (Still pending within payment window?) is (Yes)
    :Request invoice status from SePay;
    |SePay|
    :Return invoice status;
    |System|
  endwhile (No)
  if (SePay reports CAPTURED and amount matches?) then (Yes)
    :Set Payment SUCCESS and standard Order PROCESSING;
    |Customer|
    :View payment success;
    stop
  else (No)
    |System|
    :Mark payment failed or expired;
    :Scheduler cancels the unpaid order;
    :Restore cancelled standard-order items to cart;
    |Customer|
    :View cancelled order;
    stop
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
and `DeliverySync`.

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
:Validate PROCESSING or DEPOSIT_PAID status;
:Read shop and recipient addresses;
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
  :Return the shipment creation error;
  |Shop Manager|
  :Review the error, then retry shipment or cancel the order;
  stop
endif
@enduml
```

## 4. Custom Flower Order and Deposit

Source flow: `CustomFlowerOrderController`, `OrderController`,
`OrderService.makeCustomOrder`, and `SePayGatewayService`.

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
  :Accept the request with the quote;
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
    :Prepare accepted custom order;
    :Create GHN shipment after order processing;
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
@enduml
```

## 5. Manager Shop Application

Source flow: `ManagerApplicationService`.

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

Source flow: `StaffApplicationService`.

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
:Check active CUSTOMER and active shop;
:Save Staff_Application as PENDING;
|Shop Manager|
:Review staff application;
if (Approve?) then (Yes)
  :Approve the application;
  |System|
  :Create ShopStaff membership;
  :Change User role to SHOP_STAFF;
  :Revoke old sessions;
  :Set application APPROVED;
  |Customer|
  :View approved staff application;
  stop
else (No)
  |Shop Manager|
  :Reject the application;
  |System|
  :Set staff application REJECTED;
  |Customer|
  :View rejected staff application;
  stop
endif
@enduml
```

## 7. Staff Refund Handling

Source flow: `StaffRefundController` and `StaffRefundService`. Staff can list
refunds for an assigned shop and update refund status; staff do not confirm
orders, create GHN shipments, or operate chat in the current code.

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
:Check SHOP_STAFF role and shop assignment;
:Return refund requests;
|Shop Staff|
:Review refund request;
:Submit refund status update;
|System|
:Validate refund, shop, staff membership, and target status;
if (Status update invalid?) then (Yes)
  :Return validation error;
  |Shop Staff|
  :View validation error;
  stop
else (No)
  |System|
  :Save refund status;
  |Customer|
  :Receive refund status notification;
  stop
endif
@enduml
```

## 8. Customer and Shop Manager Chat

Source flow: `CustomerChatController`, `ManagerChatController`, and
`ChatService`. Shop staff has no chat endpoint in the current implementation.

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
|#FDE8C8|Shop Manager|
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
:Update unread count and notification;
:Broadcast through STOMP;
|Shop Manager|
:Receive message in shop chat;
:Reply to customer;
|System|
:Authorize shop owner side of session;
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
