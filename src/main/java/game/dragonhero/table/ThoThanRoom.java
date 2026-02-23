package game.dragonhero.table;

import game.battle.model.BossGod;
import game.battle.model.Character;
import game.battle.model.ThoThan;
import game.battle.object.Pos;
import game.dragonhero.mapping.main.BaseMap;
import game.dragonhero.mapping.main.ResBossEntity;
import game.dragonhero.service.resource.ResEnemy;

import java.util.List;

public class ThoThanRoom extends BossGodRoom {
    public ThoThanRoom(BaseMap mapInfo, List<Character> aPlayer, String keyRoom, int mode) {
        super(mapInfo, aPlayer, keyRoom, mode);
    }

    @Override
    protected BossGod bossData() {
        ResBossEntity bossData = ResEnemy.getBoss(Math.toIntExact(getBossId()));
        return new ThoThan(bossData, bossData.getInstancePos(), team, this);
    }
}
