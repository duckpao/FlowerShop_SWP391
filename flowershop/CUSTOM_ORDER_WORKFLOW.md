# Custom flower orders in the shared order workflow

Apply `sql/022_custom_order_workflow.sql` to the target database before starting this version.
The local database used during development has already been migrated. The script can be rerun.

Customer submits a design and an owned GHN delivery address at `/custom-orders/new`.
This creates one `Orders` row with `order_type=CUSTOM`, plus a single batch line in
`Order_Details`. The line's `custom_materials` JSON contains the flower budget, optional
description and design preferences, including the requested bouquet quantity.
A reference product is for inspiration only; its stock is never reserved or restored.

Workflow:

1. `AWAITING_QUOTE`: appears in the shop's normal order management and queue.
2. Shop reviews the design, then rejects it (`CANCELLED`) or quotes the full batch price.
   The flower price cannot exceed the customer's flower budget. GHN shipping is quoted
   server-side and shown separately. Status becomes `QUOTED`.
3. Customer reviews the quote in the normal order details and agrees to its current total.
   A stale total is rejected. COD creates the usual pending COD payment and moves the
   order to `PROCESSING`. Online payment moves it to `PENDING` and uses the existing SePay
   checkout. The ten-minute payment window starts at acceptance, not request creation.
4. Successful online payment moves to `PROCESSING`. Shop creates the normal GHN shipment;
   delivery tracking and completion use the shared `DELIVERING` / `COMPLETED` flow.

## Endpoints

| Method | Endpoint | Purpose |
| --- | --- | --- |
| POST | `/api/customer/orders/custom` | `{addressId, design: {shopId, budget, quantity, description, preferences}}` |
| POST | `/api/shop/mine/{shopId}/orders/{id}/quote` | `{amount, note}`; amount is the full flower batch price |
| POST | `/api/customer/orders/{id}/accept-quote` | `{paymentMethod: "COD" or "ONLINE", expectedTotal}` |
| PUT | `/api/customer/orders/{id}/custom-address` | `{addressId}`; complete an imported request's missing address |

All lists, detail views, cancellation, payment and delivery use the existing order APIs.
Updates that alter the quote or accept it use an order write lock.

## Earlier requests

Migration copies earlier `Custom_Order_Requests` rows into `Orders`, preserving their IDs
and source records. Earlier rejected requests become cancelled orders. Other requests
require a fresh quote so the customer can explicitly agree to shipping and payment.
If no valid saved address exists, the imported order asks the customer to supply one
before the shop can quote it. Changing the delivery address requires a fresh quote.
The old shop navigation entry is removed; old shop links open normal order management.

## Verification

`gradlew.bat test -x buildFrontend -x installFrontend`

`CustomOrderWorkflowTests` covers shared lists, budget and ownership checks, customer
agreement, COD shipment completion, online payment timeout, duplicate/stale acceptance,
legacy missing addresses, and the HTTP flow with authentication and CSRF protection.
Payment and carrier calls in these tests are mocked; they do not charge or ship anything.
