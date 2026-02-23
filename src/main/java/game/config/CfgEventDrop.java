package game.config;

import com.google.gson.Gson;
import game.config.aEnum.PackType;
import game.config.aEnum.StatusType;
import game.config.aEnum.TriggerEventTimer;
import game.dragonhero.mapping.UserDataEntity;
import game.dragonhero.mapping.UserEntity;
import game.dragonhero.mapping.UserEventCloEntity;
import game.dragonhero.mapping.UserEventEntity;
import game.dragonhero.mapping.main.ResEventClockEntity;
import game.dragonhero.server.IAction;
import game.dragonhero.service.Services;
import game.dragonhero.service.resource.ResEvent;
import game.dragonhero.service.user.Bonus;
import game.object.DataDaily;
import game.object.MyUser;
import game.protocol.CommonProto;
import lombok.Data;
import ozudo.base.database.DBJPA;
import ozudo.base.helper.DateTime;
import ozudo.base.helper.NumberUtil;
import ozudo.base.helper.Util;
import protocol.Pbmethod;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

public class CfgEventDrop {

    public static DataConfig config;

    public static boolean inEvent() {
        return config.active;
    }

    public static List<Long> bonusDrop(int per, int num) {
        if (!inEvent()) return new ArrayList<>();
        List<Long> bonus = new ArrayList<>();
        for (int i = 0; i < num; i++) {
            int perx = NumberUtil.getRandom(1000);
            if (perx < per) bonus.addAll(Bonus.viewItem(config.itemId, 1));
        }
        return bonus;
    }


    public static void loadConfig(String strJson) {
        config = new Gson().fromJson(strJson, DataConfig.class);
    }

    @Data
    public class DataConfig {
        boolean active;
        int eventId;
        int itemId; // const item id
        int rateDropCampaign;
        int rateDropTree;
        int rateDropBossGod;
        int rateDropTower;
    }
}
