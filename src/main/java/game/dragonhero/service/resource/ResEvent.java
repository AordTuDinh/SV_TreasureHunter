package game.dragonhero.service.resource;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import game.config.CfgServer;
import game.config.aEnum.PackType;
import game.config.aEnum.TriggerEventTimer;
import game.dragonhero.mapping.main.ResEventClockEntity;
import game.dragonhero.mapping.main.ResPackEntity;
import game.dragonhero.mapping.main.ResVipEntity;
import ozudo.base.database.DBResource;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class ResEvent {

    static Map<Integer, ResPackEntity> mResPack = new HashMap<>();
    static Map<Integer, ResVipEntity> mVip = new HashMap<>();
    public static Map<Integer, ResEventClockEntity> mEventTimer = new HashMap<>();

    public static int lengthVip = 0;

    public static ResPackEntity getResPack(int id) {
        return mResPack.get(id);
    }

    public static ResEventClockEntity getResEventTimer(int id) {
        return mEventTimer.get(id);
    }

    public static ResPackEntity getResPack(PackType packType) {
        return mResPack.get(packType.value);
    }

    public static ResVipEntity getResVip(int vip) {
        return mVip.get(vip);
    }

    public static void init() {
        // packs list
        List<ResPackEntity> aPacks = DBResource.getInstance().getList(CfgServer.DB_MAIN + "res_pack", ResPackEntity.class);
        mResPack.clear();
        aPacks.forEach(item -> mResPack.put(item.getId(), item));
        // event timer
        List<ResEventClockEntity> eTimer = DBResource.getInstance().getList(CfgServer.DB_MAIN + "res_event_clock", ResEventClockEntity.class);
        mEventTimer.clear();
        eTimer.forEach(e -> {
            e.init();
            if (e.getTriggerType() == TriggerEventTimer.TIME) {
                if (e.inEvent()) mEventTimer.put(e.getId(), e);
            } else mEventTimer.put(e.getId(), e);
        });
        //vip
        List<ResVipEntity> aVips = DBResource.getInstance().getList(CfgServer.DB_MAIN + "res_vip", ResVipEntity.class);
        lengthVip = aVips.size();
        mVip.clear();
        aVips.forEach(item -> {
            item.init();
            mVip.put(item.getVip(), item);
        });

    }
}
