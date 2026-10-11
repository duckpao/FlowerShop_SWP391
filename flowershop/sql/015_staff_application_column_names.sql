SET @staff_full_name_old_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'Staff_Applications'
      AND COLUMN_NAME = 'fullName'
);

SET @staff_full_name_new_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'Staff_Applications'
      AND COLUMN_NAME = 'full_name'
);

SET @rename_staff_full_name_sql = IF(
    @staff_full_name_old_exists > 0 AND @staff_full_name_new_exists = 0,
    'ALTER TABLE Staff_Applications CHANGE COLUMN fullName full_name VARCHAR(100) NOT NULL',
    'SELECT 1'
);

PREPARE rename_staff_full_name_statement FROM @rename_staff_full_name_sql;
EXECUTE rename_staff_full_name_statement;
DEALLOCATE PREPARE rename_staff_full_name_statement;

SET @staff_submitted_at_old_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'Staff_Applications'
      AND COLUMN_NAME = 'submittedAt'
);

SET @staff_submitted_at_new_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'Staff_Applications'
      AND COLUMN_NAME = 'submitted_at'
);

SET @rename_staff_submitted_at_sql = IF(
    @staff_submitted_at_old_exists > 0 AND @staff_submitted_at_new_exists = 0,
    'ALTER TABLE Staff_Applications CHANGE COLUMN submittedAt submitted_at DATETIME(6) NOT NULL',
    'SELECT 1'
);

PREPARE rename_staff_submitted_at_statement FROM @rename_staff_submitted_at_sql;
EXECUTE rename_staff_submitted_at_statement;
DEALLOCATE PREPARE rename_staff_submitted_at_statement;