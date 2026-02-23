package game.dragonhero.mapping.main;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import game.object.MapData;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Transient;
import java.io.Serializable;
import java.util.List;


@Entity
@NoArgsConstructor
public class BaseMap extends BaseEntity implements Serializable {
    @Id
    @Getter
    int id;
    String name, map;
    @Transient
    @Getter
    List<Long> aMap, aBonus; // sub map
    @Transient
    @Getter
    MapData mapData;


    public void init() {
        if (map != null && !map.isEmpty()) {
            mapData = new Gson().fromJson(map, new TypeToken<MapData>() {
            }.getType());
            checkJson(id, map);
//            if (mapData.getGeos() != null) {
//                mapData.getGeos().forEach(geometry -> {
//                    if (geometry.getType() == GeometryType.Circle) {
//                        if (geometry.getRadius() == 0) {
//                            System.out.println("----->> SAI radius column DATA in res_map ID = " + id);
//                        }
//                    } else if (geometry.getType() == GeometryType.Triangle) {
//                        if (geometry.getPos().size() != 3) {
//                            System.out.println("----->> SAI num point column  DATA in res_map ID = " + id);
//                        }
//                    }
//                });
//            } else System.out.println("Map data is null -> mapId =" + id);
        }
    }
}

