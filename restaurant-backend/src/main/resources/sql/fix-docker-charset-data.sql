USE restaurant_db;
SET NAMES utf8mb4;

UPDATE role SET role_name = '管理员' WHERE role_id = 1;
UPDATE role SET role_name = '服务员' WHERE role_id = 2;
UPDATE role SET role_name = '厨师' WHERE role_id = 3;
UPDATE role SET role_name = '顾客' WHERE role_id = 4;

UPDATE `user` SET username = '系统管理员' WHERE user_id = 1;
UPDATE `user` SET username = '李服务' WHERE user_id = 2;
UPDATE `user` SET username = '王厨师' WHERE user_id = 3;
UPDATE `user` SET username = '张顾客' WHERE user_id = 4;

UPDATE category SET category_name = '热菜' WHERE category_id = 1;
UPDATE category SET category_name = '凉菜' WHERE category_id = 2;
UPDATE category SET category_name = '饮品' WHERE category_id = 3;
UPDATE category SET category_name = '甜品' WHERE category_id = 4;

UPDATE table_info SET table_number = 'A01', area = '大厅' WHERE table_id = 1;
UPDATE table_info SET table_number = 'A02', area = '大厅' WHERE table_id = 2;
UPDATE table_info SET table_number = 'B01', area = '包间' WHERE table_id = 3;

UPDATE dish SET dish_name = '宫保鸡丁', image = '', description = '经典川味热菜，香辣微甜。' WHERE dish_id = 1;
UPDATE dish SET dish_name = '红烧肉', image = '', description = '肥瘦相间，酱香浓郁。' WHERE dish_id = 2;
UPDATE dish SET dish_name = '拍黄瓜', image = '', description = '清爽开胃凉菜。' WHERE dish_id = 3;
UPDATE dish SET dish_name = '可乐', image = '', description = '冰镇饮品。' WHERE dish_id = 4;
UPDATE dish SET dish_name = '杨枝甘露', image = '', description = '芒果西柚甜品。' WHERE dish_id = 5;
