package game.dragonhero.mapping;

import game.battle.calculate.IMath;
import game.battle.object.HeroBattle;
import game.battle.object.Point;
import game.battle.object.Shuriken;
import game.battle.object.WeaponBattle;
import game.config.aEnum.EquipSlotType;
import game.dragonhero.mapping.main.ResHeroEntity;
import game.dragonhero.service.resource.ResHero;
import game.dragonhero.service.resource.ResItem;
import game.object.MyUser;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Data
@Entity
@Table(name = "user_hero")
@NoArgsConstructor
public class UserHeroEntity implements Serializable {
    @Id
    int heroId, userId;
    String skin, itemEquipment;
    @Transient
    Point point;

    public UserHeroEntity(int userId, int heroId) {
        this.heroId = heroId;
        this.userId = userId;
        this.skin = "[]";
        this.itemEquipment = NumberUtil.genListStringInt(ResItem.sizeItemEquipment, 0);
    }

    public List<Integer> getItemEquipment() {
        List<Integer> equipment = GsonUtil.strToListInt(itemEquipment);
        while (equipment.size() < ResItem.sizeItemEquipment) equipment.add(0);
        return equipment;
    }

    public long getPower() {
        if(point==null) return 0;
        return point.getPower();
    }

    public Point getPoint(MyUser mUser){
        if(point==null){
            point = calPointHero(mUser,IMath.calculatePoint(mUser, false));
        }
        return point;
    }

    public Point calPointHero(MyUser mUser, Point pointBase) {
        point = pointBase;
        List<Integer> itemIds = getListIdEquipmentEquip();
        IMath.calPointItemEquip(mUser, itemIds, point);
        return point;
    }

    public List<Integer> getSkins() {
        return GsonUtil.strToListInt(skin);
    }

    public void addSkin(int skinId) {
        List<Integer> skins = getSkins();
        skins.add(skinId);
    }

    public ResHeroEntity getRes() {
        return ResHero.getHero(heroId);
    }

    public boolean isEquip(int itemId) {
        return getListIdEquipmentEquip().contains(itemId);
    }

    public List<Integer> getListIdEquipmentEquip() { // only id
        List<Integer> lst = getItemEquipment();
        List<Integer> ret = new ArrayList<>();
        for (int i = 0; i < lst.size(); i += 3) {
            ret.add(lst.get(i));
        }
        return ret;
    }

    public Pbmethod.PbHero toProto() {
        Pbmethod.PbHero.Builder pb = Pbmethod.PbHero.newBuilder();
        pb.setHeroId(heroId);
        pb.addAllSkins(getSkins());
        pb.addAllItemEquipId(getListIdEquipmentEquip());
        return pb.build();
    }

    public HeroBattle toHeroBattle(int team, int id, int slot, WeaponBattle[] weaponBattles, UserPetEntity monster) {
        Point point = getPoint();
        if (point==null) point = new Point();
        return new HeroBattle(team, id, heroId, getPoint().cloneInstance(), slot, weaponBattles, monster);
    }

    public boolean updateItemEquip(List<Integer> items) {
        if (update(Arrays.asList("item_equipment", StringHelper.toDBString(items)))) {
            this.itemEquipment = items.toString();
            return true;
        }
        return false;
    }

    public boolean update(List<Object> updateData) {
        return DBJPA.update("user_hero", updateData, Arrays.asList("user_id", userId, "hero_id", heroId));
    }
}
