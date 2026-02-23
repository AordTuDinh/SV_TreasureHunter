package game.object;

import java.io.Serializable;

public class EquipmentPoint implements Serializable {
    public  int id;
    public  int point;
    public  int addValue;

    public EquipmentPoint(int id, int point, int addValue) {
        this.id = id;
        this.point = point;
        this.addValue = addValue;
    }
}
