package game.dragonhero.table;

import game.battle.model.ChunkObject;
import game.battle.type.RoomState;
import game.dragonhero.mapping.main.ResMapEntity;

import java.util.Map;

public class HomeRoom extends BaseBattleRoom {
    public HomeRoom(ResMapEntity mapInfo, Map<Integer, ChunkObject> mChunk, String keyRoom) {
        super(mapInfo, mChunk, keyRoom);
    }

    @Override
    protected void startInit() {
        super.startInit();
        roomState = RoomState.ACTIVE;
    }
}
