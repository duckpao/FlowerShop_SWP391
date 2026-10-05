ALTER TABLE Manager_Applications
    ADD COLUMN review_note VARCHAR(1000) NULL,
    ADD COLUMN reviewed_at DATETIME(6) NULL,
    ADD COLUMN reviewed_by VARCHAR(36) NULL,
    ADD COLUMN shop_id VARCHAR(36) NULL;