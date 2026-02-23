package game.battle.object;

import game.object.MapData;
import lombok.Data;

import java.io.Serializable;

@Data
public class PanelMap implements Serializable {
    public Pos botLeft;
    public Pos topRight;

    public PanelMap(MapData map) {
        this.botLeft = map.getBotLeft().clone();
        this.topRight = map.getTopRight().clone();
    }

    public PanelMap(Pos botLeft, Pos topRight) {
        this.botLeft = botLeft.clone();
        this.topRight = topRight.clone();
    }

    public PanelMap Clone() {
        return new PanelMap(botLeft.clone(), topRight.clone());
    }
}
