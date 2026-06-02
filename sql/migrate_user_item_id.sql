-- Migration: user_item — PK id (bigint), bỏ cột number, user_id + item_id là index phụ
-- Chạy trên DB game trước khi deploy server mới.

ALTER TABLE user_item DROP PRIMARY KEY;

ALTER TABLE user_item ADD COLUMN id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY FIRST;

ALTER TABLE user_item DROP COLUMN number;

CREATE INDEX idx_user_item_user_id ON user_item (user_id);
CREATE INDEX idx_user_item_item_id ON user_item (item_id);
CREATE INDEX idx_user_item_user_item ON user_item (user_id, item_id);
