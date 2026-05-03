-- Add cooking status to order_detail for kitchen display system
ALTER TABLE order_detail
ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/PREPARING/READY/SERVED'
AFTER remark;

-- Kitchen queue view for chef display
CREATE OR REPLACE VIEW v_kitchen_queue AS
SELECT od.order_id, od.dish_id, d.dish_name, od.quantity, od.remark,
       o.table_id, t.table_number, o.order_time,
       TIMESTAMPDIFF(MINUTE, o.order_time, NOW()) AS wait_minutes,
       od.status AS cooking_status
FROM order_detail od
JOIN orders o ON od.order_id = o.order_id
JOIN dish d ON od.dish_id = d.dish_id
JOIN table_info t ON o.table_id = t.table_id
WHERE o.status IN ('PENDING','PAID')
  AND (od.status IS NULL OR od.status != 'SERVED')
ORDER BY o.order_time ASC;
