-- Bảng user_item thống nhất (consumable + equipment + currency + event)
-- type: 1=dùng thường, 2=trang bị, 3=gold/gem/ruby, 4=sự kiện
-- Chạy trên DB game (đổi dson. nếu cần)

USE dson;

DROP TABLE IF EXISTS user_item_equipment;
DROP TABLE IF EXISTS user_item;

CREATE TABLE user_item (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  user_id       INT          NOT NULL,
  item_id       INT          NOT NULL,
  type          INT          NOT NULL DEFAULT 1 COMMENT '1=consumable,2=equipment,3=currency,4=event',
  level         INT          NOT NULL DEFAULT 0,
  lock_destroy  TINYINT      NOT NULL DEFAULT 0,
  tier          INT          NOT NULL DEFAULT 0,
  slot          INT          NOT NULL DEFAULT 0 COMMENT 'index ô trong túi UI',
  point         TEXT         NULL,
  data          TEXT         NULL,
  PRIMARY KEY (id),
  KEY idx_user_item_user_id (user_id),
  KEY idx_user_item_item_id (item_id),
  KEY idx_user_item_user_item (user_id, item_id),
  KEY idx_user_item_type (user_id, type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
