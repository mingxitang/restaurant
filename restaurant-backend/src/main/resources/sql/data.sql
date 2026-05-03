USE restaurant_db;

INSERT INTO role(role_id, role_name) VALUES
(1, '管理员'), (2, '服务员'), (3, '厨师'), (4, '顾客');

-- 默认密码均为 123456
INSERT INTO `user`(username, phone, points, password, role_id) VALUES
('系统管理员', '13800000000', 0, '$2a$10$5Xf7uAsRmirotatopF4ep.FO2iHaBwDQ26HcS7PmhIq3sheLmhwW2', 1),
('李服务', '13800000001', 0, '$2a$10$5Xf7uAsRmirotatopF4ep.FO2iHaBwDQ26HcS7PmhIq3sheLmhwW2', 2),
('王厨师', '13800000002', 0, '$2a$10$5Xf7uAsRmirotatopF4ep.FO2iHaBwDQ26HcS7PmhIq3sheLmhwW2', 3),
('张顾客', '13800000003', 100, '$2a$10$5Xf7uAsRmirotatopF4ep.FO2iHaBwDQ26HcS7PmhIq3sheLmhwW2', 4);

INSERT INTO category(category_id, category_name) VALUES
(1, '热菜'), (2, '凉菜'), (3, '饮品'), (4, '甜品');

INSERT INTO table_info(table_id, table_number, area, capacity, status) VALUES
(1, 'A01', '大厅', 4, 'FREE'),
(2, 'A02', '大厅', 2, 'FREE'),
(3, 'B01', '包间', 8, 'FREE');

INSERT INTO dish(dish_id, dish_name, price, stock, image, description, category_id, available) VALUES
(1, '宫保鸡丁', 38.00, 100, '', '经典川味热菜，香辣微甜。', 1, TRUE),
(2, '红烧肉', 48.00, 80, '', '肥瘦相间，酱香浓郁。', 1, TRUE),
(3, '拍黄瓜', 12.00, 50, '', '清爽开胃凉菜。', 2, TRUE),
(4, '可乐', 5.00, 200, '', '冰镇饮品。', 3, TRUE),
(5, '杨枝甘露', 22.00, 40, '', '芒果西柚甜品。', 4, TRUE);
