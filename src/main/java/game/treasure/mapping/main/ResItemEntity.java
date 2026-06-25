package game.treasure.mapping.main;

import protocol.Pbmethod;
import game.object.BonusConfig;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Transient;
import java.io.Serializable;
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
    int tier,  sellPrice;
    int type;
    @Getter
    @Transient
    List<BonusConfig> itemOpen;
    @Getter
    @Transient
    Pbmethod.ItemType itemType;


    /** user_item.tier cho consumable/event — lấy từ cột rank. */
    public int getTier() {
        return tier > 0 ? tier : 1;
    }

    public void init() {
        itemType = Pbmethod.ItemType.valueOf(type);
//        if (type == ItemType.ITEM_OPEN.value) {
//            try {
//                itemOpen = new Gson().fromJson(data, new TypeToken<List<BonusConfig>>() {
//                }.getType());
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
    }
}
