-- VIP runtime data (user) + config pairs (res_vip)
-- MySQL < 8.0.29 không hỗ trợ ADD COLUMN IF NOT EXISTS — chạy 1 lần, bỏ qua nếu cột đã có.

ALTER TABLE user_settings
    ADD COLUMN vip_data VARCHAR(512) NOT NULL DEFAULT '[]' COMMENT 'JSON [26 int] tích lũy theo VipType index';

ALTER TABLE res_vip
    ADD COLUMN vip_data VARCHAR(1024) NOT NULL DEFAULT '[]' COMMENT 'JSON [type,value,type,value,...] bonus khi lên cấp VIP';
