-- Hóa hình tier trên item đã craft: 0=chưa craft, 1=craft không HH, 2+=craft+HH tier
ALTER TABLE user_item ADD COLUMN hh INT NOT NULL DEFAULT 0;
ALTER TABLE user_equipment ADD COLUMN hh INT NOT NULL DEFAULT 0;
ALTER TABLE user_pet ADD COLUMN hh INT NOT NULL DEFAULT 0;
ALTER TABLE user_mount ADD COLUMN hh INT NOT NULL DEFAULT 0;
ALTER TABLE user_artifact ADD COLUMN hh INT NOT NULL DEFAULT 0;

-- Backfill consumable đã craft (icon HH 1000–1005 → hh 2–7)
UPDATE user_item SET hh = 1 WHERE is_craft = 1 AND hh = 0 AND (icon < 1000 OR icon > 1005);
UPDATE user_item SET hh = icon - 998 WHERE is_craft = 1 AND icon >= 1000 AND icon <= 1005;

UPDATE user_equipment SET hh = 1 WHERE is_craft = 1 AND hh = 0;
UPDATE user_pet SET hh = 1 WHERE is_craft = 1 AND hh = 0;
UPDATE user_mount SET hh = 1 WHERE is_craft = 1 AND hh = 0;
UPDATE user_artifact SET hh = 1 WHERE is_craft = 1 AND hh = 0;
