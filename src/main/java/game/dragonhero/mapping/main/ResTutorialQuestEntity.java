package game.dragonhero.mapping.main;


import game.config.aEnum.EquipSlotType;
import game.config.aEnum.QuestTutType;
import game.config.aEnum.RankType;
import game.config.lang.Lang;
import game.dragonhero.service.resource.*;
import game.object.MyUser;
import lombok.Getter;
import ozudo.base.helper.GsonUtil;
import ozudo.base.helper.Util;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Transient;
import java.util.ArrayList;
import java.util.List;

@Entity
public class ResTutorialQuestEntity extends BaseEntity {
    @Getter
    @Id
    private int id;
    @Getter
    private int num, gotoId;
    @Getter
    private String bonus;

    private String questType;
    @Transient
    List<Long> aBonus;
    @Transient
    List<Integer> quest;

    public void init() {
        aBonus = GsonUtil.strToListLong(bonus);
        quest = GsonUtil.strToListInt(questType);
        checkJson(id, bonus);
        checkJson(id, questType);
    }

    public String getTitle(MyUser mUser) {
        QuestTutType questTut = QuestTutType.get(quest.get(0));
        String title = Lang.getTitle(mUser, questTut.keyLang);
        switch (questTut) {
            case KILL_ENEMY, HAS_MONSTER -> {
                return String.format(title, ResEnemy.getEnemy(quest.get(1)).getName());
            }
            case PLAN_FARM, HARVEST -> {
                return String.format(title, Lang.getTitle(mUser,ResFarm.getItemFarm(quest.get(1)).getName()));
            }
            case HAS_COMBO_WEAPON -> {
                ResComboWeaponEntity rCombo = ResWeapon.mComboWeapon.get(quest.get(1) + 1);
                return String.format(title, Lang.getTitle(mUser,rCombo.getName()));
            }
            case HAS_WEAPON_ID -> {
                return String.format(title,Lang.getTitle(mUser, ResWeapon.getWeapon(quest.get(1)).getName()));
            }
            case ATTACK_BOSS_GOD -> {
                int type = quest.get(1);
                switch (type) {
                    case 1 -> {
                        return String.format(title, Lang.getTitle(mUser.getUser().getLang(), Lang.god_fire));
                    }
                    case 2 -> {
                        return String.format(title, Lang.getTitle(mUser.getUser().getLang(), Lang.god_water));
                    }
                    case 3 -> {
                        return String.format(title, Lang.getTitle(mUser.getUser().getLang(), Lang.god_flame));
                    }
                    case 4 -> {
                        return String.format(title, Lang.getTitle(mUser.getUser().getLang(), Lang.god_earth));
                    }
                }
            }
            case HAS_PET -> {
                return String.format(title, Lang.getTitle(mUser,ResPet.getPet(quest.get(1)).getName()));
            }
            case HAS_ITEM_EQUIP_ID, HAS_ITEM_EQUIP_LEVEL -> {
                return String.format(title, Lang.getTitle(mUser, ResItem.getItemEquipment(quest.get(1)).getName()), num);
            }
            case USE_ITEM -> {
                return String.format(title,Lang.getTitle(mUser, ResItem.getItem(quest.get(1)).getName()));
            }
            case HAS_WEAPON_BY_RANK -> {
                return String.format(title, num,Lang.getTitle(mUser, RankType.get(quest.get(1)).name));
            }
        }
        return String.format(title, num);
    }

    public QuestTutType getType() {
        return QuestTutType.get(quest.get(0));
    }

    public int getIdInfo() {
        return quest.get(1);
    }

    public List<Long> getABonus() {
        return new ArrayList<>(aBonus);
    }


}
