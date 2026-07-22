package game.treasure.mapping.main;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import lombok.Getter;
import lombok.NoArgsConstructor;
import protocol.Pbmethod;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@Entity
@Table(name = "res_item_point")
public class ResItemPointEntity implements Serializable {
    @Getter
    @Id
    int pointId;
    @Getter
    String name;
    @Getter
    String desc;
    /** JSON list materialId — dùng khi type = OPEN_BOX. */
    @Getter
    String data;
    @Getter
    int tier;
    @Getter
    int type, sellPrice;
    /** 0 = ẩn túi; 1 = hiện. */
    @Getter
    int showBag;

    @Getter
    @Transient
    Pbmethod.ItemPointType itemPointType;

    @Getter
    @Transient
    List<Integer> materialIds = new ArrayList<>();

    public void init() {
        itemPointType = type > 0 ? Pbmethod.ItemPointType.valueOf(type) : null;
        materialIds = parseMaterialIds(data);
    }

    static List<Integer> parseMaterialIds(String raw) {
        List<Integer> ids = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            return ids;
        }
        try {
            JsonArray arr = JsonParser.parseString(raw.trim()).getAsJsonArray();
            for (JsonElement el : arr) {
                if (el == null || el.isJsonNull()) {
                    continue;
                }
                int id = el.getAsInt();
                if (id > 0) {
                    ids.add(id);
                }
            }
        } catch (Exception ignored) {
        }
        return ids;
    }
}
