package game.battle.calculate;

import game.battle.object.Point;
import game.battle.object.Pos;
import game.battle.model.Unit;
import game.config.CfgClan;
import game.config.CfgStats;
import game.treasure.mapping.*;
import game.treasure.mapping.main.*;
import game.treasure.service.Services;
import game.treasure.service.resource.*;
import game.object.*;
import ozudo.base.helper.NumberUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class IMath {

    public static final List<Integer> POINT_X100 = List.of();

    public static float randomBetweenFloat(float max, float min) {
        float random = min + new Random().nextFloat() * (max - min);
        return random;
    }

    public static Pos randomPosInMap(float sizeX, float sizeY) {
        Pos ps = Pos.zero();
        ps.x = randomBetweenFloat(-Math.abs(sizeX), Math.abs(sizeX));
        ps.y = randomBetweenFloat(-Math.abs(sizeY), Math.abs(sizeY));
        return ps;
    }

    public static float getOf() {
        return NumberUtil.getRandom(0.5f, 1.5f);
    }

    public static float round1(float num) {
        return Math.round(num * 10f) / 10f;
    }


    public static boolean isCrit(long crit) {
        return CfgStats.rollSuccess(CfgStats.getEffectiveStatForSuccess(crit));
    }

    /** Roll né: success(D), D = Né − Chính Xác (cap statCap / referenceEffective). */
    public static boolean rollDodge(Unit target, Unit attacker) {
        if (target == null) {
            return false;
        }
        long accuracy = attacker != null ? attacker.getPoint().getAccuracy() : 0;
        long effectiveDodge = CfgStats.getEffectiveDodge(target.getPoint().getDoge(), accuracy);
        return CfgStats.rollSuccess(effectiveDodge);
    }

    public static Pos getDirection(Pos from, Pos to) {
        return new Pos(to.x - from.x, to.y - from.y).normalized();
    }

    /** Đa mục tiêu: 75 điểm = +1 mục tiêu, tối đa 7. */
    public static int calcMaxAttackTargets(long multiAttackStat) {
        long stat = Math.max(0, multiAttackStat);
        return Math.min(7, 1 + (int) (stat / 75));
    }


    public static long[] calculateDamage(Unit attacker, Unit target) {// crit, normalDame, critDame
        return calculateDamage(attacker, target, attacker.getPoint().getAttackDamage());
    }

    public static long[] calculateDamage(Unit attacker, Unit target, long atkDame) {
        int status = 0;
        if (rollDodge(target, attacker)) {
            return new long[]{status, 0, 0};
        }

        int normalDame = calculateDamageBase(atkDame, target, null, attacker);
        long critDame = 0;
        if (isCrit(attacker.getPoint().getCrit())) {
            status = 1;
            critDame = calcCritBonusDamage(attacker);
        }
        return new long[]{status, normalDame, critDame};
    }

    /**
     * Phản đòn (point 12): proc 450→60% (trần 65%), phản 100% sát thương chuẩn vừa nhận,
     * rồi giảm theo Né nạn nhân (450 Né → 25%, trần 30%).
     */
    public static boolean tryCounterAttack(Unit counterer, Unit victim, long normalDamageReceived) {
        if (counterer == null || victim == null || !counterer.isAlive() || !victim.isAlive()) {
            return false;
        }
        if (normalDamageReceived <= 0) {
            return false;
        }
        if (!CfgStats.rollCounterProc(counterer.getPoint().getCounterAttack())) {
            return false;
        }
        int counterDmg = calculateCounterDamage(counterer, victim, normalDamageReceived);
        if (counterDmg <= 0) {
            return false;
        }
        victim.beAttackDamage(counterer, counterDmg);
        victim.protoStatus(protocol.Pbmethod.SubStateType.BE_DAMAGE,
                Arrays.asList(counterer.getId(), 0L, -(long) counterDmg, 0L));
        return true;
    }

    public static int calculateCounterDamage(Unit counterer, Unit victim, long normalDamageReceived) {
        int baseDmg = (int) normalDamageReceived;
        long effectiveDodge = CfgStats.getEffectiveDodge(
                victim.getPoint().getDoge(),
                counterer.getPoint().getAccuracy()
        );
        return CfgStats.calcCounterDamageTaken(baseDmg, effectiveDodge);
    }

    /** Sát thương crit riêng = chỉ số crit damage × 2, trừ thẳng vào máu (không qua giáp). */
    public static long calcCritBonusDamage(Unit attacker) {
        if (attacker == null) {
            return 0;
        }
        return Math.max(0, attacker.getPoint().getCritDamage()) * 2L;
    }

    /** Hồi máu theo % sát thương đã gây (450 hút → 40%, trần 45%). */
    public static void applyLifeSteal(Unit attacker, long damageDealt) {
        if (attacker == null || damageDealt <= 0 || !attacker.isAlive()) {
            return;
        }
        int heal = CfgStats.calcLifeStealHeal(attacker.getPoint().getLifeSteal(), damageDealt);
        if (heal > 0) {
            attacker.reHpFixed(heal);
        }
    }

    /** Độc: chỉ số bị Kháng target trừ %, roll success khi đòn trúng (không miss). */
    public static void tryApplyPoison(Unit attacker, Unit target) {
        if (attacker == null || target == null || !attacker.isAlive() || !target.isAlive()) {
            return;
        }
        long poison = CfgStats.getEffectiveElementAfterResist(
                attacker.getPoint().getPoison(), target.getPoint().getResistance());
        if (poison <= 0 || !CfgStats.rollPoisonProc(poison)) {
            return;
        }
        target.applyPoison(attacker, poison);
    }

    /** Gió: chỉ số bị Kháng trừ %, proc success; trúng thì giảm Né / Chính Xác theo successRate(Gió). */
    public static void tryApplyWind(Unit attacker, Unit target) {
        if (attacker == null || target == null || !attacker.isAlive() || !target.isAlive()) {
            return;
        }
        long wind = CfgStats.getEffectiveElementAfterResist(
                attacker.getPoint().getWind(), target.getPoint().getResistance());
        if (wind <= 0 || !CfgStats.rollWindProc(wind)) {
            return;
        }
        target.applyWind(wind);
    }

    /** Lửa: chỉ số bị Kháng trừ %, roll success khi đòn trúng; tick sau 1s, damage = Lửa/2 trừ thẳng máu. */
    public static void tryApplyFire(Unit attacker, Unit target) {
        if (attacker == null || target == null || !attacker.isAlive() || !target.isAlive()) {
            return;
        }
        long fire = CfgStats.getEffectiveElementAfterResist(
                attacker.getPoint().getFire(), target.getPoint().getResistance());
        if (fire <= 0 || !CfgStats.rollFireProc(fire)) {
            return;
        }
        target.applyFire(attacker, fire);
    }

    /** Băng: chỉ số bị Kháng trừ %, roll success khi đòn trúng (không miss). */
    public static void tryApplyFreeze(Unit attacker, Unit target) {
        if (attacker == null || target == null || !attacker.isAlive() || !target.isAlive()) {
            return;
        }
        long freezeStat = CfgStats.getEffectiveElementAfterResist(
                attacker.getPoint().getFreezeStat(), target.getPoint().getResistance());
        if (freezeStat <= 0 || !CfgStats.rollFreezeProc(freezeStat)) {
            return;
        }
        target.applyFreeze(attacker, freezeStat);
    }

    // Hàm gốc - tất cả đều tính qua hàm này
    public static int calculateDamageBase(long atkDame, Unit beAttacker, PointBuff buff, Unit attacker) {
        long def = beAttacker.getPoint().getDefense();
        if (buff != null && buff.getPointId() == Point.DEFENSE) {
            def += buff.getValue();
        }

        long accuracy = attacker != null ? attacker.getPoint().getAccuracy() : 0;
        def = CfgStats.calcDefenseAfterAccuracy(def, accuracy, beAttacker.getPoint().getDoge());
        float reduceRate = CfgStats.calcSuccessRate(CfgStats.getEffectiveStatForSuccess(def));
        float dmgF = atkDame * (1f - reduceRate);
        int dmg = (int) dmgF;
        if (dmg <= 0) dmg = 1;
        return dmg;
    }

    public static Point calculatePoint(MyUser mUser, boolean hasItemEquip) {
        Point pt = PlayerBasePoint.getBase();
//        System.out.println("point 8 = " + pt.toMiniString());
        // phúc lợi bang hội
        if (mUser.getUser().getClan() > 0) {
            ClanEntity clan = Services.clanDAO.getClan(mUser.getUser().getClan());
            if (clan != null) {
                int level = clan.getLevel();
                for (int i = 0; i < level; i++) {
                    CfgClan.ClanWelfare welfare = CfgClan.getClanWelfare(i);
                    if (welfare.point > 0) addPointData(pt, welfare.point, welfare.num / 100f);
                }
            }
        } else if (mUser.getUser().getClan() == CfgClan.ASSASSIN_CLAN_ID) {
            addPointData(pt, Point.P_MOVE_SPEED, CfgClan.assassinMoveSpeedBonus);
        } else if (mUser.getUser().getClan() == CfgClan.WARRIOR_CLAN_ID) {
            addPointData(pt, Point.P_HP, CfgClan.warriorHpBonus);
        }
        if (hasItemEquip)
            addEquippedItemPoints(mUser, pt);
        // cal power from shuriken equipment
        float perPowerWeaponEquip = 1;

        // power tinh cuoi cung
        pt.calculatorPower(1, perPowerWeaponEquip);
        return pt;
    }

    /** Cộng stat từ trang bị đang mặc (user.item_equipment, id > 0). */
    static void addEquippedItemPoints(MyUser mUser, Point pt) {
        for (int itemId : mUser.getUser().getListIdEquipmentEquip()) {
            if (itemId <= 0)
                continue;
            UserEquipmentEntity item = mUser.getResources().getItemEquipment(itemId);
            if (item == null)
                continue;
            List<Long> itemPoints = item.getPoint();
            if (itemPoints == null || itemPoints.isEmpty())
                continue;
            for (int i = 0; i + 1 < itemPoints.size(); i += 2)
                addPointData(pt, itemPoints.get(i).intValue(), itemPoints.get(i + 1).floatValue());
        }
    }

    public static void addPointEffect(Point point, PointData[] aEffect) {
        for (int i = 0; i < aEffect.length; i++) {
            List<Long> aPoint = aEffect[i].getPoint();
            for (int j = 0; j < aPoint.size(); j += 2) {
                addPointData(point, aPoint.get(j).intValue(), aPoint.get(j + 1) / 100f);
            }
        }
    }


    // point cộng thẳng, đã chia 100, với các point khác thì x100 rồi chia trong battle
    public static void addPointData(Point point, int pData, float addValue) {
        if (POINT_X100.contains(pData)) {
            point.add(pData, (int) (addValue * 100));
        } else point.add(pData, (int) addValue);
    }


    //format [mainId - pointId - value]
    public static List<Long> mergePointWeapon(List<Long> point, List<Long> pointAdd) {
        List<Long> pointIds = new ArrayList<>();
        for (int i = 0; i < point.size(); i += 3) {
            pointIds.add(point.get(i + 1));
        }
        for (int i = 0; i < pointAdd.size(); i += 2) {
            if (pointIds.contains(pointAdd.get(i))) {
                int indexId = pointIds.indexOf(pointAdd.get(i));
                point.set(indexId * 3 + 2, point.get(indexId * 3 + 2) + pointAdd.get(i + 1));
            } else {
                point.add(0L);
                point.add(pointAdd.get(i));
                point.add(pointAdd.get(i + 1));
            }
        }
        return point;
    }
}
