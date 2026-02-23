package game.object;

import game.battle.type.GeometryType;
import game.battle.object.Pos;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


public class Geometry implements Serializable {
    int type;
    int inSize;
    List<Float> point;
    List<Float> center;
    float radius;


    public GeometryType getType() {
        return GeometryType.get(type);
    }

    public boolean isInSize() { // 1 int 0 out
        return inSize == 1;
    }

    public List<Pos> getPos() {
        List<Pos> posList = new ArrayList<>();
        for (int i = 0; i < point.size(); i += 2) {
            posList.add(new Pos(point.get(i), point.get(i + 1)));
        }
        return posList;
    }

    public Pos getCenter() {
        return new Pos(center.get(0), center.get(1));
    }

    public float getRadius() {
        return radius;
    }
}
