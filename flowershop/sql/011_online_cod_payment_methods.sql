-- Normalize all legacy gateway-specific values into the two business-facing
-- payment methods supported by the application: ONLINE and COD.

ALTER TABLE Payments
    MODIFY COLUMN payment_method
        ENUM('VNPAY', 'MOMO', 'BANK_TRANSFER', 'SEPAY', 'ONLINE', 'COD') NOT NULL;

UPDATE Payments
SET payment_method = 'ONLINE'
WHERE payment_method <> 'COD';

ALTER TABLE Payments
    MODIFY COLUMN payment_method ENUM('ONLINE', 'COD') NOT NULL;

-- invoice_number is reconciliation metadata for ONLINE payments, not a payment
-- method. Add it only when upgrading an older database that does not have it.
SET @invoice_column_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'Payments'
      AND COLUMN_NAME = 'invoice_number'
);

SET @add_invoice_column_sql = IF(
    @invoice_column_exists = 0,
    'ALTER TABLE Payments ADD COLUMN invoice_number VARCHAR(100) NULL AFTER gateway_transaction_no',
    'SELECT 1'
);

PREPARE add_invoice_column_statement FROM @add_invoice_column_sql;
EXECUTE add_invoice_column_statement;
DEALLOCATE PREPARE add_invoice_column_statement;

SET @invoice_unique_index_exists = (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'Payments'
      AND COLUMN_NAME = 'invoice_number'
      AND NON_UNIQUE = 0
);

SET @add_invoice_unique_index_sql = IF(
    @invoice_unique_index_exists = 0,
    'ALTER TABLE Payments ADD CONSTRAINT uq_payments_invoice_number UNIQUE (invoice_number)',
    'SELECT 1'
);

PREPARE add_invoice_unique_index_statement FROM @add_invoice_unique_index_sql;
EXECUTE add_invoice_unique_index_statement;
DEALLOCATE PREPARE add_invoice_unique_index_statement;
