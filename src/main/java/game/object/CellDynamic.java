package game.object;

import java.io.Serializable;

public class CellDynamic implements Serializable {
    public int id;
    public String name;
    public int type;

    public CellDynamic(int id, String name, int type) {
        this.id = id;
        this.name = name;
        this.type = type;
    }
}
