package game.dragonhero.service.resource;

import game.config.CfgServer;
import game.dragonhero.mapping.main.ResBossEntity;
import game.dragonhero.mapping.main.ResEnemyEntity;
import lombok.Getter;
import ozudo.base.database.DBResource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResEnemy {
    static Map<Integer, ResEnemyEntity> mEnemy = new HashMap<>();
    static List<ResEnemyEntity> aEnemy = new ArrayList<>();
    // boss
    static Map<Integer, ResBossEntity> mBoss = new HashMap<>();
    @Getter
    static ResBossEntity bossClan;
    static List<ResBossEntity> aBoss = new ArrayList<>();
    static final int MODEL_CLAN = 5;

    public static ResEnemyEntity getEnemy(int enemyId) {
        return mEnemy.get(enemyId);
    }

    public static ResBossEntity getBoss(int bossId) {
        return mBoss.get(bossId);
    }

    public static void init() {
        // monster
        aEnemy = DBResource.getInstance().getList(CfgServer.DB_MAIN + "res_enemy", ResEnemyEntity.class);
        mEnemy.clear();
        aEnemy.forEach(enemy -> {
            enemy.init();
            mEnemy.put(enemy.getId(), enemy);
        });
        // boss
        aBoss = DBResource.getInstance().getList(CfgServer.DB_MAIN + "res_boss", ResBossEntity.class);
        mBoss.clear();
        aBoss.forEach(boss -> {
            boss.init();
            mBoss.put(boss.getId(), boss);
            if (boss.getModel() == MODEL_CLAN) {
                bossClan = boss;
            }

        });
    }
}
