package game.dragonhero.mapping;


import game.config.CfgFarm;
import game.dragonhero.service.resource.ResFarm;
import lombok.Data;
import lombok.NoArgsConstructor;
import ozudo.base.database.DBJPA;
import ozudo.base.helper.GsonUtil;
import ozudo.base.helper.NumberUtil;
import ozudo.base.helper.StringHelper;
import protocol.Pbmethod;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

@Data
@Entity
@NoArgsConstructor
@Table(name = "user_land")
public class UserLandEntity implements Serializable {
    @Id
    int userId, id;
    String info; //id cây, thời gian trồng, thời gian thu hoạch,tưới nước, bón phân
    @Transient
    List<Long> cacheBonus;
    @Transient
    List<Integer> cacheInfo;

    public UserLandEntity(int userId, int id) {
        this.userId = userId;
        this.id = id;
        this.info = NumberUtil.genListStringInt(CfgFarm.MaxSizeInfoFarm, 0);
    }

    public List<Integer> getInfo() {
        List<Integer> infos = GsonUtil.strToListInt(info);
        while (infos.size() < CfgFarm.MaxSizeInfoFarm) infos.add(0);
        return GsonUtil.strToListInt(info);
    }

    public String getInfoString() {
        return info;
    }

    public Pbmethod.PbLand.Builder toProto() {
        List<Long> data = GsonUtil.strToListLong(info);
        long curSeconds = Calendar.getInstance().getTime().getTime() / 1000;
        Pbmethod.PbLand.Builder pb = Pbmethod.PbLand.newBuilder();
        pb.setLandId(id);
        int idTree = Math.toIntExact(data.get(CfgFarm.ID_TREE));
        pb.setTreeId(idTree);
        pb.setTimePlant(data.get(CfgFarm.TIME_PLANT));
        long timeHarvest =data.get(CfgFarm.TIME_HARVEST) -  System.currentTimeMillis()/1000;
        pb.setTimeHarvest(timeHarvest<0?0:timeHarvest);
        pb.setHasWater(Math.toIntExact(data.get(CfgFarm.WATER)));
        pb.setFertilize(data.get(CfgFarm.FERTILIZE) == 0L && curSeconds < data.get(CfgFarm.TIME_HARVEST) ? 0 : 1);
        int ferTime = data.get(CfgFarm.FER_TIME) == 0L && curSeconds < data.get(CfgFarm.TIME_HARVEST) ? 0 : 1;
        pb.setFerTime(ferTime);
//        System.out.println("Proto : " + GsonUtil.toJson(pb));
        return pb;
    }

    public boolean updateInfo(List<Integer> infos) {
        if (update(Arrays.asList("info", StringHelper.toDBString(infos)))) {
            this.info = infos.toString();
            return true;
        }
        return false;
    }

    public boolean update(List<Object> data) {
        return DBJPA.update("user_land", data, Arrays.asList("user_id", userId, "id", id));
    }
}
