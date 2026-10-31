> Historical design-request API documentation. The active order workflow is documented in [CUSTOM_ORDER_WORKFLOW.md](CUSTOM_ORDER_WORKFLOW.md).

# M04 — Custom Flower Ordering

## Customer flow

1. Open **Hoa theo ngân sách** (`/budget-flowers`). Enter the total budget, quantity (1–100), and optionally choose an active shop.
2. Available, visible products are suggested when `unit price × quantity <= total budget` and stock covers the quantity. Results are sorted by price descending and paginated.
3. Choose a reference bouquet or open **Đặt hoa theo yêu cầu** (`/custom-orders/new`) from a shop page. Select a shop and enter flowers, colors, occasion, wrapping, desired date and description.
4. Sign in as CUSTOMER and submit. The server rechecks shop visibility, reference product ownership, stock and current price. It stores the request as REQUESTED.
5. Open `/custom-orders` and reload responses to see the shop's note, quote and status. Requests remain available after refreshing the page.

## Shop flow

1. Sign in as SHOP and open **Yêu cầu đặt hoa riêng** (`/shop-admin/custom-orders`).
2. Select an owned shop, optionally filter by status, and read the customer's specifications.
3. Respond with NEGOTIATING, ACCEPTED or REJECTED. A note is required; ACCEPTED also requires a total quote within the customer's budget.
4. ACCEPTED and REJECTED are final. Requests cannot be reset to REQUESTED. Updates use a database write lock to prevent conflicting final decisions.

## API and implementation

| Method | Endpoint | Access |
| --- | --- | --- |
| GET | `/api/public/custom-orders/suggestions?budget=500000&quantity=1&shopId=...&page=0` | Public; shopId optional; q optional product-name filter |
| POST | `/api/customer/custom-orders` | Active CUSTOMER |
| GET | `/api/customer/custom-orders?page=0` | Customer's own requests |
| GET | `/api/customer/custom-orders/{id}` | Customer's own request |
| GET | `/api/shop/mine/{shopId}/custom-orders?page=0&status=REQUESTED` | Shop owner; status optional |
| PUT | `/api/shop/mine/{shopId}/custom-orders/{id}` | Active shop and owner |

Create body:

```json
{
  "shopId": "existing-shop-id",
  "budget": 500000,
  "quantity": 2,
  "description": "Two pastel birthday bouquets with pink roses",
  "preferences": {
    "flowers": "Rose, baby breath",
    "colors": "Pink and white",
    "occasion": "Birthday",
    "wrapping": "Kraft paper",
    "deliveryDate": "",
    "referenceProductId": null
  }
}
```

Decision body: `{"status":"ACCEPTED","note":"We can make this design","quotedPrice":450000}`.

`CustomFlowerOrderController` → `CustomFlowerOrderService` → `CustomOrderRequestRepository`, `ShopRepository`, `UserRepository`, `ProductRepository`. Product suggestions use the existing `ProductCardAssembler`.

Frontend: `BudgetFlowersView`, `CustomFlowerOrderView`, `CustomerCustomOrdersView`, `ManagerCustomOrdersView`; shared UI in `CustomFlowerParts`; API calls in `customFlowerService`.

## Storage and scope

Uses the existing `Custom_Order_Requests` table and `CustomOrderRequest` entity. No schema migration is required when the current database.sql schema is installed. Quantity, preferences, reference snapshot, shop response and quote are stored in `desired_components` JSON. Legacy flower arrays and wrapper fields are supported.

Suggestions estimate whole bouquet/product prices using current catalog prices; the database does not hold individual stem prices. Custom specifications require a shop quote. Budget is the total for all requested bouquets. Desired dates cannot be in the past (Asia/Ho_Chi_Minh).

Submitting or accepting a design request does not reserve inventory, create a paid order, take payment, or schedule shipping. These requests use a separate workflow from ordinary product checkout. Shop replies are displayed in the customer's request list; there is no email notification for this workflow.

## Verification

`CustomFlowerOrderTests` covers persistence, private histories, budget/quantity/stock/visibility filtering, current reference prices, status transitions, quote validation, shop ownership, legacy JSON and public/private API access. Uses isolated H2 data and a mocked mail sender.
