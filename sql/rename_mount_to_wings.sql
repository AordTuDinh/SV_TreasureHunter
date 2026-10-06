-- Đổi schema mount -> wings.
-- user_* nằm ở database dson. res_* / config nằm ở dson_main.
-- Chạy một lần. Bỏ qua lệnh nếu bảng/cột đã đổi.

-- Resource (nếu server vẫn còn tên cũ)
RENAME TABLE dson_main.res_mount TO dson_main.res_wings;

-- Instance người chơi
RENAME TABLE dson.user_mount TO dson.user_wings;
ALTER TABLE dson.user_wings RENAME COLUMN mount_id TO wings_id;

-- Config vòng quay: key JSON "mount" -> "wings"
UPDATE dson_main.config
SET v = REPLACE(v, '"mount"', '"wings"')
WHERE k = 'config_luckySpine' AND v LIKE '%"mount"%';

UPDATE dson_main.config_dev
SET v = REPLACE(v, '"mount"', '"wings"')
WHERE k = 'config_luckySpine' AND v LIKE '%"mount"%';
