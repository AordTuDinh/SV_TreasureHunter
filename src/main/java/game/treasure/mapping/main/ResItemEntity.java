package game.treasure.mapping.main;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import game.config.aEnum.ItemType;
import game.object.BonusConfig;
import game.object.PointBuff;
import lombok.Getter;
import lombok.NoArgsConstructor;
import ozudo.base.helper.GsonUtil;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Transient;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@Entity
public class ResItemEntity extends BaseEntity implements Serializable {
    @Getter
    @Id
    int id;
    @Getter
    String name,data;
    @Getter
    int rank,  sellPrice;
    int type;
    @Getter
    @Transient
    List<BonusConfig> itemOpen;
    @Getter
    @Transient
    ItemType itemType;


    public void init() {
        itemType = ItemType.get(type);
        if (type == ItemType.ITEM_OPEN.value) {
            try {
                itemOpen = new Gson().fromJson(data, new TypeToken<List<BonusConfig>>() {
                }.getType());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
