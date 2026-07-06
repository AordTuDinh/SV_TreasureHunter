-- Player trading marketplace schema (run manually on game DB)

ALTER TABLE user_pet ADD COLUMN is_trading INT NOT NULL DEFAULT 0;
ALTER TABLE user_pet ADD COLUMN in_market INT NOT NULL DEFAULT 0;
ALTER TABLE user_mount ADD COLUMN is_trading INT NOT NULL DEFAULT 0;
ALTER TABLE user_mount ADD COLUMN in_market INT NOT NULL DEFAULT 0;
ALTER TABLE user_item ADD COLUMN is_trading INT NOT NULL DEFAULT 0;
ALTER TABLE user_item ADD COLUMN in_market INT NOT NULL DEFAULT 0;
ALTER TABLE user_equipment ADD COLUMN is_trading INT NOT NULL DEFAULT 0;
ALTER TABLE user_equipment ADD COLUMN in_market INT NOT NULL DEFAULT 0;
ALTER TABLE user_material ADD COLUMN is_trading INT NOT NULL DEFAULT 0;
ALTER TABLE user_material ADD COLUMN in_market INT NOT NULL DEFAULT 0;
ALTER TABLE user_mob ADD COLUMN is_trading INT NOT NULL DEFAULT 0;
ALTER TABLE user_mob ADD COLUMN in_market INT NOT NULL DEFAULT 0;
ALTER TABLE user_artifact ADD COLUMN is_trading INT NOT NULL DEFAULT 0;
ALTER TABLE user_artifact ADD COLUMN in_market INT NOT NULL DEFAULT 0;
ALTER TABLE user_skin ADD COLUMN is_craft INT NOT NULL DEFAULT 0;
ALTER TABLE user_skin ADD COLUMN is_trading INT NOT NULL DEFAULT 0;
ALTER TABLE user_skin ADD COLUMN in_market INT NOT NULL DEFAULT 0;

CREATE TABLE IF NOT EXISTS user_trading (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id INT NOT NULL,
  server INT NOT NULL,
  tab INT NOT NULL,
  item_type INT NOT NULL,
  item_id BIGINT NOT NULL,
  item_info TEXT,
  price INT NOT NULL,
  verify_until BIGINT NOT NULL,
  date_created DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_server_tab (server, tab),
  INDEX idx_user (user_id)
);

-- Lang chợ tự do — bảng dson_main.config_language
INSERT INTO dson_main.config_language (k, vi) VALUES
  ('err_trading_wallet_full', 'Túi giao dịch không còn ô trống.'),
  ('err_trading_waiting', 'Vật phẩm đang chờ xác minh, vui lòng thử lại sau.'),
  ('err_trading_need_craft', 'Chỉ có thể đăng bán vật phẩm đã chế tạo.'),
  ('err_trading_poison_not_ready', 'Thuốc độc chưa đủ điều kiện đăng bán.'),
  ('err_trading_material_not_ready', 'Nguyên liệu cần đạt tier 4 và cấp 10 mới được đăng bán.'),
  ('err_bag_full', 'Túi không còn ô trống.')
ON DUPLICATE KEY UPDATE vi = VALUES(vi);
