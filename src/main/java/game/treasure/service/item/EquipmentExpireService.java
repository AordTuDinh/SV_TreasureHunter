package game.treasure.service.item;

import game.object.MyUser;
import game.treasure.mapping.UserEquipmentEntity;
import game.treasure.service.user.Bonus;
import ozudo.base.log.Logs;

import java.util.ArrayList;
import java.util.List;

/**
 * Trang bị hết hạn: gỡ đang mặc + xóa khỏi bag slot.
 * Item vẫn giữ trong inventory (client hiện overlay expired).
 */
public final class EquipmentExpireService {
    public static final long DEFAULT_DURATION_SECONDS = 30L * 24 * 60 * 60;

    private EquipmentExpireService() {
    }

    public static long nowSeconds() {
        return System.currentTimeMillis() / 1000L;
    }

    public static long defaultExpireAt() {
        return nowSeconds() + DEFAULT_DURATION_SECONDS;
    }

    /** Sync khi login / load resources. */
    public static void purgeExpiredOnLogin(MyUser mUser) {
        if (mUser == null || mUser.getResources() == null)
            return;
        List<UserEquipmentEntity> expired = new ArrayList<>();
        for (UserEquipmentEntity equip : mUser.getResources().listEquipment()) {
            if (equip != null && equip.isExpired())
                expired.add(equip);
        }
        for (UserEquipmentEntity equip : expired)
            unequipAndClearBag(mUser, equip);
    }

    public static boolean unequipAndClearBag(MyUser mUser, UserEquipmentEntity equip) {
        if (mUser == null || equip == null)
            return false;
        try {
            boolean wasEquipped = equip.isEquip()
                    || mUser.getUser().getListIdEquipmentEquip().contains((int) equip.getId());
            if (wasEquipped) {
                List<Integer> lst = mUser.getUser().normalizeItemEquipList();
                int slotIndex = game.treasure.mapping.UserEntity.findEquipSlotByItemId(lst, (int) equip.getId());
                if (slotIndex >= 0) {
                    lst.set(slotIndex, 0);
                    lst.set(slotIndex + 1, 0);
                    lst.set(slotIndex + 2, 0);
                    if (!mUser.getUser().updateItemEquip(lst))
                        Logs.warn("EquipmentExpire unequip failed id=" + equip.getId());
                }
                equip.unEquip();
            }
            Bonus.clearItemFromSlot(mUser, Bonus.BONUS_EQUIPMENT, equip.getId());
            equip.setBagSlot(-1);
            return true;
        } catch (Exception ex) {
            Logs.error(ex);
            return false;
        }
    }
}
