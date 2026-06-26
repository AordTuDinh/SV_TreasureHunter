-- Thêm cột lưu thời điểm kích hoạt artifact (ms tuyệt đối, global CD).
ALTER TABLE user_data ADD COLUMN IF NOT EXISTS time_active_artifact BIGINT NOT NULL DEFAULT 0;
