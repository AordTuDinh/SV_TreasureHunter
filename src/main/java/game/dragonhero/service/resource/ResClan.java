package game.dragonhero.service.resource;

import game.config.CfgServer;
import game.dragonhero.mapping.main.ResClanSkillEntity;
import game.dragonhero.mapping.main.ResContributeEntity;
import game.dragonhero.mapping.main.ResDynamicTypeEntity;
import ozudo.base.database.DBResource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResClan {
    public static final int INDEX_STAR = 0;
    public static final int INDEX_TIME_DONE = 1;
    public static final int INDEX_TIME = 2;
    public static final int INDEX_STATUS = 3;
    public static final int INDEX_BONUS = 4;
    static Map<Integer, ResClanSkillEntity> mClanSkill = new HashMap<>();
    static List<ResClanSkillEntity> aClanSkill = new ArrayList<>();
    public static List<ResDynamicTypeEntity> aDynamicType = new ArrayList<>();
    public static Map<Integer, ResDynamicTypeEntity> mDynamicType = new HashMap<>();
    static Map<Integer, ResContributeEntity> mClanContribute = new HashMap<>();
    public static int maxClanSkill;
    public static int maxLevelContribute;

    public static ResClanSkillEntity getClanSkill(int skillId) {
        return mClanSkill.get(skillId);
    }

    public static ResContributeEntity getClanContribute(int level) {
        return mClanContribute.get(level);
    }

    public static int getIndexClass(int skillId) {
        if (skillId <= 10) return 0;
        else if (skillId <= 20) return 1;
        else return 2;
    }


    public static void init() {
        aClanSkill = DBResource.getInstance().getList(CfgServer.DB_MAIN + "res_clan_skill", ResClanSkillEntity.class);
        mClanSkill.clear();
        aClanSkill.forEach(skill -> {
            skill.init();
            mClanSkill.put(skill.getId(), skill);
        });
        maxClanSkill = aClanSkill.size();
        // clan contribute
        List<ResContributeEntity> aClanCon = DBResource.getInstance().getList(CfgServer.DB_MAIN + "res_contribute", ResContributeEntity.class);
        mClanContribute.clear();
        aClanCon.forEach(con -> mClanContribute.put(con.getLevel(), con));
        maxLevelContribute = aClanCon.size();
        aDynamicType = DBResource.getInstance().getList(CfgServer.DB_MAIN + "res_dynamic_type", ResDynamicTypeEntity.class);
        mDynamicType.clear();
        aDynamicType.forEach(type -> mDynamicType.put(type.getType(), type));
    }
}
