package game.battle.object;

import com.google.gson.Gson;
import game.battle.calculate.IMath;
import game.battle.model.Unit;
import game.object.PointBuff;
import game.protocol.CommonProto;
import game.config.CfgStats;
import game.treasure.BattleConfig;
import lombok.Getter;
import lombok.Setter;
import protocol.Pbmethod;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

public class Point {
    public static final int POWER = 0; // done
    public static final int CUR_HP = 1;// done
    public static final int ATTACK = 2;// done
    public static final int P_ATTACK = 3;
    public static final int HP = 4;// done
    public static final int P_HP = 5;
    public static final int MOVE_SPEED = 6;// done
    public static final int P_MOVE_SPEED = 7;
    public static final int DEFENSE = 8;// done
    public static final int P_DEFENSE = 9;
    public static final int CRIT = 10;// done
    public static final int CRIT_DAMAGE = 11;// done
    public static final int COUNTER_ATTACK = 12;
    public static final int LIFE_STEAL = 13;
    public static final int ACCURACY = 14;
    public static final int HEALING = 15;
    public static final int P_ITEM_DROP_INCREASE = 16;
    public static final int P_GOLD_DROP_INCREASE = 17;
    public static final int DOGE = 18;              //done
    public static final int DOT = 19;
    public static final int FREEZE = 20;
    public static final int POISON = 21;
    public static final int MULTI_ATTACK = 22;
    public static final int SLOT_BAG_UI = 23;// done
    public static final int SLOT_BAG_MATERIAL = 24;// done
    public static final int ATTACK_SPEED = 25;
    public static final int WIND = 26;
    public static final int SUMMON = 27;
    public static final int P_GEM_INCREASE = 28;
    public static final int P_MATERIAL_INCREASE = 29;
    public static final int RESISTANCE = 30;

    // CHANGE
    public static final int CHANGE_ATTACK = 40;
    public static final int CHANGE_DEFENSE = 41;
    public static final int CHANGE_MAGIC_RESIST = 42;
    public static final int CHANGE_AGILITY = 43;
    public static final int CHANGE_HEATH = 44;
    public static final int CHANGE_CRIT = 45;
    public static final int CHANGE_CRIT_DAMAGE = 46;
    public static final int CHANGE_MOVE_SPEED = 47;
    public static final int CHANGE_ATTACK_SPEED = 48;
    // EFFECT
    public static final int BLOCK_PARALYZE = 49;
    public static final int STUN = 50;

    @Setter
    @Getter
    private float startHpPercent = -1; // 0 - 10000
    public static int size = 100;
    private int BasePerZen = 100;

    @Getter
    long[] values;

    public Point() {
        values = new long[size];
        initDefault();
    }

    public void initDefault() {
        // set 1 số chỉ số mặc định
        values[CHANGE_MOVE_SPEED] = 100;
        values[CHANGE_ATTACK_SPEED] = 100;
        values[CHANGE_DEFENSE] = 100;
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
        values = new long[size];
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

    /** Chỉ số Băng (raw). Effect đóng băng làm sau — không ghi timestamp vào slot này. */
    public long getFreezeStat() {
        return values[FREEZE];
    }

    public void setBaseDef(int value) {
        values[DEFENSE] = value;
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

    public long setCurHp(long curHp) {
        return values[CUR_HP] = curHp;
    }


    public void resetHp() {
        setCurHp(getMaxHp());
    }

    public void resetHpPercent(int percent) {
        setCurHp((long) (getMaxHp() * percent / 100f));
    }

    public long forceDie() {
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

    // float => chia 100
    public void setMoveSpeed(int value) {
        values[MOVE_SPEED] = value;
    }

    public long getCrit() {
        long baseValue = values[CRIT];
        long changeValue = values[CHANGE_CRIT];
        return (int) ((baseValue) * (changeValue / 100f));
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
        return (long) (baseAttack + baseAttack * perAttack / 100f);
    }

    public long getDoge() { // né
        return values[DOGE];
    }

    public long getCounterAttack() {
        return values[COUNTER_ATTACK];
    }

    public long getMultiAttack() {
        return values[MULTI_ATTACK];
    }

    public long getPoison() {
        return values[POISON];
    }

    public long getWind() {
        return values[WIND];
    }

    public long getFire() {
        return values[DOT];
    }

    public long getResistance() {
        return values[RESISTANCE];
    }

    public long getHealing() {
        return values[HEALING];
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
    public long buffChange(int changId, long buff, long maxChange) {
        if (buff > 0) { // add
            values[changId] += buff;
            return buff;
        } else { // dec
            long maxReduce = 100 - maxChange;
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

    public long getLifeSteal() {
        return values[LIFE_STEAL];
    }

    public float getAttackSpeed() {
        float interval = CfgStats.calcAttackInterval(BattleConfig.attackSpeed, values[ATTACK_SPEED]);
        long changeAtkSpeed = values[CHANGE_ATTACK_SPEED];
        if (changeAtkSpeed <= 0) {
            changeAtkSpeed = 100;
        }
        return interval * (100f / changeAtkSpeed);
    }

    public int getBuffDrop() {
        return (int) (values[P_ITEM_DROP_INCREASE]);
    }

    public int getBuffGold() {
        return (int) (values[P_GOLD_DROP_INCREASE]);
    }

    public long getDefense() {
        long baseValue = values[DEFENSE];
        long perValue = values[P_DEFENSE];
        long changeValue = values[CHANGE_DEFENSE];
        return (long) ((baseValue + baseValue * perValue / 100f) * (changeValue / 100f));
    }

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

    public synchronized long getCurHP() {
        return values[CUR_HP];
    }

    public int getPerHp() {
        return (int) (getCurHP() * 100 / getMaxHp());
    }

    public Long getMaxHp() { // max HP
        long baseValue = values[HP];
        long perValue = values[P_HP];
        return (long) (baseValue + baseValue * perValue / 100f);
    }


    public Long getCurStun() {
        return get(STUN);
    }

    public Long getPower() {
        return values[POWER];
    }


    /** Lực chiến = tổng raw point; HP × 0.1, MOVE_SPEED × 2.5 để cân scale. */
    public void calculatorPower() {
        long power = 0;
        power += values[ATTACK];
        power += (long) (values[HP] * 0.1f);
        power += values[DEFENSE];
        power += values[CRIT];
        power += values[CRIT_DAMAGE];
        power += values[COUNTER_ATTACK];
        power += values[LIFE_STEAL];
        power += values[ACCURACY];
        power += values[HEALING];
        power += values[DOGE];
        power += values[DOT];
        power += values[FREEZE];
        power += values[POISON];
        power += values[MULTI_ATTACK];
        power += values[ATTACK_SPEED];
        power += values[WIND];
        power += values[SUMMON];
        power += (long) (values[MOVE_SPEED] * 2.5f);
        values[POWER] = power;
    }

    public boolean beBlock() {
        long now = System.currentTimeMillis();
        return values[STUN] > now || values[Point.BLOCK_PARALYZE] > now;
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
        values[CUR_HP] += (long) (value * values[CHANGE_HEATH] / 100f); // nhân với giảm khả năng hồi phục
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

    public void addBattlePower(long value) {
        values[POWER] += value;
    }


    public void buffPer(int per) {
        addBattlePower(getPower() * per);
        addPerHp(per);
        addPerAttack(per);
        addPerDef(per);
        addCrit(per / 10);
        addCritDamage(per);
    }


    public List<Long> toProto() {
        List<Long> ret = new ArrayList<>(getValues().length);
        for (int i = 0; i < getValues().length; i++) {
            ret.add(getValues()[i]);
        }
        return ret;
    }

    public Pbmethod.CommonVector toCommonVector() {
        return CommonProto.getCommonVector(toProto());
    }


}
