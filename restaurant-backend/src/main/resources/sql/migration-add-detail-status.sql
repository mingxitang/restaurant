-- Add cooking status to order_detail for kitchen display system
ALTER TABLE order_detail
ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/PREPARING/READY/SERVED'
AFTER remark;
