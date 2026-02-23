package game.dragonhero.mapping.main;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import game.battle.object.Point;
import game.battle.type.AttackType;
import game.battle.type.CharacterType;
import game.config.aEnum.FactionType;
import game.object.BonusConfig;
import lombok.Data;
import lombok.NoArgsConstructor;
import ozudo.base.helper.GsonUtil;
import ozudo.base.helper.NumberUtil;
import ozudo.base.helper.Util;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Transient;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@Entity
public class
ResEnemyEntity extends BaseEntity {
    @Id
    int id;
    String name, hp, hpRegen, attack, magicAttack, dataPet, dataEquip, bonus;
    int model, defense, magic_resist, crit, critDamage, agility, immunity, multiShot, bossType, rangeAttack, forcePush, moveSpeed;
    int weight, weapon, faction, rank, autoAttack;
    float attackSpeed, radius, delayAnimAttack,rangeView;
    @Transient
    long longHp, longHpRegen, longAttack, longMagicAttack;
    @Transient
    AttackType attackType;
    @Transient
    CharacterType characterType;
    @Transient
    Point point;
    @Transient
    FactionType factionType;
    @Transient
    List<BonusConfig> aBonus;

    public void init() {
        longHp = NumberUtil.castStr2Long(hp);
        longHpRegen = NumberUtil.castStr2Long(hpRegen);
        longAttack = NumberUtil.castStr2Long(attack);
        longMagicAttack = NumberUtil.castStr2Long(magicAttack);
        attackType = AttackType.get(getRangeAttack());
        characterType = CharacterType.MONSTER;
        factionType = FactionType.get(faction);
        // point
        point = new Point();
        point.setBaseHp(longHp);
        point.setCurHp(longHp);
        point.setBaseHpRegen(longHpRegen);
        point.setBaseAttack(longAttack);
        point.setBaseMagicAttack(longMagicAttack);
        point.setDefense(defense);
        point.setMagicResist(magic_resist);
        point.setBaseCritChange(crit);
        point.setCritDamage(critDamage);
        point.setAgility(agility);
        point.setImmunity(immunity);
        point.setBaseAttackSpeed((long) (100 / attackSpeed));
        //point.setMultiShot(multiShot);
        point.setMoveSpeed(moveSpeed);
        point.setWeight(weight);
        point.calculatorPower(1, 0);
        aBonus = new Gson().fromJson(bonus, new TypeToken<List<BonusConfig>>() {
        }.getType());
        checkJson(id, dataPet);
        checkJson(id, dataEquip);
    }

    public float getRangeAttack() {
        return rangeAttack / 10f;
    }

    public List<List<Long>> getDataPet() {
        if (dataPet != null && dataPet != "[]") return GsonUtil.strTo2ListLong(dataPet);
        return new ArrayList<>();
    }

    public List<List<Long>> getDataEquip() {
        if (dataEquip != null && dataEquip != "[]") return GsonUtil.strTo2ListLong(dataEquip);
        return new ArrayList<>();
    }


    public FactionType getFaction() {
        return FactionType.get(faction);
    }

    public Point toPoint() {
        return point.cloneInstance();
    }

}
