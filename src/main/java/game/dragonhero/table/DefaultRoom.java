package game.dragonhero.table;

import game.battle.model.Character;
import game.battle.type.RoomState;
import game.dragonhero.mapping.main.BaseMap;

import java.util.List;

public class DefaultRoom extends BaseBattleRoom {
    public DefaultRoom(BaseMap mapInfo, List<Character> aPlayer, String keyRoom) {
        super(mapInfo, aPlayer, keyRoom, false);
    }

    @Override
    protected void startInit() {
        super.startInit();
        roomState = RoomState.ACTIVE;
    }

    @Override
    public void EffectUpdate() {
        super.EffectUpdate();
//        if (aPlayer.get(0).getId() == 201551) {
//            System.out.println("aPlayer.get(0).getPoint().getCurHP() = " + aPlayer.get(0).getPoint().getCurHP());
//        }
    }
}
