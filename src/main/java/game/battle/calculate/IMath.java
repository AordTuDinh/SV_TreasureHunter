package game.battle.calculate;

import game.battle.object.Point;
import game.battle.object.Pos;
import game.battle.model.Unit;
import game.config.CfgClan;
import game.treasure.mapping.*;
import game.treasure.mapping.main.*;
import game.treasure.service.Services;
import game.treasure.service.resource.*;
import game.object.*;
import ozudo.base.helper.NumberUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class IMath {

    public static final List<Integer> POINT_X100 = List.of(Point.CRIT, Point.AGILITY, Point.IMMUNITY);


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
        float rawCrit = crit / 100f;
        float finalCrit = rawCrit / (1f + rawCrit / 100f);
        if (finalCrit == 0) return false;
        return NumberUtil.getRandom(10000) < (int) (finalCrit * 100f);
    }

    public static Pos getDirection(Pos from, Pos to) {
        return new Pos(to.x - from.x, to.y - from.y).normalized();
    }


    public static long[] calculateDamage(Unit attacker, Unit target) {// crit,atk,matk
        return calculateDamage(attacker, target, attacker.getPoint().getAttackDamage());
    }

    public static long[] calculateDamage(Unit attacker, Unit target, long atkDame) {
        int status = 0;
        // check doge
        long dodge = target.getPoint().getDoge();
        long acc = attacker != null ? attacker.getPoint().getAccuracy() : 0;

        dodge = Math.min(dodge, 450);
        acc = Math.min(acc, 450);
        if (dodge > 0) {
            float dodgeChance =  (dodge * 1f) / (dodge + acc);
            if (NumberUtil.getRandom(1000) < (int) (dodgeChance * 1000f)) {
                return new long[]{status, 0};
            }
        }

        float critPer = 1f;
        if (isCrit(attacker.getPoint().getCrit())) {
            status = 1;
            float bonus = attacker.getPoint().getCritDamage() - 200f; // nếu critDamage đang chứa total%
            float reduce = target.getPoint().getCritDamageReduce();   // hiểu là % giảm bonus hoặc giảm total tuỳ bạn
            float finalCritDamagePercent = 200f + Math.max(0f, bonus - reduce);
            critPer = finalCritDamagePercent / 100f;
        }
        int dame = calculateDamageBase(atkDame, target, critPer, null, attacker);
        return new long[]{status, dame};
    }

    // Hàm gốc - tất cả đều tính qua hàm này
    public static int calculateDamageBase(long atkDame, Unit beAttacker, float critPer, PointBuff buff, Unit attacker) {
        // Lưu ý : Sát thương chuẩn sẽ mạnh hơn giảm dame trực tiếp
        float changeDame = beAttacker.getPoint().getChangeDame();
        long def = beAttacker.getPoint().getDefense();
        if (buff != null && buff.getPointId() == Point.DEFENSE) {
            def += buff.getValue();
        }

        // tinh dame
        float atkF = (float) atkDame;
        float defF = (float) def;
        int dmg = (int) ((atkF * atkF) / (atkF + defF + 0.0001f)); // tránh chia 0

        dmg = (int) (dmg * changeDame * critPer);
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
        }
        // cal power from shuriken equipment
        float perPowerWeaponEquip = 1;

        // power tinh cuoi cung
        pt.calculatorPower(1, perPowerWeaponEquip);
        return pt;
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
