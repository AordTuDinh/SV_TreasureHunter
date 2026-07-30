package game.treasure.service.user;

import game.config.aEnum.VipType;
import game.object.MyUser;
import game.treasure.mapping.UserVip;
import game.treasure.mapping.main.ResVipEntity;
import game.treasure.service.resource.ResEvent;

import java.util.List;

public final class VipService {
    private VipService() {
    }

    /** Cộng bonus res_vip.vip_data vào user_settings.vip_data cho từng cấp VIP mới đạt được. */
    public static void applyLevelUpBonuses(MyUser mUser, int fromLevel, int toLevel) {
        if (mUser == null || mUser.getUSetting() == null || fromLevel > toLevel) return;
        UserVip uVip = mUser.getUSetting().getUVip();
        boolean changed = false;
        int shieldHoursAdded = 0;
        for (int level = fromLevel; level <= toLevel; level++) {
            ResVipEntity res = ResEvent.getResVip(level);
            if (res == null) continue;
            List<Integer> pairs = res.getAVipData();
            if (pairs == null || pairs.isEmpty()) continue;
            for (int i = 0; i + 1 < pairs.size(); i += 2) {
                int type = pairs.get(i);
                int value = pairs.get(i + 1);
                uVip.addValue(type, value);
                if (type == VipType.PROTECTION_SHIELD_HOUR && value > 0) {
                    shieldHoursAdded += value;
                }
                changed = true;
            }
        }
        if (changed) {
            uVip.update();
            mUser.getUSetting().syncVipData(uVip.getList());
            VipRuntimeService.rebuildEffectiveVip(mUser);
        }
        if (shieldHoursAdded > 0) {
            ProtectVipService.addHoursToDaily(mUser, shieldHoursAdded, uVip);
        }
    }
}
