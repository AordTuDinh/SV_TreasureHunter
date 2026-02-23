package game.dragonhero.service.resource;

import game.config.CfgServer;
import game.dragonhero.mapping.main.ResSkillEntity;
import game.dragonhero.service.battle.EffectType;
import ozudo.base.database.DBResource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResSkill {
    static Map<Integer, ResSkillEntity> mSkill = new HashMap<>();
    static List<ResSkillEntity> aSkill = new ArrayList<>();

    public static ResSkillEntity getSkills(Integer id) {
        return mSkill.get(id);
    }

    public static void init() {
        // map
        aSkill = DBResource.getInstance().getList(CfgServer.DB_MAIN + "res_skill", ResSkillEntity.class);
        mSkill.clear();
        aSkill.forEach(item -> {
            item.initData();
            mSkill.put(item.getId(), item);
        });
    }
}
