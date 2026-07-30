package game.treasure.service.user;

import game.config.aEnum.PackType;
import game.config.aEnum.VipType;
import game.object.MyUser;
import game.treasure.mapping.UserPackEntity;
import game.treasure.mapping.main.ResPackEntity;
import ozudo.base.helper.GsonUtil;
import ozudo.base.helper.StringHelper;

import java.util.ArrayList;
import java.util.List;

/** VIP runtime = clone user_settings.vip_data + buff từ thẻ đặc quyền (pack 12/13/14) còn HSD. */
public final class VipRuntimeService {
    private static final int[] PRIVILEGE_PACK_IDS = {
            PackType.THE_VINH_VIEN.value,
            PackType.THE_THANG.value,
            PackType.THE_TUAN.value,
    };

    private VipRuntimeService() {
    }

    public static boolean isPrivilegePack(int packId) {
        return packId == PackType.THE_VINH_VIEN.value
                || packId == PackType.THE_THANG.value
                || packId == PackType.THE_TUAN.value;
    }

    /** Clone vip_data persist, cộng res_pack.data từ thẻ còn hạn, push client. */
    public static void rebuildEffectiveVip(MyUser mUser) {
        if (mUser == null || mUser.getUSetting() == null || mUser.getResources() == null)
            return;
        List<Integer> base = new ArrayList<>(mUser.getUSetting().getUVip().getList());
        while (base.size() < VipType.COUNT) {
            base.add(0);
        }
        for (int packId : PRIVILEGE_PACK_IDS) {
            applyPackVipData(mUser, base, packId);
        }
        mUser.setEffectiveVipData(base);
        mUser.queueVipDataSync();
    }

    private static void applyPackVipData(MyUser mUser, List<Integer> eff, int packId) {
        UserPackEntity pack = mUser.getResources().getPack(packId);
        if (pack == null || pack.getNumber() <= 0 || !pack.hasHSD())
            return;
        ResPackEntity res = pack.getRes();
        if (res == null)
            return;
        String dataStr = res.getStringData();
        if (StringHelper.isEmpty(dataStr))
            return;
        List<Integer> pairs = GsonUtil.strToListInt(dataStr);
        if (pairs == null || pairs.isEmpty())
            return;
        for (int i = 0; i + 1 < pairs.size(); i += 2) {
            int type = pairs.get(i);
            int value = pairs.get(i + 1);
            if (type < 0 || type >= VipType.COUNT || value == 0)
                continue;
            eff.set(type, eff.get(type) + value);
        }
    }
}
