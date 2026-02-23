package game.dragonhero.service.resource;

import game.config.CfgServer;
import game.dragonhero.mapping.main.ResHeroEntity;
import game.dragonhero.mapping.main.ResItemEntity;
import ozudo.base.database.DBResource;

import java.util.*;

public class ResHero {
    static Map<Integer, ResHeroEntity> mHero = new HashMap<>();
    static List<ResHeroEntity> aHero = new ArrayList<>();


    public static ResHeroEntity getHero(int heroId) {
        return mHero.get(heroId);
    }

    public static void init() {
        // for hero
        aHero = DBResource.getInstance().getList(CfgServer.DB_MAIN + "res_hero", Arrays.asList("enable", 1), "", ResHeroEntity.class);
        mHero.clear();
        aHero.forEach(hero -> {
            mHero.put(hero.getHeroId(), hero);
        });
    }
}
