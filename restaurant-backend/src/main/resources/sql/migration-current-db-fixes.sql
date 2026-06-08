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

CREATE TABLE IF NOT EXISTS stock_change_log (
    log_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    dish_id BIGINT NOT NULL,
    order_id BIGINT NULL,
    quantity INT NOT NULL CHECK (quantity > 0),
    change_type VARCHAR(10) NOT NULL COMMENT 'IN/OUT',
    reason VARCHAR(50) NOT NULL COMMENT 'ORDER_CREATE/ORDER_CANCEL/REFUND_RETURN/MANUAL_ADJUST',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_stock_log_dish FOREIGN KEY (dish_id) REFERENCES dish(dish_id) ON DELETE RESTRICT,
    CONSTRAINT fk_stock_log_order FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE SET NULL
) ENGINE=InnoDB COMMENT='库存变动流水表';

SET @has_stock_log_dish_time_idx = (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'stock_change_log'
      AND INDEX_NAME = 'idx_stock_log_dish_time'
);
SET @sql = IF(
    @has_stock_log_dish_time_idx = 0,
    'CREATE INDEX idx_stock_log_dish_time ON stock_change_log(dish_id, create_time)',
    'SELECT ''idx_stock_log_dish_time already exists'' AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_stock_log_order_idx = (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'stock_change_log'
      AND INDEX_NAME = 'idx_stock_log_order'
);
SET @sql = IF(
    @has_stock_log_order_idx = 0,
    'CREATE INDEX idx_stock_log_order ON stock_change_log(order_id)',
    'SELECT ''idx_stock_log_order already exists'' AS message'
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
