package game.dragonhero.mapping.main;


import game.dragonhero.mapping.UserEntity;
import lombok.Data;
import lombok.NoArgsConstructor;
import ozudo.base.helper.GsonUtil;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Transient;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@Entity
@NoArgsConstructor
public class ResEventTopEntity {
    @Id
    int eventType;
    Date dateStart,dateEnd;
    String buttonName,buttonPart,   partBanner, titlePanel,descPanel, listBonus,serverIds;

    @Transient
    List<List<Long>> dataBonus;


    public  List<List<Long>> getDataBonus(){
        return new ArrayList<>(dataBonus);
    }

    public boolean inEvent(UserEntity user){
        return  getServerIds().contains(user.getServer());
    }

    public  List<Integer> getServerIds(){
       return GsonUtil.strToListInt(serverIds);
    }

    public void init(){
        dataBonus = GsonUtil.strTo2ListLong(listBonus);
    }

}
