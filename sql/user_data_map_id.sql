-- Map đang chơi. 0 = làng tân thủ, 1 = map chính.
-- Chạy một lần trên database game (dson). Bỏ qua nếu cột đã có.

ALTER TABLE dson.user_data
    ADD COLUMN map_id INT NOT NULL DEFAULT 0;
