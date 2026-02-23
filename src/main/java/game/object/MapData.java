package game.object;

import game.battle.object.Pos;

import java.io.Serializable;
import java.util.List;

public class MapData implements Serializable {
    int collider;// 0 : battle , 1 normal
    List<Float> map;
    //    List<Geometry> geos;
    List<Integer> teleports;

    public Pos getBotLeft() {
        return new Pos(map.get(0), map.get(1));
    }

    public Pos getBotRight() {
        return new Pos(map.get(2), map.get(1));
    }

    public Pos getTopLeft() {
        return new Pos(map.get(0), map.get(3));
    }

    public Pos getTopCenter() {
        return new Pos(0, map.get(3));
    }

    public Pos getBotCenter() {
        return new Pos(0, map.get(1));
    }

//    public List<Geometry> getGeos() {
//        return geos;
//    }

    public Pos getTopRight() {
        return new Pos(map.get(2), map.get(3));
    }

    public int getPlayerCollider() {
        return collider;
    }

    public static void main(String[] args) {
        System.out.println(System.currentTimeMillis());
    }
}