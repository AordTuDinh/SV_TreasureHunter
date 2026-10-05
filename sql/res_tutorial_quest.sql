-- Quest tuần tự. quest_type = [type, idInfo]. goto_id = 0 (chưa gán nút đi tới).
-- Chạy trên DB main. Cột name phải là utf8mb4 thì mới chứa tiếng Việt.

SET NAMES utf8mb4;

ALTER TABLE res_tutorial_quest
    MODIFY name VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '';

DELETE FROM res_tutorial_quest;

INSERT INTO res_tutorial_quest (id, name, num, goto_id, bonus, quest_type) VALUES
(1,  'Khai thác hộp',                  1,    0, '[1,100]',                         '[1,0]'),
(2,  'Đào 10 ô đất',                   10,   0, '[1,100]',                         '[2,0]'),
(3,  'Đánh bại quái 1',                1,    0, '[2,1]',                           '[3,1]'),
(4,  'Mặc trang bị',                   3,    0, '[2,1]',                           '[4,0]'),
(5,  'Nâng cấp trang bị',              3,    0, '[2,1]',                           '[5,0]'),
(6,  'Đánh quái 2',                    3,    0, '[1,300,2,1]',                     '[3,2]'),
(7,  'Đánh bại quái 3',                3,    0, '[2,4]',                           '[3,3]'),
(8,  'Đào đá máu',                     1,    0, '[2,1]',                           '[6,1]'),
(9,  'Đào đá phòng thủ',               1,    0, '[2,1]',                           '[6,2]'),
(10, 'Nâng đá lên lv 2',               1,    0, '[2,1]',                           '[7,2]'),
(11, 'Gặp NPC ghép nguyên liệu',       1,    0, '[2,1]',                           '[8,1]'),
(12, 'Hợp nhất đá',                    1,    0, '[12,1302000,4]',                  '[9,0]'),
(13, 'Tìm lửa trại',                   1,    0, '[11,3,1,11,3,1,11,5,1,11,5,1]',   '[8,4]'),
(14, 'Chế tạo trang bị',               1,    0, '[2,1]',                           '[10,0]'),
(15, 'Chế tạo thuốc',                  1,    0, '[2,1]',                           '[11,0]'),
(16, 'Gặp Mira',                       1,    0, '[1,600,2,1]',                     '[8,2]'),
(17, 'Đánh bại quái 4',                 2,    0, '[1,500,2,1]',                     '[3,4]'),
(18, 'Mặc đồ huyền thoại',             5,    0, '[10,1,1]',                        '[12,4]'),
(19, 'Đánh bại rồng xanh',             1,    0, '[9,1,1]',                         '[3,16]'),
(20, 'Gặp gỡ teleport',                1,    0, '[2,10]',                          '[8,3]'),
(21, 'Ghép đá',                        3,    0, '[2,5]',                           '[13,0]'),
(22, 'Chế tạo trang bị',               4,    0, '[2,5]',                           '[10,0]'),
(23, 'Đào đất',                        50,   0, '[2,2]',                           '[2,0]'),
(24, 'Thức tỉnh thú cưng',             1,    0, '[2,5]',                           '[14,0]'),
(25, 'Chế tạo cánh',                   1,    0, '[2,5]',                           '[15,0]'),
(26, 'Đánh bại người chơi',            1,    0, '[2,2]',                           '[16,0]'),
(27, 'Đạt cấp 5 lò rèn',               5,    0, '[2,2]',                           '[17,5]'),
(28, 'Sở hữu nguyên liệu huyền thoại', 1,    0, '[11,1,4]',                        '[18,4]'),
(29, 'Đạt lực chiến',                  1000, 0, '[2,10]',                          '[19,0]'),
(30, 'Tham gia đấu trường',            1,    0, '[2,5]',                           '[20,0]'),
(31, 'Tham gia bang hội',              1,    0, '[2,10]',                          '[21,0]'),
(32, 'Mở rương',                       1,    0, '[2,10]',                          '[22,0]');

-- Test từ đầu: UPDATE user_data SET quest_tutorial = 1, quest_tutorial_number = 0;
