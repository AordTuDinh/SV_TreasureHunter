package game.dragonhero.mapping.main;


import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import game.battle.object.Pos;
import game.object.MapData;
import lombok.Getter;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Transient;
import java.io.Serializable;
import java.util.List;

@Entity
public class ResMapEntity extends BaseEntity implements Serializable {
    @Id
    @Getter
    int id;
    String  map,botLeft,topRight;
    @Transient
    @Getter
    List<Long> aMap, aBonus; // sub map
    @Transient
    @Getter
    MapData mapData;
    @Getter
    @Transient
    Pos botLeftP,topRightP;


    public void init() {
        if (map != null && !map.isEmpty()) {
            mapData = new Gson().fromJson(map, new TypeToken<MapData>() {
            }.getType());
            checkJson(id, map);
        }
        botLeftP = new Pos(botLeft);
        topRightP = new Pos(topRight);
    }
}
