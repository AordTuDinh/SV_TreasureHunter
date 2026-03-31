package game.dragonhero.mapping.main;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import game.config.aEnum.FactionType;
import lombok.Data;
import lombok.NoArgsConstructor;
import ozudo.base.helper.GsonUtil;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Transient;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@Entity
public class ResPetEntity implements Serializable {
    @Id
    int id;
    String name, desc, data, skill, bonusFaction;
    int rank, faction, showSummon; // showSummon: có thể summon ra
    float timeActive;
    @Transient
    FactionType factionType;

    public List<List<Long>> getData() {
        return GsonUtil.strTo2ListLong(data);
    }


    public List<Long> getBonusFaction() {
        return GsonUtil.strToListLong(bonusFaction);
    }

    public void init() {
        factionType = FactionType.get(faction);
    }
}
