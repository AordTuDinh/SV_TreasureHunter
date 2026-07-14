-- Hạn sử dụng trang bị (unix seconds). -1 = vĩnh viễn, >0 = thời điểm hết hạn.
ALTER TABLE user_equipment ADD COLUMN time_expire BIGINT NOT NULL DEFAULT -1;
