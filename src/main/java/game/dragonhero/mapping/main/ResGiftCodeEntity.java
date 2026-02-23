package game.dragonhero.mapping.main;


import game.config.aEnum.GiftCodeType;
import lombok.Data;
import lombok.NoArgsConstructor;
import ozudo.base.database.DBJPA;
import ozudo.base.helper.GsonUtil;
import ozudo.base.helper.StringHelper;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.io.Serializable;
import java.util.*;

@Entity
@NoArgsConstructor
@Data
@Table(name = "dson_main.res_gift_code")
public class ResGiftCodeEntity  implements Serializable {
    @Id
    String id;
    int type;
    int eventGift;
    String data;
    String bonus;
    Date timeEnd;

    public GiftCodeType getType() {
        return GiftCodeType.get(type);
    }

    public boolean expire() {
        return  Calendar.getInstance().getTime().after(timeEnd) ;
    }


    // =0 có thể nhận, =1 đã nhận
    public int getDataInt(){
        try {
          return Integer.parseInt(data);
        }catch (Exception e){
            return 0;
        }
    }

    public List<Integer> getListDataInt(){
        return   data == null ? new ArrayList<>() : GsonUtil.strToListInt(data);
    }

    public List<Long> getBonus() {
        return GsonUtil.strToListLong(bonus);
    }


    public boolean updateData(String data) {
        this.data = data;
        return DBJPA.update(this);
    }
}
