-- Thanh toán gộp nhiều đơn (mỗi shop một đơn) trong một giao dịch SePay.
-- group_invoice là mã order_invoice_number gửi sang SePay; NULL với thanh toán đơn lẻ.
ALTER TABLE Payments
    ADD COLUMN group_invoice VARCHAR(100) NULL,
    ADD INDEX idx_payments_group_invoice (group_invoice);
