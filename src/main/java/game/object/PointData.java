package game.object;

import ozudo.base.helper.GsonUtil;

import javax.persistence.Transient;
import java.util.List;

public class PointData {
    public int faction;
    public int clazz;
    String point; // [id - num x 100]

    public List<Long> getPoint() {
        return GsonUtil.strToListLong(point);
    }
}
