package game.dragonhero.table;

import game.battle.model.BossGod;
import game.battle.model.Character;
import game.battle.model.HoaThan;
import game.battle.model.Support;
import game.battle.object.Point;
import game.battle.object.Pos;
import game.dragonhero.mapping.main.BaseMap;
import game.dragonhero.mapping.main.ResBossEntity;
import game.dragonhero.service.resource.ResEnemy;

import java.util.List;

public class HoaThanRoom extends BossGodRoom {

    public HoaThanRoom(BaseMap mapInfo, List<Character> aPlayer, String keyRoom, int mode) {
        super(mapInfo, aPlayer, keyRoom, mode);
        aPlayer.get(0).setPos(new Pos(0, -3));
    }

    @Override
    protected BossGod bossData() {
        ResBossEntity bossData = ResEnemy.getBoss(Math.toIntExact(getBossId()));
        return new HoaThan(bossData, bossData.getInstancePos(), team, this);
    }

    @Override
    public void addSupport(Support sp) {
        super.addSupport(sp);
        getBoss().getPoint().add(Point.ATTACK, 1000L);
    }


    @Override
    public void removeSupport(Support sp) {
        super.removeSupport(sp);
        getBoss().getPoint().add(Point.ATTACK, -1000L);
    }
}
