package game.object;

import game.battle.object.Pos;

import java.util.List;

public class Teleport {
    public int type;
    public int roomType;
    public int mapId;
    public List<Float> pos;

    public Pos GetPos() {
        return new Pos(pos.get(0), pos.get(1));
    }
}
