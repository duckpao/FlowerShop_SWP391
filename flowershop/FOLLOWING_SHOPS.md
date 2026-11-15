# Following shops

## Behavior

- Public shop directory: `/shops`. Search by shop name; only ACTIVE shops appear.
- Shop information and products: `/shops/{id}`.
- Customer followed-shop list: `/following-shops`. `/favorite-shops` remains compatible.
- Following enables new-product email by default. Unfollowing stops future emails and cancels unsent notifications when they are processed.
- A product is announced on its first public ACTIVE save or first activation. Drafts, admin-hidden products, inactive shops, ordinary edits, and repeated hide/unhide do not generate another announcement.
- Followers must be ACTIVE CUSTOMER accounts with verified email. Recipients are determined when the product is first published, and checked again before delivery.
- Publication and pending mail rows commit with the product. The scheduled worker sends one email per recipient (no shared To/CC list), processes 20 at a time, and retries up to five times with increasing delays. Failures do not roll back product creation.
- SMTP is at-least-once: a process crash after the SMTP server accepts a message but before the database commits can cause a duplicate delivery. Publication records and recipient uniqueness prevent duplicates from ordinary product edits.

## Setup

1. Apply `sql/020_shop_follow_notifications.sql` to the existing database. `ddl-auto=none` does not create these tables automatically. Do not re-run the full database reset scripts.
2. Optionally apply `sql/021_demo_follow_shops.sql`. This inserts three sample shops and their owners without changing existing accounts. Sample owners have no password and use reserved test addresses; use an existing real shop owner to test publishing, or set demo credentials through your local admin tooling.
3. Restart the backend. Configure `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, and `MAIL_FROM` using the existing SMTP setup.
4. Set `FRONTEND_BASE_URL` to the URL customers can open (default `http://localhost:5173`). For a deployed app, localhost links must be replaced with the public URL.
5. The worker is enabled by default. Set `SHOP_FOLLOW_MAIL_ENABLED=false` to disable actual delivery for local experiments; publication and pending records are still saved. Set `SHOP_FOLLOW_MAIL_INTERVAL_MS` to adjust the default 30-second poll interval.

## Manual checks

1. Sign in as a customer with a verified email. Open `/shops`, view a shop, follow it, and reload `/following-shops`.
2. Sign in separately as that shop's owner. Create a new ACTIVE product. Within the next worker cycle, the customer should receive its shop name, product name, price, description, and product/shop links.
3. Check `Shop_Product_Mails`: PENDING → SENT; failures retain attempt counts and become FAILED after five attempts.
4. Edit the same product or hide/unhide it: no new announcement should be queued.
5. Create an INACTIVE product: no announcement. Activate it once: announcement queued.
6. Unfollow before delivery: the pending message becomes CANCELLED. Other customers' personal follow lists remain isolated.

The automated tests mock SMTP; they do not send real emails.
