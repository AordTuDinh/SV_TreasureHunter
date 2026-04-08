package game.battle.object;

import com.google.gson.Gson;
import game.battle.calculate.IMath;
import game.battle.model.Unit;
import game.object.PointBuff;
import game.protocol.CommonProto;
import lombok.Getter;
import lombok.Setter;
import protocol.Pbmethod;

import java.util.Arrays;
import java.util.ArrayList;
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

    int[] values;

    public Point() {
        values = new int[size];
        if (values.length < size) {
            int[] newValues = new int[size];
            for (int i = 0; i < values.length; i++) {
                newValues[i] = values[i];
            }
            values = newValues;
        }
        initDefault();
    }

    public void initDefault() {
        // set 1 số chỉ số mặc định
        values[CHANGE_MOVE_SPEED] = 100;
        values[CHANGE_DEFENSE] = 100;
        values[CHANGE_DAME] = 100;
        values[CHANGE_MAGIC_RESIST] = 100;
        values[CHANGE_AGILITY] = 100;
        values[CHANGE_ATTACK_SPEED] = 100;
        values[CHANGE_ATTACK] = 100;
        values[CHANGE_MAGIC_ATTACK] = 100;
        values[CHANGE_CRIT] = 100;
        values[CHANGE_CRIT_DAMAGE] = 100;
        values[CHANGE_HEATH] = 100;
        values[BLOCK_PARALYZE] = 0;
        values[STUN] = 0;
        values[FREEZE] = 0;
        values[TRUE_DAME] = 0;
    }

    public Point(List<Integer> data) {
        values = new int[size];
        initDefault();
        for (int i = 0; i < data.size(); i++) {
            values[i] = data.get(i);
        }
    }


    public void clear() {
        for (int i = 0; i < values.length; i++) values[i] = 0;
    }

    public void set(int index, Integer value) {
        if (value > 0) values[index] = value;
    }

    public void set(PointBuff point) {
        if (point.getValue() > 0) values[point.getPointId()] = point.getValue();
    }

    public void add(int index, int value) {
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

    public void buffListPoint(List<Integer> buffs) {
        for (int i = 0; i < buffs.size(); i += 2) {
            IMath.addPointData(this, buffs.get(i), (int) (buffs.get(i + 1) / 100f));
        }
    }


    public void setListPoint(List<Integer> points) { // set  = value luôn chứ k phải buff
        for (int i = 0; i < points.size(); i += 2) {
            values[points.get(i)] = (int) (points.get(i + 1) / 100f);
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

    public synchronized void add(PointBuff buff, Unit unit) {
        if(!unit.isAlive()) return;
        switch (buff.getPointId()) {
            case CUR_HP -> {
                unit.setAlive(addCurHp(buff.getValue()));
            }
            case CUR_MP -> values[CUR_MP] = Math.min(getCurMP() + buff.getValue(), getMaxMp());
//            case ADD_CUR_HP -> {
//                boolean alive = addCurHp((int) (getMaxHp() * buff.getValue() / 100f));
//                character.setAlive(alive);
//            }
//            case ADD_CUR_MP -> {
//                values[CUR_MP] = Math.min(getCurMP() + (int) (buff.getValue() * buff.getValue() / 100f), getMaxMp());
//            }
            default -> values[buff.getPointId()] += buff.getValue();
        }
        if (values[buff.getPointId()] < 0) values[buff.getPointId()] = 0;
    }

    //region get
    public int get(int index) {
        if (index >= values.length) return 0;
        return values[index];
    }

    public void addStun(int time) {
        int newStun = time + (int) System.currentTimeMillis();
        int curStun = get(Point.STUN);
        if (curStun < newStun) { // cái nào stun lâu hơn thì chọn cái đó
            values[STUN] = newStun;
        }
    }

    public void setBaseHp(int value) {
        values[HP] = value;
    }


    public void addBaseHp(int value) {
        values[HP] += value;
    }

    public void addBaseAttack(int value) {
        values[ATTACK] += value;
    }

    public int setCurHp(int curHp) {
        return values[CUR_HP] = curHp;
    }


    public void resetHpMp() {
        initDefault();
        setCurHp(getMaxHp());
        setCurMp(getMaxMp());
    }

    public int setMaxCurHp() {
        return values[CUR_HP] = getMaxHp();
    }

    public int setCurMp(int curMp) {
        return values[CUR_MP] = curMp;
    }

    public int setMaxCurMp() {
        return values[CUR_MP] = getMaxMp();
    }

    public int forceDie() {
        return values[CUR_HP] = 0;
    }


    public synchronized void addCurMp(int value) {
        values[CUR_MP] += value;
        values[CUR_MP] = values[CUR_MP] > getMaxMp() ? getMaxMp() : values[CUR_MP];
        values[CUR_MP] = values[CUR_MP] < 0 ? 0 : values[CUR_MP];
    }

    public void setBaseAttack(int value) {
        values[ATTACK] = value;
    }

    public void setWeight(int p_weight) {
        values[WEIGHT] = p_weight;
    }

    public void setBaseMagicAttack(int value) {
        values[MAGIC_ATTACK] = value;
    }

    public void addBaseMagicAttack(int value) {
        values[MAGIC_ATTACK] += value;
    }

    public void setDefense(int value) {
        values[DEFENSE] = value;
    }


    public void setMagicResist(int value) {
        values[MAGIC_RESIST] = value;
    }

    // cai nay phai chia 100
    public void setBaseCritChange(int value) {
        values[CRIT] = value;
    }

    public void setBaseAttackSpeed(int value) {
        values[ATTACK_SPEED] = value;
    }


    public void setCritDamage(int value) {
        values[CRIT_DAMAGE] = value;
    }


    public void setImmunity(int value) {
        values[IMMUNITY] = value;
    }

    public void setAgility(int value) {
        values[AGILITY] = value;
    }

    public void setShell(int value) {
        values[SHELL] = value;
    }

    public void setBaseHpRegen(int value) {
        values[HP_REGEN] = value;
    }

    // float => chia 100
    public void setMoveSpeed(int value) {
        values[MOVE_SPEED] = value;
    }

    public int getCrit() {
        int baseValue = values[CRIT];
        int changeValue = values[CHANGE_CRIT];
        return (int) ((baseValue) * (changeValue / 100f));
    }


    public int getCritDamage() {
        int baseValue = values[CRIT_DAMAGE];
        int changeValue = values[CHANGE_CRIT_DAMAGE];
        return (int) ((baseValue) * (changeValue / 100f));
    }

    public int getAttackDamage() {
        int baseAttack = values[ATTACK];
        int perAttack = values[P_ATTACK];
        int changeValue = values[CHANGE_ATTACK];
        return (int) (((baseAttack + baseAttack * perAttack / 100f) * (BasePerZen + values[ZEN_ATTACK]) / 100f) * (changeValue / 100f));
    }

    public int getDoge() { // né
        return values[DOGE];
    }

    public int getMagicDamage() {
        int baseValue = values[MAGIC_ATTACK];
        int perValue = values[P_MAGIC_ATTACK];
        int changeValue = values[CHANGE_MAGIC_ATTACK];
        return (int) (((baseValue + baseValue * perValue / 100f) * (BasePerZen + values[ZEN_MAGIC_ATTACK]) / 100f) * (changeValue / 100f));
    }

    public int getDameToBoss() {
        return values[ADDITION_DAMAGE_TO_BOSS];
    }

    public int getMagicResist() {
        int baseValue = values[MAGIC_RESIST];
        int perValue = values[P_MAGIC_RESIST];
        int changeValue = values[CHANGE_MAGIC_RESIST];
        return (int) ((baseValue + baseValue * perValue / 100f) * (changeValue / 100f));
    }

    public int getImmunity() {
        return values[IMMUNITY];
    }

    public int getAgility() {
        int baseValue = values[AGILITY];
//        System.out.println("baseValue = " + baseValue);
        int changeValue = values[CHANGE_AGILITY];
//        System.out.println("changeValue = " + changeValue);
//        System.out.println("baseValue * changeValue = " + baseValue * changeValue);
        return (baseValue * changeValue) / 100;
    }

    public int getCoolDown() {
        return values[COOLDOWN];
    }

    public int getMoveSpeed() {
        int baseValue = values[MOVE_SPEED];
        int perValue = values[P_MOVE_SPEED];
        int changeSpeed = values[CHANGE_MOVE_SPEED];
        return (int) ((baseValue + baseValue * perValue / 100f) * (changeSpeed / 100f));
    }

    public boolean equals(Point point) {
        return Arrays.equals(values, point.getValues());
    }

    // logic hơi phức tạp, buff <=100 thì buff thoải mái, dec >100 thì dec thoải mái.
    // trường hợp có dec mới thì phải so với dec cũ xem dec nào tốt hơn thì active, th buff cũ hết time thì áp buff mới vào luôn
    // thôi khó quá, trừ max 90% cho dễ =))
    public int buffChange(int changId, int buff, int maxChange) {
        if (buff > 0) { // add
            values[changId] += buff;
            return buff;
        } else { // dec
            int maxReduce = 100 - maxChange;
            if (values[changId] + buff < maxReduce) {
                int realBuff = values[changId] - maxReduce;
                values[changId] = maxReduce;
                return realBuff;
            } else {
                values[changId] += buff;
                return buff;
            }
        }
    }

    public int getAccuracy() {
        return values[ACCURACY];
    }

    public float getAttackSpeed() {
        float baseValue = values[ATTACK_SPEED] / 100f;
        float perValue = values[P_ATTACK_SPEED];
        int changeValue = values[CHANGE_ATTACK_SPEED];
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

    public int getDefense() {
        int baseValue = values[DEFENSE];
        int perValue = values[P_DEFENSE];
        int changeValue = values[CHANGE_DEFENSE];
        return (int) ((baseValue + baseValue * perValue / 100f) * (changeValue / 100f));
    }

    public float getChangeDame() {
        return values[CHANGE_DAME] / 100f;
    }


    public int getChangeAttackSpeed() {
        return values[CHANGE_ATTACK_SPEED];
    }

    public boolean isTrueDame() {
        return values[TRUE_DAME] == 1;
    }

    public synchronized int getCurMP() {
        return values[CUR_MP];
    }

    public int getWeight() {
        return values[WEIGHT];
    }

    public int getCritDamageReduce() {
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
            point.values[i] = (int) (values[i] * per);
        }
        return point;
    }

    public synchronized int getCurHP() {
        return values[CUR_HP];
    }

    public int getPerHp() {
        return (int) (getCurHP() * 100 / getMaxHp());
    }

    public int getPerMp() {
        return (int) (getCurMP() * 100 / getMaxMp());
    }

    public int getMaxHp() { // max HP
        int baseValue = values[HP];
        int perValue = values[P_HP];
        return (int) ((baseValue + baseValue * perValue / 100f) * (BasePerZen + values[ZEN_HP]) / 100f);
    }

    public int getMaxMp() { // max MP
        int baseValue = values[MP];
        int perValue = values[P_MP];
        return (int) (baseValue + baseValue * perValue / 100f);
    }

    public int getHpRegen() {
        int baseValue = values[HP_REGEN];
        int perValue = values[P_HP_REGEN];
        return Math.round(baseValue + baseValue * perValue / 100f);
    }

    public int getCurStun() {
        return get(STUN);
    }

    public int getCurShell() {
        return get(SHELL);
    }

    public int getMpRegen() {
        int baseValue = values[MP_REGEN];
        int perValue = values[P_MP_REGEN];
        return Math.round(baseValue + baseValue * perValue / 100f);
    }

    public int getPower() {
        return values[POWER];
    }

    public void calculatorPower(int level, float perItemWeaponEquip) { // perItemWeaponEquip : hệ số atk
        int power = 0;
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

    public int[] getValues() {
        return values;
    }

    public boolean beBlock() {
        int now = (int) System.currentTimeMillis();
        return values[STUN] > now || values[FREEZE] > now || values[Point.BLOCK_PARALYZE] > now;
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
        int[] newValues = point.getValues();
        int size = Math.min(newValues.length, values.length);
        for (int i = 0; i < size; i++) {
            if (values[i] < newValues[i]) values[i] = newValues[i];
        }
    }

    // tang giam chi so
    public synchronized boolean addCurHp(int value) {
        values[CUR_HP] += value * values[CHANGE_HEATH] / 100f; // nhân với giảm khả năng hồi phục
        values[CUR_HP] = values[CUR_HP] < 0 ? 0 : values[CUR_HP];
        values[CUR_HP] = Math.min(values[CUR_HP], getMaxHp());
        return values[CUR_HP] > 0; // true = alive
    }

    public void checkCurHp() {
        values[CUR_HP] = values[CUR_HP] > getMaxHp() ? getMaxHp() : values[CUR_HP];
    }


    public void addAttack(int value) {
        values[ATTACK] += value;
    }


    public void addMagicAttack(int value) {
        values[MAGIC_ATTACK] += value;
    }

    public void addPerAttack(int value) {
        values[P_ATTACK] += value;
    }

    public void addPerMagicAttack(int value) {
        values[P_MAGIC_ATTACK] += value;
    }

    public void addPerMoveSpeed(int value) {
        values[P_MAGIC_ATTACK] += value;
    }


    void addHp(int value) {
        values[HP] += value;
    }

    public void addPerHp(int value) {
        values[P_HP] += value;
    }

    public void addMp(int value) {
        values[MP] += value;
    }

    public void addPerMp(int value) {
        values[P_MP] += value;
    }

    public void addCrit(int value) {
        values[CRIT] += value;
    }

    public void addCritDamage(int value) {
        values[CRIT_DAMAGE] += value;
    }

    public void addDef(int value) {
        values[DEFENSE] += value;
    }

    public void addPerDef(int value) {
        values[P_DEFENSE] += value;
    }

    public void addMagicResist(int value) {
        values[MAGIC_RESIST] += value;
    }

    public void addPerMagicResist(int value) {
        values[P_MAGIC_RESIST] += value;
    }

    public void addHpRegen(int value) {
        values[HP_REGEN] += value;
    }

    public void addPerHpRegen(int value) {
        values[P_HP_REGEN] += value;
    }

    public void addMpRegen(int value) {
        values[MP_REGEN] += value;
    }

    public void addMoveSpeed(int value) {
        values[MOVE_SPEED] += value;
    }

    public void addPerMpRegen(int value) {
        values[P_MP_REGEN] += value;
    }

    public void addCoolDown(int value) {
        values[COOLDOWN] += value;
    }

    public void addImmunity(int value) {
        values[IMMUNITY] += value;
    }

    public void addAgility(int value) {
        values[AGILITY] += value;
    }

    public void addAttackSpeed(int value) {
        values[ATTACK_SPEED] += value;
    }

    public void addBattlePower(int value) {
        values[POWER] += value;
    }

    public void addZenAttack(int value) {
        values[ZEN_ATTACK] += value;
    }

    public void addZenMagicAttack(int value) {
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


    public List<Integer> toProto() {
        List<Integer> ret = new ArrayList<>(getValues().length);
        for (int i = 0; i < getValues().length; i++) {
            ret.add(getValues()[i]);
        }
        return ret;
    }

    public Pbmethod.CommonVector toCommonVector() {
        return CommonProto.getCommonIntVector(toProto());
    }


}
