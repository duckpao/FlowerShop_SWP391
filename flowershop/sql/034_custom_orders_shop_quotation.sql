-- Existing databases: preserve historical budgets, allow requests without a customer price.
-- Select the application's schema before running. Safe to run again.
ALTER TABLE Custom_Order_Requests MODIFY COLUMN budget DECIMAL(12, 2) NULL;
