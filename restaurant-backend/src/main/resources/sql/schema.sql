CREATE DATABASE IF NOT EXISTS restaurant_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE restaurant_db;

DROP TABLE IF EXISTS review;
DROP TABLE IF EXISTS refund_record;
DROP TABLE IF EXISTS order_detail;
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS dish;
DROP TABLE IF EXISTS table_info;
DROP TABLE IF EXISTS `user`;
DROP TABLE IF EXISTS category;
DROP TABLE IF EXISTS role;

CREATE TABLE role (
    role_id INT AUTO_INCREMENT PRIMARY KEY,
    role_name VARCHAR(50) NOT NULL UNIQUE COMMENT '管理员/服务员/厨师/顾客'
) ENGINE=InnoDB COMMENT='角色信息表';

CREATE TABLE `user` (
    user_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    phone VARCHAR(20) NOT NULL UNIQUE,
    points INT NOT NULL DEFAULT 0,
    password VARCHAR(255) NOT NULL,
    role_id INT NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_role FOREIGN KEY (role_id) REFERENCES role(role_id) ON DELETE RESTRICT
) ENGINE=InnoDB COMMENT='用户信息表';

CREATE TABLE category (
    category_id INT AUTO_INCREMENT PRIMARY KEY,
    category_name VARCHAR(50) NOT NULL UNIQUE,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='菜品分类表';

CREATE TABLE table_info (
    table_id INT AUTO_INCREMENT PRIMARY KEY,
    table_number VARCHAR(20) NOT NULL UNIQUE,
    table_name VARCHAR(50),
    area VARCHAR(50),
    capacity INT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'FREE',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='桌位信息表';

CREATE TABLE dish (
    dish_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    dish_name VARCHAR(100) NOT NULL,
    price DECIMAL(10,2) NOT NULL CHECK (price >= 0),
    stock INT NOT NULL DEFAULT 0 CHECK (stock >= 0),
    image VARCHAR(255),
    description TEXT,
    category_id INT NOT NULL,
    available BOOLEAN NOT NULL DEFAULT TRUE,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_dish_category FOREIGN KEY (category_id) REFERENCES category(category_id) ON DELETE RESTRICT
) ENGINE=InnoDB COMMENT='菜品信息表';

CREATE TABLE orders (
    order_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    total_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    paid_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    discount_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    pay_no VARCHAR(64),
    pay_method VARCHAR(30),
    table_id INT NOT NULL,
    user_id BIGINT NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_order_table FOREIGN KEY (table_id) REFERENCES table_info(table_id) ON DELETE RESTRICT,
    CONSTRAINT fk_order_user FOREIGN KEY (user_id) REFERENCES `user`(user_id) ON DELETE RESTRICT
) ENGINE=InnoDB COMMENT='订单信息表';

CREATE TABLE order_detail (
    order_id BIGINT NOT NULL,
    dish_id BIGINT NOT NULL,
    quantity INT NOT NULL CHECK (quantity > 0),
    unit_price DECIMAL(10,2) NOT NULL,
    remark VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/PREPARING/READY/SERVED',
    PRIMARY KEY (order_id, dish_id),
    CONSTRAINT fk_detail_order FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE CASCADE,
    CONSTRAINT fk_detail_dish FOREIGN KEY (dish_id) REFERENCES dish(dish_id) ON DELETE RESTRICT
) ENGINE=InnoDB COMMENT='订单详情表';

CREATE TABLE refund_record (
    refund_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    refund_reason VARCHAR(255),
    quantity INT NOT NULL CHECK (quantity > 0),
    refund_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    stock_action TINYINT NOT NULL COMMENT '0-报废, 1-回库',
    refund_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    order_id BIGINT NOT NULL,
    dish_id BIGINT NOT NULL,
    CONSTRAINT fk_refund_order FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE CASCADE,
    CONSTRAINT fk_refund_dish FOREIGN KEY (dish_id) REFERENCES dish(dish_id) ON DELETE RESTRICT
) ENGINE=InnoDB COMMENT='退换记录表';

CREATE TABLE review (
    review_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rating TINYINT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment TEXT,
    review_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    order_id BIGINT NOT NULL UNIQUE,
    CONSTRAINT fk_review_order FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE CASCADE
) ENGINE=InnoDB COMMENT='评价信息表';

CREATE INDEX idx_dish_name ON dish(dish_name);
CREATE INDEX idx_order_time ON orders(order_time);
CREATE INDEX idx_order_status ON orders(status);
CREATE INDEX idx_refund_time ON refund_record(refund_time);

CREATE OR REPLACE VIEW v_order_details AS
SELECT o.order_id, o.order_time, t.table_number, d.dish_name, od.quantity, od.unit_price,
       od.quantity * od.unit_price AS subtotal
FROM orders o
JOIN table_info t ON o.table_id = t.table_id
JOIN order_detail od ON o.order_id = od.order_id
JOIN dish d ON od.dish_id = d.dish_id;

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

CREATE OR REPLACE VIEW v_monthly_revenue_trend AS
SELECT DATE_FORMAT(order_time, '%Y-%m') AS report_month,
       COUNT(order_id) AS total_orders,
       SUM(CASE WHEN status IN ('PAID','COMPLETED') THEN paid_amount ELSE 0 END) AS total_revenue
FROM orders
GROUP BY DATE_FORMAT(order_time, '%Y-%m');
