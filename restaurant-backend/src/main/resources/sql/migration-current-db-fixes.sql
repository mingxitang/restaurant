USE restaurant_db;

SET @has_detail_status = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'order_detail'
      AND COLUMN_NAME = 'status'
);
SET @sql = IF(
    @has_detail_status = 0,
    'ALTER TABLE order_detail ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT ''PENDING'' COMMENT ''PENDING/PREPARING/READY/SERVED'' AFTER remark',
    'SELECT ''order_detail.status already exists'' AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_reminder_count = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'orders'
      AND COLUMN_NAME = 'reminder_count'
);
SET @sql = IF(
    @has_reminder_count = 0,
    'ALTER TABLE orders ADD COLUMN reminder_count INT NOT NULL DEFAULT 0 AFTER status',
    'SELECT ''orders.reminder_count already exists'' AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_last_reminder_time = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'orders'
      AND COLUMN_NAME = 'last_reminder_time'
);
SET @sql = IF(
    @has_last_reminder_time = 0,
    'ALTER TABLE orders ADD COLUMN last_reminder_time DATETIME NULL AFTER reminder_count',
    'SELECT ''orders.last_reminder_time already exists'' AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE OR REPLACE VIEW v_kitchen_queue AS
SELECT od.order_id, od.dish_id, d.dish_name, od.quantity, od.remark,
       o.table_id, t.table_number, o.order_time,
       o.reminder_count, o.last_reminder_time,
       TIMESTAMPDIFF(MINUTE, o.order_time, NOW()) AS wait_minutes,
       od.status AS cooking_status
FROM order_detail od
JOIN orders o ON od.order_id = o.order_id
JOIN dish d ON od.dish_id = d.dish_id
JOIN table_info t ON o.table_id = t.table_id
WHERE o.status IN ('PENDING','PAID')
  AND (od.status IS NULL OR od.status != 'SERVED')
ORDER BY o.order_time ASC;
