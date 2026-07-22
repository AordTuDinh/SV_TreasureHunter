package game.treasure.service.battle;

import game.battle.model.Player;
import game.battle.object.Point;
import game.config.aEnum.ToastType;
import game.object.MyUser;
import game.treasure.mapping.UserEntity;
import game.treasure.mapping.UserEquipmentEntity;
import game.treasure.server.IAction;
import ozudo.base.helper.Util;
import protocol.Pbmethod;

import java.util.List;

/**
 * Gate tấn công theo vùng (typeRoom) dựa trên ATK vũ khí đã scale level.
 * Vùng xanh (1): không check. Xám (2): ≥300. Băng (3): ≥400.
 */
public final class ZoneAttackService {
    public static final int ATK_REQUIRE_GREY = 300;
    public static final int ATK_REQUIRE_ICE = 400;
    private static final long TOAST_COOLDOWN_MS = 2000L;

    private ZoneAttackService() {
    }

    /** Đọc ATK vũ khí đang mặc (đã scale) → cập nhật 2 flag trên MyUser. */
    public static void refresh(MyUser mUser) {
        if (mUser == null) {
            return;
        }
        long weaponAtk = getEquippedWeaponAttack(mUser);
        mUser.setCanAttackGrey(weaponAtk >= ATK_REQUIRE_GREY);
        mUser.setCanAttackIce(weaponAtk >= ATK_REQUIRE_ICE);
    }

    public static long getEquippedWeaponAttack(MyUser mUser) {
        if (mUser == null || mUser.getUser() == null || mUser.getResources() == null) {
            return 0;
        }
        List<Integer> lst = mUser.getUser().normalizeItemEquipList();
        int idx = UserEntity.equipSlotIndex(Pbmethod.EquipSlotType.WEAPON.getNumber());
        if (idx < 0 || idx >= lst.size()) {
            return 0;
        }
        int rowId = lst.get(idx);
        if (rowId <= 0) {
            return 0;
        }
        UserEquipmentEntity equip = mUser.getResources().getEquipment(rowId);
        if (equip == null) {
            return 0;
        }
        List<Long> points = equip.getPoint();
        if (points == null || points.isEmpty()) {
            return 0;
        }
        for (int i = 0; i + 1 < points.size(); i += 2) {
            if (points.get(i).intValue() == Point.ATTACK) {
                return points.get(i + 1);
            }
        }
        return 0;
    }

    public static int getRequiredAtk(int typeRoom) {
        if (typeRoom == 3) {
            return ATK_REQUIRE_ICE;
        }
        if (typeRoom == 2) {
            return ATK_REQUIRE_GREY;
        }
        return 0;
    }

    /** Check theo typeRoom chunk hiện tại + flag đã cache. */
    public static boolean canAttack(Player player) {
        if (player == null || player.getMUser() == null) {
            return false;
        }
        int typeRoom = DeathPenaltyService.resolveTypeRoom(player);
        if (typeRoom <= 1) {
            return true;
        }
        MyUser mUser = player.getMUser();
        if (typeRoom == 2) {
            return mUser.isCanAttackGrey();
        }
        if (typeRoom == 3) {
            return mUser.isCanAttackIce();
        }
        return true;
    }

    /** Reject ATTACK + toast "Vũ khí cần X tấn công để khai thác ở vùng này". */
    public static boolean tryAttackOrToast(Player player) {
        if (canAttack(player)) {
            return true;
        }
        notifyBlocked(player);
        return false;
    }

    public static void notifyBlocked(Player player) {
        if (player == null || player.getMUser() == null || player.getMUser().getChannel() == null) {
            return;
        }
        MyUser mUser = player.getMUser();
        long now = System.currentTimeMillis();
        if (now - mUser.getLastZoneAttackToastMs() < TOAST_COOLDOWN_MS) {
            return;
        }
        mUser.setLastZoneAttackToastMs(now);
        int typeRoom = DeathPenaltyService.resolveTypeRoom(player);
        int need = getRequiredAtk(typeRoom);
        if (need <= 0) {
            need = ATK_REQUIRE_GREY;
        }
        String msg = "Vũ khí cần " + need + " tấn công để khai thác ở vùng này";
        Util.sendProtoData(mUser.getChannel(), ToastType.NORMAL.retToast(msg), IAction.MSG_TOAST);
    }
}
