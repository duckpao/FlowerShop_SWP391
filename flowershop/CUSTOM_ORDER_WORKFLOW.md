# Custom flower orders in the shared order workflow

Apply `sql/022_custom_order_workflow.sql` to the target database before starting this version.
Also apply `sql/023_custom_flower_options.sql` for database-managed choices in the custom order form. See `CUSTOM_FLOWER_CATALOG.md` for configuration and Disable instructions.
For databases merged from master, follow `sql/MIGRATION_ORDER.md`, including the schema reconciliation scripts. Do not reimport `database.sql` over an existing database.

Customer submits a design and an owned saved delivery address at `/custom-orders/new`.
Missing GHN codes on an older address do not prevent the design request from being saved.
The shop receives an in-app notification linking directly to the new order. The customer
must complete the address's GHN codes before the shop can finalize the shipping quote;
order details show an address-completion prompt until then.
Flower budget, bouquet quantity and budget-based reference suggestions are integrated
into this form. The separate budget-flower page and navigation entry have been removed.
This creates one `Orders` row with `order_type=CUSTOM`, plus a single batch line in
`Order_Details`. The line's `custom_materials` JSON contains the optional
description and design preferences, including the requested bouquet quantity. No budget is stored: the request input has no budget field.
A reference product is for inspiration only; its stock is never reserved or restored.
Design JSON uses Hibernate's explicit JSON JDBC mapping; older string-encoded snapshots
remain readable. See the [Hibernate JSON mapping documentation](https://docs.jboss.org/hibernate/orm/current/userguide/html_single/Hibernate_User_Guide.html#basic-mapping-json).

Workflow:

1. `AWAITING_QUOTE`: appears in the shop's normal order management and queue.
2. Shop reviews the design, then rejects it (`CANCELLED`, reason required) or quotes the full batch price.
   No budget check applies to the price. GHN shipping is quoted
   server-side and shown separately. Status becomes `QUOTED`.
3. Customer reviews the quote in the normal order details and agrees to its current total.
   A stale total is rejected. COD creates the usual pending COD payment and moves the
   order to `PROCESSING`. Online payment moves it to `PENDING` and uses the existing SePay
   checkout. The ten-minute payment window starts at acceptance, not request creation. With `paymentType: "DEPOSIT"` (requires ONLINE), the order moves to `AWAITING_DEPOSIT` with `depositAmount` set by `CustomOrderPricing.deposit(...)`; this path stores no deposit percent, so the default is 50% of the total. The deposit window is 24 hours from acceptance. A mismatched `expectedDeposit` returns 409 (`Mức cọc đã thay đổi...`).
4. Successful online payment moves to `PROCESSING`. Shop creates the normal GHN shipment;
   delivery tracking and completion use the shared `DELIVERING` / `COMPLETED` flow.

## Endpoints

| Method | Endpoint | Purpose |
| --- | --- | --- |
| POST | `/api/customer/orders/custom` | `{addressId, design: {shopId, budget, quantity, description, preferences}}` |
| POST | `/api/shop/mine/{shopId}/orders/{id}/quote` | `{amount, note}`; amount is the full flower batch price |
| POST | `/api/customer/orders/{id}/accept-quote` | `{paymentMethod: "COD" or "ONLINE", expectedTotal, optional paymentType: "FULL" (default) or "DEPOSIT", optional expectedDeposit, optional couponCode and shippingCouponCode}` |
| PUT | `/api/customer/orders/{id}/custom-address` | `{addressId}`; complete an imported request's missing address |

All lists, detail views, cancellation, payment and delivery use the existing order APIs.
Updates that alter the quote or accept it use an order write lock.

## Earlier requests

Migration copies earlier `Custom_Order_Requests` rows into `Orders`, preserving their IDs
and source records. Earlier rejected requests become cancelled orders. Other requests
require a fresh quote so the customer can explicitly agree to shipping and payment.
If no valid saved address exists, the imported order asks the customer to supply one
before the shop can quote it. Changing the delivery address requires a fresh quote.
The old shop navigation entry is removed. Chat cards for new CUSTOM orders link directly
to normal order details and enforce both customer and shop ownership. Old chat bookmarks
resolve their linked order (whether migration preserved the request ID or an earlier
deposit order references it). Requests without a linked order remain viewable as history;
customers can submit a fresh design through the shared workflow.

Legacy request creation (`POST /api/customer/custom-orders`), request decisions
(`PUT /api/shop/mine/{shopId}/custom-orders/{id}`), and separate deposit-order creation
(`POST /api/orders/{userId}/custom/{requestId}`) are still registered and not retired; no HTTP 410 response exists in the backend. Legacy GET endpoints
remain available for history. Existing deposit orders retain their payment/delivery
handling, and their earlier unwrapped design JSON remains readable. This change does
not delete historical requests, orders, payments, or catalog data.

## Verification

`gradlew.bat test -x buildFrontend -x installFrontend`

`CustomOrderWorkflowTests` covers shared lists, budget and ownership checks, customer
agreement, COD shipment completion, online payment timeout, duplicate/stale acceptance,
legacy missing addresses, and the HTTP flow with authentication and CSRF protection.
It also commits a request with a legacy address, reloads customer/shop API lists in a
fresh transaction, and checks notifications and address completion before quoting.
Payment and carrier calls in these tests are mocked; they do not charge or ship anything.
