package game.battle.object;

import com.google.gson.Gson;
import game.battle.calculate.IMath;
import game.battle.model.Character;
import game.dragonhero.mapping.UserWeaponEntity;
import game.object.PointBuff;
import game.protocol.CommonProto;
import lombok.Getter;
import lombok.Setter;
import ozudo.base.helper.ListUtil;
import protocol.Pbmethod;

import java.util.Arrays;
import java.util.List;

public class Point {
    // point int
    public static final int POWER = 0;
    public static final int CUR_HP = 1;
    public static final int CUR_MP = 2;
    public static final int ATTACK = 3;
    public static final int P_ATTACK = 4;
    public static final int ZEN_ATTACK = 5;
    public static final int MAGIC_ATTACK = 6;
    public static final int P_MAGIC_ATTACK = 7;
    public static final int ZEN_MAGIC_ATTACK = 8;
    public static final int ATTACK_SPEED = 9; // base
    public static final int P_ATTACK_SPEED = 49;// per add
    public static final int HP = 10;
    public static final int P_HP = 11;
    public static final int ZEN_HP = 12;
    public static final int HP_REGEN = 13; // hồi máu mỗi 1s
    public static final int P_HP_REGEN = 14; // hồi máu mỗi 1s
    public static final int MP = 15;
    public static final int P_MP = 16;
    public static final int MP_REGEN = 17; // hồi mana mỗi 1s
    public static final int P_MP_REGEN = 18; // hồi mana mỗi 1s
    public static final int MOVE_SPEED = 19;
    public static final int P_MOVE_SPEED = 20;
    public static final int DEFENSE = 21;
    public static final int P_DEFENSE = 22;
    public static final int MAGIC_RESIST = 23;
    public static final int P_MAGIC_RESIST = 24;
    public static final int CRIT = 25;
    public static final int CRIT_DAMAGE = 26;
    public static final int IMMUNITY = 27;
    public static final int AGILITY = 28;
    public static final int COOLDOWN = 29;
    public static final int ADAPTIVE_FORCE = 30;
    public static final int ACCURACY = 31;
    public static final int CRIT_DAMAGE_REDUCTION = 32;
    public static final int P_ITEM_DROP_INCREASE = 33;
    public static final int P_GOLD_CAMPAIGN_INCREASE = 34;
    public static final int P_EXP_CAMPAIGN_INCREASE = 35;
    public static final int ADDITION_DAMAGE = 36;
    public static final int RECEIVED_DAMAGE = 37;
    public static final int HEAL_EFFICIENCY = 38;
    public static final int HEAL_PER_KILL = 39;
    public static final int ADDITION_DAMAGE_TO_BOSS = 40;
    public static final int ADDITION_HEAL_EFFICIENCY = 41;
    public static final int BUFF_CUR_PER_HP = 42; // hồi phục % hp
    public static final int BUFF_CUR_PER_MP = 43; // hồi phục % hp
    public static final int WEIGHT = 44;
    public static final int CHANGE_MOVE_SPEED = 45; // % tốc độ di chuyển thay đổi
    public static final int CHANGE_ATTACK = 46;
    public static final int CHANGE_MAGIC_ATTACK = 47;
    public static final int DOGE = 48;
    public static final int CHANGE_DEFENSE = 50;
    public static final int CHANGE_MAGIC_RESIST = 51;
    public static final int CHANGE_AGILITY = 52;
    public static final int CHANGE_ATTACK_SPEED = 53; // tang thi - giam thi +
    public static final int CHANGE_HEATH = 54;// tăng, giảm khả năng hồi phục nhận vào
    public static final int SHELL = 55; // giáp ảo
    public static final int CHANGE_CRIT = 58;
    public static final int CHANGE_CRIT_DAMAGE = 59;

    // add dec -------------------------------

    public static final int BLOCK_PARALYZE = 60; // 0-1 : 1 block
    public static final int STUN = 61; // time ms be block
    public static final int FREEZE = 62; //time ms be đóng băng
    //
    public static final int CHANGE_DAME = 63; // tăng, giảm sát thương nhận vào
    public static final int TRUE_DAME = 64; // sát thương bỏ qua giáp và kháng phép


    @Setter
    @Getter
    private float startHpPercent = -1; // 0 - 10000
    public static int size = 100;
    private int BasePerZen = 100;

    long[] values;

    public Point() {
        values = new long[size];
        if (values.length < size) {
            long[] newValues = new long[size];
            for (int i = 0; i < values.length; i++) {
                newValues[i] = values[i];
            }
            values = newValues;
        }
        initDefault();
    }

    public void initDefault() {
        // set 1 số chỉ số mặc định
        values[CHANGE_MOVE_SPEED] = 100L;
        values[CHANGE_DEFENSE] = 100L;
        values[CHANGE_DAME] = 100L;
        values[CHANGE_MAGIC_RESIST] = 100L;
        values[CHANGE_AGILITY] = 100L;
        values[CHANGE_ATTACK_SPEED] = 100L;
        values[CHANGE_ATTACK] = 100L;
        values[CHANGE_MAGIC_ATTACK] = 100L;
        values[CHANGE_CRIT] = 100L;
        values[CHANGE_CRIT_DAMAGE] = 100L;
        values[CHANGE_HEATH] = 100L;
        values[BLOCK_PARALYZE] = 0L;
        values[STUN] = 0L;
        values[FREEZE] = 0L;
        values[TRUE_DAME] = 0L;
    }

    public Point(List<Long> data) {
        values = new long[size];
        initDefault();
        for (int i = 0; i < data.size(); i++) {
            values[i] = data.get(i);
        }
    }


    public void clear() {
        for (int i = 0; i < values.length; i++) values[i] = 0;
    }

    public void set(int index, long value) {
        if (value > 0) values[index] = value;
    }

    public void set(PointBuff point) {
        if (point.getValue() > 0) values[point.getPointId()] = point.getValue();
    }

    public void add(int index, long value) {
        values[index] += value;
        if (values[index] < 0) values[index] = 0;
        switch (index) {
            case CUR_HP:
                values[CUR_HP] = values[CUR_HP] > getMaxHp() ? getMaxHp() : values[CUR_HP];
                break;
            case CUR_MP:
                values[CUR_MP] = values[CUR_MP] > getMaxMp() ? getMaxMp() : values[CUR_MP];
                break;
        }
    }

    public void buffListPoint(List<Long> buffs) {
        for (int i = 0; i < buffs.size(); i += 2) {
            IMath.addPointData(this, Math.toIntExact(buffs.get(i)), (long) (buffs.get(i + 1) / 100f));
        }
    }


    public void setListPoint(List<Long> points) { // set  = value luôn chứ k phải buff
        for (int i = 0; i < points.size(); i += 2) {
            values[Math.toIntExact(points.get(i))] = (long) (points.get(i + 1) / 100f);
        }
    }

    public void addPoint(Point point) {
        for (int i = 0; i < point.values.length; i++) {
            if (values[i] == CRIT) {

            } else {
                values[i] += point.values[i];
            }
        }
    }

    public synchronized void add(PointBuff buff, Character character) {
        if(!character.isAlive()) return;
        switch (buff.getPointId()) {
            case CUR_HP -> {
                character.setAlive(addCurHp(buff.getValue()));
            }
            case CUR_MP -> values[CUR_MP] = Math.min(getCurMP() + buff.getValue(), getMaxMp());
//            case ADD_CUR_HP -> {
//                boolean alive = addCurHp((long) (getMaxHp() * buff.getValue() / 100f));
//                character.setAlive(alive);
//            }
//            case ADD_CUR_MP -> {
//                values[CUR_MP] = Math.min(getCurMP() + (long) (buff.getValue() * buff.getValue() / 100f), getMaxMp());
//            }
            default -> values[buff.getPointId()] += buff.getValue();
        }
        if (values[buff.getPointId()] < 0) values[buff.getPointId()] = 0;
    }

    //region get
    public long get(int index) {
        if (index >= values.length) return 0;
        return values[index];
    }

    public void addStun(long time) {
        long newStun = time + System.currentTimeMillis();
        long curStun = get(Point.STUN);
        if (curStun < newStun) { // cái nào stun lâu hơn thì chọn cái đó
            values[STUN] = newStun;
        }
    }

    public void setBaseHp(long value) {
        values[HP] = value;
    }


    public void addBaseHp(long value) {
        values[HP] += value;
    }

    public void addBaseAttack(long value) {
        values[ATTACK] += value;
    }

    public long setCurHp(long curHp) {
        return values[CUR_HP] = curHp;
    }


    public void resetHpMp() {
        initDefault();
        setCurHp(getMaxHp());
        setCurMp(getMaxMp());
    }

    public long setMaxCurHp() {
        return values[CUR_HP] = getMaxHp();
    }

    public long setCurMp(long curMp) {
        return values[CUR_MP] = curMp;
    }

    public long setMaxCurMp() {
        return values[CUR_MP] = getMaxMp();
    }

    public long forceDie() {
        return values[CUR_HP] = 0L;
    }


    public synchronized void addCurMp(long value) {
        values[CUR_MP] += value;
        values[CUR_MP] = values[CUR_MP] > getMaxMp() ? getMaxMp() : values[CUR_MP];
        values[CUR_MP] = values[CUR_MP] < 0 ? 0 : values[CUR_MP];
    }

    public void setBaseAttack(long value) {
        values[ATTACK] = value;
    }

    public void setWeight(long p_weight) {
        values[WEIGHT] = p_weight;
    }

    public void setBaseMagicAttack(long value) {
        values[MAGIC_ATTACK] = value;
    }

    public void addBaseMagicAttack(long value) {
        values[MAGIC_ATTACK] += value;
    }

    public void setDefense(long value) {
        values[DEFENSE] = value;
    }


    public void setMagicResist(long value) {
        values[MAGIC_RESIST] = value;
    }

    // cai nay phai chia 100
    public void setBaseCritChange(long value) {
        values[CRIT] = value;
    }

    public void setBaseAttackSpeed(long value) {
        values[ATTACK_SPEED] = value;
    }


    public void setCritDamage(long value) {
        values[CRIT_DAMAGE] = value;
    }


    public void setImmunity(long value) {
        values[IMMUNITY] = value;
    }

    public void setAgility(long value) {
        values[AGILITY] = value;
    }

    public void setShell(long value) {
        values[SHELL] = value;
    }

    public void setBaseHpRegen(long value) {
        values[HP_REGEN] = value;
    }

    // float => chia 100
    public void setMoveSpeed(long value) {
        values[MOVE_SPEED] = value;
    }

    public long getCrit() {
        long baseValue = values[CRIT];
        long changeValue = values[CHANGE_CRIT];
        return (long) ((baseValue) * (changeValue / 100f));
    }


    public long getCritDamage() {
        long baseValue = values[CRIT_DAMAGE];
        long changeValue = values[CHANGE_CRIT_DAMAGE];
        return (long) ((baseValue) * (changeValue / 100f));
    }

    public long getAttackDamage() {
        long baseAttack = values[ATTACK];
        long perAttack = values[P_ATTACK];
        long changeValue = values[CHANGE_ATTACK];
        return (long) (((baseAttack + baseAttack * perAttack / 100f) * (BasePerZen + values[ZEN_ATTACK]) / 100f) * (changeValue / 100f));
    }

    public long getDoge() { // né
        return values[DOGE];
    }

    public long getMagicDamage() {
        long baseValue = values[MAGIC_ATTACK];
        long perValue = values[P_MAGIC_ATTACK];
        long changeValue = values[CHANGE_MAGIC_ATTACK];
        return (long) (((baseValue + baseValue * perValue / 100f) * (BasePerZen + values[ZEN_MAGIC_ATTACK]) / 100f) * (changeValue / 100f));
    }

    public long getDameToBoss() {
        return values[ADDITION_DAMAGE_TO_BOSS];
    }

    public long getMagicResist() {
        long baseValue = values[MAGIC_RESIST];
        long perValue = values[P_MAGIC_RESIST];
        long changeValue = values[CHANGE_MAGIC_RESIST];
        return (long) ((baseValue + baseValue * perValue / 100f) * (changeValue / 100f));
    }

    public long getImmunity() {
        return values[IMMUNITY];
    }

    public long getAgility() {
        long baseValue = values[AGILITY];
//        System.out.println("baseValue = " + baseValue);
        long changeValue = values[CHANGE_AGILITY];
//        System.out.println("changeValue = " + changeValue);
//        System.out.println("baseValue * changeValue = " + baseValue * changeValue);
        return (baseValue * changeValue) / 100;
    }

    public long getCoolDown() {
        return values[COOLDOWN];
    }

    public long getMoveSpeed() {
        long baseValue = values[MOVE_SPEED];
        long perValue = values[P_MOVE_SPEED];
        long changeSpeed = values[CHANGE_MOVE_SPEED];
        return (long) ((baseValue + baseValue * perValue / 100f) * (changeSpeed / 100f));
    }

    public boolean equals(Point point) {
        return Arrays.equals(values, point.getValues());
    }

    // logic hơi phức tạp, buff <=100 thì buff thoải mái, dec >100 thì dec thoải mái.
    // trường hợp có dec mới thì phải so với dec cũ xem dec nào tốt hơn thì active, th buff cũ hết time thì áp buff mới vào luôn
    // thôi khó quá, trừ max 90% cho dễ =))
    public long buffChange(int changId, long buff, int maxChange) {
        if (buff > 0) { // add
            values[changId] += buff;
            return buff;
        } else { // dec
            int maxReduce = 100 - maxChange;
            if (values[changId] + buff < maxReduce) {
                long realBuff = values[changId] - maxReduce;
                values[changId] = maxReduce;
                return realBuff;
            } else {
                values[changId] += buff;
                return buff;
            }
        }
    }

    public long getAccuracy() {
        return values[ACCURACY];
    }

    public float getAttackSpeed() {
        float baseValue = values[ATTACK_SPEED] / 100f;
        float perValue = values[P_ATTACK_SPEED];
        long changeValue = values[CHANGE_ATTACK_SPEED];
        return 1 / ((baseValue + baseValue * perValue / 100f)) * (changeValue / 100f);
    }

    public int getBuffDrop() {
        return (int) (values[P_ITEM_DROP_INCREASE]);
    }

    public int getBuffGold() {
        return (int) (values[P_GOLD_CAMPAIGN_INCREASE]);
    }

    public int getBuffExp() {
        return (int) (values[P_EXP_CAMPAIGN_INCREASE]);
    }

    public void setTrueDame(boolean active) {
        values[TRUE_DAME] = active ? 1 : 0;
    }

    public long getDefense() {
        long baseValue = values[DEFENSE];
        long perValue = values[P_DEFENSE];
        long changeValue = values[CHANGE_DEFENSE];
        return (long) ((baseValue + baseValue * perValue / 100f) * (changeValue / 100f));
    }

    public float getChangeDame() {
        return values[CHANGE_DAME] / 100f;
    }


    public long getChangeAttackSpeed() {
        return values[CHANGE_ATTACK_SPEED];
    }

    public boolean isTrueDame() {
        return values[TRUE_DAME] == 1;
    }

    public synchronized long getCurMP() {
        return values[CUR_MP];
    }

    public long getWeight() {
        return values[WEIGHT];
    }

    public long getCritDamageReduce() {
        return values[CRIT_DAMAGE_REDUCTION];
    }
    //

    public Point cloneInstance() {
        Point point = new Point();
        for (int i = 0; i < size; i++) {
            point.values[i] = values[i];
        }
        return point;
    }

    public Point cloneOffset(float per) {
        Point point = new Point();
        for (int i = 0; i < size; i++) {
            point.values[i] = (long) (values[i] * per);
        }
        return point;
    }

    public synchronized long getCurHP() {
        return values[CUR_HP];
    }

    public int getPerHp() {
        return (int) (getCurHP() * 100 / getMaxHp());
    }

    public int getPerMp() {
        return (int) (getCurMP() * 100 / getMaxMp());
    }

    public long getMaxHp() { // max HP
        long baseValue = values[HP];
        long perValue = values[P_HP];
        return (long) ((baseValue + baseValue * perValue / 100f) * (BasePerZen + values[ZEN_HP]) / 100f);
    }

    public long getMaxMp() { // max MP
        long baseValue = values[MP];
        long perValue = values[P_MP];
        return (long) (baseValue + baseValue * perValue / 100f);
    }

    public long getHpRegen() {
        long baseValue = values[HP_REGEN];
        long perValue = values[P_HP_REGEN];
        return Math.round(baseValue + baseValue * perValue / 100f);
    }

    public long getCurStun() {
        return get(STUN);
    }

    public long getCurShell() {
        return get(SHELL);
    }

    public long getMpRegen() {
        long baseValue = values[MP_REGEN];
        long perValue = values[P_MP_REGEN];
        return Math.round(baseValue + baseValue * perValue / 100f);
    }

    public long getPower() {
        return values[POWER];
    }

    public void calculatorPower(int level, float perItemWeaponEquip) { // perItemWeaponEquip : hệ số atk
        long power = 0;
//        System.out.println("perItemWeaponEquip = " + perItemWeaponEquip);
        power += getAttackDamage() * 0.5f;
        power += getAttackDamage() * perItemWeaponEquip;
//        System.out.println("power1 = " + power);
        power += getMagicDamage() * 0.5f;
        power += getMagicDamage() * perItemWeaponEquip;
//        System.out.println("power2 = " + power);
        power += getMaxHp() * 0.5f;
//        System.out.println("power3 = " + power);
        power += getAttackSpeed() * 5f;
//        System.out.println("power4 = " + power);
        power += getHpRegen() * 2f;
//        System.out.println("power5 = " + power);
        power += getMaxMp() * 0.1f;
//        System.out.println("power6 = " + power);
        power += getMpRegen() * 2f;
//        System.out.println("power7 = " + power);
        power += getMoveSpeed() * 2f;
//        System.out.println("power8 = " + power);
        power += getDefense() * 2f;
//        System.out.println("power9 = " + power);
        power += getMagicResist() * 2f;
//        System.out.println("power10 = " + power);
        power += getCrit() * level * 0.02f;
//        System.out.println("power11 = " + power);
        power += getCritDamage() * 0.02f;
//        System.out.println("power12 = " + power);
        power += getAgility() * level * 0.02f;
//        System.out.println("power13 = " + power);
        power += getImmunity() * level * 0.02f;
//        System.out.println("power14 = " + power);
        power += getCoolDown() * level * 0.01f;
//        System.out.println("all power = " + power);
        values[POWER] = power;

    }

    public long[] getValues() {
        return values;
    }

    public boolean beBlock() {
        return values[STUN] > System.currentTimeMillis() || values[FREEZE] > System.currentTimeMillis() || values[Point.BLOCK_PARALYZE] > System.currentTimeMillis();
    }


    public String toString() {
        return new Gson().toJson(values);
    }

    public String toMiniString() {
        String ret = "Point: [";
        for (int i = 0; i < 36; i++) {
            ret += values[i] + ",";
        }
        ret = ret.substring(0, ret.length() - 1);
        return ret += "]";
    }

    public void copySpecialValue(Point point) {
        long[] newValues = point.getValues();
        int size = Math.min(newValues.length, values.length);
        for (int i = 0; i < size; i++) {
            if (values[i] < newValues[i]) values[i] = newValues[i];
        }
    }

    // tang giam chi so
    public synchronized boolean addCurHp(long value) {
        values[CUR_HP] += value * values[CHANGE_HEATH] / 100f; // nhân với giảm khả năng hồi phục
        values[CUR_HP] = values[CUR_HP] < 0 ? 0 : values[CUR_HP];
        values[CUR_HP] = Math.min(values[CUR_HP], getMaxHp());
        return values[CUR_HP] > 0; // true = alive
    }

    public void checkCurHp() {
        values[CUR_HP] = values[CUR_HP] > getMaxHp() ? getMaxHp() : values[CUR_HP];
    }


    public void addAttack(long value) {
        values[ATTACK] += value;
    }


    public void addMagicAttack(long value) {
        values[MAGIC_ATTACK] += value;
    }

    public void addPerAttack(long value) {
        values[P_ATTACK] += value;
    }

    public void addPerMagicAttack(long value) {
        values[P_MAGIC_ATTACK] += value;
    }

    public void addPerMoveSpeed(long value) {
        values[P_MAGIC_ATTACK] += value;
    }


    void addHp(long value) {
        values[HP] += value;
    }

    public void addPerHp(long value) {
        values[P_HP] += value;
    }

    public void addMp(long value) {
        values[MP] += value;
    }

    public void addPerMp(long value) {
        values[P_MP] += value;
    }

    public void addCrit(long value) {
        values[CRIT] += value;
    }

    public void addCritDamage(long value) {
        values[CRIT_DAMAGE] += value;
    }

    public void addDef(long value) {
        values[DEFENSE] += value;
    }

    public void addPerDef(long value) {
        values[P_DEFENSE] += value;
    }

    public void addMagicResist(long value) {
        values[MAGIC_RESIST] += value;
    }

    public void addPerMagicResist(long value) {
        values[P_MAGIC_RESIST] += value;
    }

    public void addHpRegen(long value) {
        values[HP_REGEN] += value;
    }

    public void addPerHpRegen(long value) {
        values[P_HP_REGEN] += value;
    }

    public void addMpRegen(long value) {
        values[MP_REGEN] += value;
    }

    public void addMoveSpeed(long value) {
        values[MOVE_SPEED] += value;
    }

    public void addPerMpRegen(long value) {
        values[P_MP_REGEN] += value;
    }

    public void addCoolDown(long value) {
        values[COOLDOWN] += value;
    }

    public void addImmunity(long value) {
        values[IMMUNITY] += value;
    }

    public void addAgility(long value) {
        values[AGILITY] += value;
    }

    public void addAttackSpeed(long value) {
        values[ATTACK_SPEED] += value;
    }

    public void addBattlePower(long value) {
        values[POWER] += value;
    }

    public void addZenAttack(long value) {
        values[ZEN_ATTACK] += value;
    }

    public void addZenMagicAttack(long value) {
        values[ZEN_MAGIC_ATTACK] += value;
    }


    public void buffPer(int per){
        addBattlePower(getPower()*per);
        addPerHp(per);
        addPerMp(per);
        addPerAttack(per);
        addPerMagicAttack(per);
        addPerMoveSpeed(per);
        addPerDef(per);
        addPerMagicResist(per);
        addCrit(per/10);
        addCritDamage(per);
        addImmunity(per);
        addAgility(per);
    }


    public List<Long> toProto() {
        List<Long> ret = ListUtil.arrToListLong(getValues());
        for (int i = 0; i < ret.size(); i++) {
            ret.set(i, ret.get(i));
        }
        return ret;
    }

    public Pbmethod.CommonVector toCommonVector() {
        return CommonProto.getCommonVector(toProto());
    }


}
