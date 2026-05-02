USE restaurant_db;

ALTER TABLE order_detail
    ADD COLUMN remark VARCHAR(255) NULL AFTER unit_price;
