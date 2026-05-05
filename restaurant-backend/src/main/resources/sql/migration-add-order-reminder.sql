USE restaurant_db;

ALTER TABLE orders
ADD COLUMN reminder_count INT NOT NULL DEFAULT 0 AFTER status,
ADD COLUMN last_reminder_time DATETIME NULL AFTER reminder_count;
