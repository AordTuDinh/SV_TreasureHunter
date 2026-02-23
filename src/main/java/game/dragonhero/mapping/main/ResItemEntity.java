package game.dragonhero.mapping.main;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import game.config.aEnum.ItemType;
import game.monitor.Telegram;
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
    String name;
    String desc; //pointBuff : point,value,time
    @Getter
    String preview, pointBuff, data;
    @Getter
    int rank, enable, sellPrice, showBag;
    int type;
    @Getter
    @Transient
    List<PointBuff> buffs;
    @Getter
    @Transient
    List<BonusConfig> itemOpen;
    @Getter
    @Transient
    ItemType itemType;

    public boolean isBuff() {
        return !buffs.isEmpty();
    }


    public void init() {
        buffs = new ArrayList<>();
        itemType = ItemType.get(type);
        List<Long> buff = GsonUtil.strToListLong(getPointBuff());
        for (int i = 0; i < buff.size(); i += 3) {
            buffs.add(new PointBuff(buff.get(i), buff.get(i + 1), buff.get(i + 2)));
        }
        checkJson(id, data);
        checkJson(id, pointBuff);
        checkJson(id, preview);
        if (type == ItemType.ITEM_OPEN.value) {
            try {
                itemOpen = new Gson().fromJson(data, new TypeToken<List<BonusConfig>>() {
                }.getType());
            } catch (Exception e) {
                e.printStackTrace();
//                Telegram.sendNotify("ERROR PARSE res_item " + id + " -> " + e.getMessage());
            }
        }
    }
}
