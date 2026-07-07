package game.treasure.service.user;

import game.battle.object.Point;
import game.object.MyUser;
import game.treasure.mapping.UserEquipmentEntity;

import java.util.List;

/** Bonus ô túi từ trang bị đang mặc — point 23 (bag UI), point 24 (material). */
public final class EquipSlotBonus {

    private EquipSlotBonus() {
    }

    /** > 0 thì lấy phần nguyên (vd 1.5 → 1). */
    public static int floorBonus(float value) {
        return value > 0f ? (int) Math.floor(value) : 0;
    }

    public static int bagBonus(MyUser mUser) {
        return sumEquippedPoint(mUser, Point.SLOT_BAG_UI);
    }

    public static int materialBonus(MyUser mUser) {
        return sumEquippedPoint(mUser, Point.SLOT_BAG_MATERIAL);
    }

    static int sumEquippedPoint(MyUser mUser, int pointId) {
        if (mUser == null || mUser.getUser() == null || mUser.getResources() == null)
            return 0;
        int total = 0;
        for (int itemId : mUser.getUser().getListIdEquipmentEquip()) {
            if (itemId <= 0)
                continue;
            UserEquipmentEntity item = mUser.getResources().getItemEquipment(itemId);
            if (item == null)
                continue;
            List<Long> points = item.getPoint();
            if (points == null || points.isEmpty())
                continue;
            for (int i = 0; i + 1 < points.size(); i += 2) {
                if (points.get(i).intValue() != pointId)
                    continue;
                total += floorBonus(points.get(i + 1).floatValue());
            }
        }
        return total;
    }
}
