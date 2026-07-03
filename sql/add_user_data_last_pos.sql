-- Lưu vị trí HOME khi logout + trạng thái chết
ALTER TABLE user_data
    ADD COLUMN last_pos VARCHAR(50) NOT NULL DEFAULT '[0,0]',
    ADD COLUMN last_dead TINYINT NOT NULL DEFAULT 0;
