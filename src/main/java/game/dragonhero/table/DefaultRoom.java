package game.dragonhero.table;

import game.battle.model.Character;
import game.battle.type.RoomState;
import game.dragonhero.mapping.main.ResMapEntity;

import java.util.List;

public class DefaultRoom extends BaseBattleRoom {
    public DefaultRoom(ResMapEntity mapInfo, List<Character> aPlayer, String keyRoom) {
        super(mapInfo, aPlayer, keyRoom, false);
    }

    @Override
    protected void startInit() {
        super.startInit();
        roomState = RoomState.ACTIVE;
    }
}
