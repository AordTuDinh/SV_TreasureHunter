-- =============================================================================
-- Gộp user_item_equipment -> user_item
-- type: 1=consumable, 2=equipment, 3=currency (gold/gem/ruby), 4=event
-- Chạy trên schema game (đổi dson. nếu DB khác tên).
-- Mỗi ALTER chạy riêng: nếu báo Duplicate column thì bỏ qua lệnh đó (đã có cột).
-- =============================================================================

USE dson;

-- -----------------------------------------------------------------------------
-- Bước 1: Thêm cột mới (MySQL không hỗ trợ ADD COLUMN IF NOT EXISTS)
-- -----------------------------------------------------------------------------

ALTER TABLE user_item
  ADD COLUMN type INT NOT NULL DEFAULT 1 COMMENT '1=use,2=equip,3=currency,4=event' AFTER item_id;

ALTER TABLE user_item
  ADD COLUMN level INT NOT NULL DEFAULT 0;

ALTER TABLE user_item
  ADD COLUMN lock_destroy TINYINT NOT NULL DEFAULT 0;

ALTER TABLE user_item
  ADD COLUMN tier INT NOT NULL DEFAULT 0;

ALTER TABLE user_item
  ADD COLUMN point TEXT NULL;

-- Cột data thường đã có trên user_item (vé số, quest...). Chỉ chạy nếu chưa có:
-- ALTER TABLE user_item ADD COLUMN data TEXT NULL;

-- -----------------------------------------------------------------------------
-- Bước 2: Copy trang bị — GIỮ NGUYÊN id (hero đang lưu id equip)
-- Bỏ qua row nếu id đã tồn tại trong user_item
-- -----------------------------------------------------------------------------

INSERT INTO user_item (id, user_id, item_id, type, level, lock_destroy, tier, point, data)
SELECT
  e.id,
  e.user_id,
  e.item_id,
  2,
  IFNULL(e.level, 0),
  IFNULL(e.lock_destroy, 0),
  IFNULL(e.tier, 1),
  IFNULL(e.point, '[]'),
  '[]'
FROM user_item_equipment e
WHERE NOT EXISTS (
  SELECT 1 FROM user_item u WHERE u.id = e.id
);

-- Nếu bảng equipment dùng cột is_lock thay lock_destroy, dùng lệnh thay thế bước 2:
/*
INSERT INTO user_item (id, user_id, item_id, type, level, lock_destroy, tier, point, data)
SELECT
  e.id, e.user_id, e.item_id, 2,
  IFNULL(e.level, 0), IFNULL(e.is_lock, 0), IFNULL(e.tier, 1),
  IFNULL(e.point, '[]'), '[]'
FROM user_item_equipment e
WHERE NOT EXISTS (SELECT 1 FROM user_item u WHERE u.id = e.id);
*/

-- Cập nhật AUTO_INCREMENT sau khi insert id thủ công
SET @next_ai = (SELECT IFNULL(MAX(id), 0) + 1 FROM user_item);
SET @sql_ai = CONCAT('ALTER TABLE user_item AUTO_INCREMENT = ', @next_ai);
PREPARE stmt_ai FROM @sql_ai;
EXECUTE stmt_ai;
DEALLOCATE PREPARE stmt_ai;

-- -----------------------------------------------------------------------------
-- Bước 3: Gán type cho row consumable cũ (đang type=1 mặc định)
-- ItemKey: GOLD=5, GEM=6, RUBY=7 (pbmethod.proto)
-- -----------------------------------------------------------------------------

UPDATE user_item
SET type = 3
WHERE type = 1
  AND item_id IN (5, 6, 7);

-- Event / nguyên liệu sự kiện (res_item.type = 1 = ITEM_MATERIAL)
UPDATE user_item ui
INNER JOIN res_item ri ON ui.item_id = ri.id
SET ui.type = 4
WHERE ui.type = 1
  AND ri.type = 1;

-- -----------------------------------------------------------------------------
-- Bước 4: Kiểm tra trước khi xóa bảng cũ
-- SELECT type, COUNT(*) FROM user_item GROUP BY type;
-- SELECT COUNT(*) FROM user_item_equipment;
-- -----------------------------------------------------------------------------

-- DROP TABLE user_item_equipment;
