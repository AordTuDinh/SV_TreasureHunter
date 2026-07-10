package game.treasure.service.user;

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
        for (int level = fromLevel; level <= toLevel; level++) {
            ResVipEntity res = ResEvent.getResVip(level);
            if (res == null) continue;
            List<Integer> pairs = res.getAVipData();
            if (pairs == null || pairs.isEmpty()) continue;
            for (int i = 0; i + 1 < pairs.size(); i += 2) {
                uVip.addValue(pairs.get(i), pairs.get(i + 1));
                changed = true;
            }
        }
        if (changed) {
            uVip.update();
            mUser.getUSetting().syncVipData(uVip.getList());
            mUser.queueVipDataSync();
        }
    }
}
