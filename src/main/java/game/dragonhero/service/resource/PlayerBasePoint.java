package game.dragonhero.service.resource;

import game.battle.object.Point;
import game.config.CfgServer;
import game.object.PointBuff;
import ozudo.base.database.DBResource;

import java.util.ArrayList;
import java.util.List;

public class PlayerBasePoint {
    static List<PointBuff> aPoint = new ArrayList<>();

    public static void init() {
        aPoint = DBResource.getInstance().getList(CfgServer.DB_MAIN + "player_base_point", PointBuff.class);
    }

    public static Point getBase() {
        Point point = new Point();
        for (int i = 0; i < aPoint.size(); i++) {
            point.set(aPoint.get(i));
        }
        return point;
    }
}
