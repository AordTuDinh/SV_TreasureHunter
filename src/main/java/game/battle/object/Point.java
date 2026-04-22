package game.battle.object;

import com.google.gson.Gson;
import game.battle.calculate.IMath;
import game.battle.model.Unit;
import game.object.PointBuff;
import game.protocol.CommonProto;
import game.treasure.BattleConfig;
import lombok.Getter;
import lombok.Setter;
import protocol.Pbmethod;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

public class Point {
    public static final int POWER = 0;
    public static final int CUR_HP = 1;
    public static final int ATTACK = 2;
    public static final int P_ATTACK = 3;
    public static final int HP = 4;
    public static final int P_HP = 5;
    public static final int MOVE_SPEED = 6;
    public static final int P_MOVE_SPEED = 7;
    public static final int DEFENSE = 8;
    public static final int P_DEFENSE = 9;
    public static final int CRIT = 10;
    public static final int CRIT_DAMAGE = 11;
    public static final int IMMUNITY = 12;
    public static final int AGILITY = 13;
    public static final int ACCURACY = 14;
    public static final int CRIT_DAMAGE_REDUCTION = 15;
    public static final int P_ITEM_DROP_INCREASE = 16;
    public static final int P_GOLD_INCREASE = 17;
    public static final int DOGE = 18;

    // CHANGE
    public static final int CHANGE_MOVE_SPEED = 19;
    public static final int CHANGE_ATTACK = 20;
    public static final int CHANGE_DEFENSE = 21;
    public static final int CHANGE_MAGIC_RESIST = 22;
    public static final int CHANGE_AGILITY = 23;
    public static final int CHANGE_HEATH = 24;
    public static final int CHANGE_CRIT = 25;
    public static final int CHANGE_CRIT_DAMAGE = 26;

    // EFFECT
    public static final int BLOCK_PARALYZE = 27;
    public static final int STUN = 28;
    public static final int FREEZE = 29;
    public static final int CHANGE_DAME = 30;

    @Setter
    @Getter
    private float startHpPercent = -1; // 0 - 10000
    public static int size = 100;
    private int BasePerZen = 100;

    int[] values;

    public Point() {
        values = new int[size];
        initDefault();
    }

    public void initDefault() {
        // set 1 số chỉ số mặc định
        values[CHANGE_MOVE_SPEED] = 100;
        values[CHANGE_DEFENSE] = 100;
        values[CHANGE_DAME] = 100;
        values[CHANGE_MAGIC_RESIST] = 100;
        values[CHANGE_AGILITY] = 100;
        values[CHANGE_ATTACK] = 100;
        values[CHANGE_CRIT] = 100;
        values[CHANGE_CRIT_DAMAGE] = 100;
        values[CHANGE_HEATH] = 100;
        values[BLOCK_PARALYZE] = 0;
        values[STUN] = 0;
        values[FREEZE] = 0;
    }

    public Point(List<Integer> data) {
        values = new int[size];
        initDefault();
        for (int i = 0; i < data.size(); i++) {
            values[i] = data.get(i);
        }
    }


    public void clear() {
        Arrays.fill(values, 0);
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
                values[CUR_HP] = Math.min(values[CUR_HP], getMaxHp());
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
        if (!unit.isAlive()) return;
        switch (buff.getPointId()) {
            case CUR_HP -> unit.setAlive(addCurHp(buff.getValue()));
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
    }

    public int forceDie() {
        return values[CUR_HP] = 0;
    }


    public void setBaseAttack(int value) {
        values[ATTACK] = value;
    }

    public void setDefense(int value) {
        values[DEFENSE] = value;
    }

    // cai nay phai chia 100
    public void setBaseCritChange(int value) {
        values[CRIT] = value;
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
//        int baseAttack = values[ATTACK];
//        int perAttack = values[P_ATTACK];
//        int changeValue = values[CHANGE_ATTACK];
//        return (int) (((baseAttack + baseAttack * perAttack / 100f) * (BasePerZen + values[ZEN_ATTACK]) / 100f) * (changeValue / 100f));
        return 200;
    }

    public int getDoge() { // né
        return values[DOGE];
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
        return BattleConfig.attackSpeed;
    }

    public int getBuffDrop() {
        return (int) (values[P_ITEM_DROP_INCREASE]);
    }

    public int getBuffGold() {
        return (int) (values[P_GOLD_INCREASE]);
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

    public int getMaxHp() { // max HP
        int baseValue = values[HP];
        int perValue = values[P_HP];
        return (int) (baseValue + baseValue * perValue / 100f);
    }


    public int getCurStun() {
        return get(STUN);
    }

    public int getPower() {
        return values[POWER];
    }

    public void calculatorPower(int level, float perItemWeaponEquip) { // perItemWeaponEquip : hệ số atk
        int power = 0;
//        System.out.println("perItemWeaponEquip = " + perItemWeaponEquip);
        power += getAttackDamage() * 0.5f;
        power += getAttackDamage() * perItemWeaponEquip;
//        System.out.println("power2 = " + power);
        power += getMaxHp() * 0.5f;
//        System.out.println("power3 = " + power);
        power += getAttackSpeed() * 5f;
        power += getMoveSpeed() * 2f;
//        System.out.println("power8 = " + power);
        power += getDefense() * 2f;
//        System.out.println("power10 = " + power);
        power += getCrit() * level * 0.02f;
//        System.out.println("power11 = " + power);
        power += getCritDamage() * 0.02f;
//        System.out.println("power12 = " + power);
        power += getAgility() * level * 0.02f;
//        System.out.println("power13 = " + power);
        power += getImmunity() * level * 0.02f;
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

    public void addPerAttack(int value) {
        values[P_ATTACK] += value;
    }

    void addHp(int value) {
        values[HP] += value;
    }

    public void addPerHp(int value) {
        values[P_HP] += value;
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

    public void addMoveSpeed(int value) {
        values[MOVE_SPEED] += value;
    }

    public void addImmunity(int value) {
        values[IMMUNITY] += value;
    }

    public void addAgility(int value) {
        values[AGILITY] += value;
    }

    public void addBattlePower(int value) {
        values[POWER] += value;
    }


    public void buffPer(int per) {
        addBattlePower(getPower() * per);
        addPerHp(per);
        addPerAttack(per);
        addPerDef(per);
        addCrit(per / 10);
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
