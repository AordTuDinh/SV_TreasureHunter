package game.dragonhero.table;

import game.battle.model.BossGod;
import game.battle.model.Character;
import game.battle.model.KimThan;
import game.battle.model.Player;
import game.dragonhero.mapping.main.BaseMap;
import game.dragonhero.mapping.main.ResBossEntity;
import game.dragonhero.service.resource.ResEnemy;

import java.util.List;

public class KimThanRoom extends BossGodRoom {

    public KimThanRoom(BaseMap mapInfo, List<Character> aPlayer, String keyRoom, int mode) {
        super(mapInfo, aPlayer, keyRoom, mode);
    }

    @Override
    protected BossGod bossData() {
        ResBossEntity bossData = ResEnemy.getBoss(Math.toIntExact(getBossId()));
        return new KimThan(bossData, bossData.getInstancePos(), team, this);
    }
}
