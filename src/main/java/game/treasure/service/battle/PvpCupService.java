package game.treasure.service.battle;

import game.battle.model.Player;
import game.config.CfgServer;
import game.config.CfgUser;
import game.config.aEnum.DetailActionType;
import game.monitor.Online;
import game.object.DataDaily;
import game.object.MyUser;
import game.treasure.mapping.UserEntity;
import game.treasure.service.user.Actions;
import game.treasure.service.user.Bonus;
import ozudo.base.database.DBJPA;
import protocol.Pbmethod;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Chuyển cup khi player hạ player khác.
 * Online: ADD_BONUS (BONUS_CUP) → BattleUI textBonus.
 * Offline: chỉ cập nhật cột cup bảng user.
 * Login qua ngày: nếu đang 0 cup thì tặng 1 cup (một lần/ngày).
 */
public final class PvpCupService {
    private PvpCupService() {
    }

    /**
     * Login qua ngày: nếu cup == 0 thì tặng 1 cup (đánh dấu daily để không lặp trong ngày).
     * @return wire bonus đã nhận, hoặc empty nếu không tặng
     */
    public static List<Long> grantDailyFloorIfNeeded(MyUser mUser) {
        if (mUser == null || mUser.getUser() == null || mUser.getUserDaily() == null)
            return Collections.emptyList();
        DataDaily data = mUser.getUserDaily().getUDaily();
        if (data.getValue(DataDaily.GET_CUP_FLOOR) != 0)
            return Collections.emptyList();
        if (mUser.getUser().getCup() > 0) {
            data.setValueAndUpdate(DataDaily.GET_CUP_FLOOR, 1);
            return Collections.emptyList();
        }
        List<Long> wire = Bonus.receiveListItem(mUser, DetailActionType.DAILY_CUP_FLOOR.getKey(), Bonus.viewCup(1));
        if (!wire.isEmpty())
            data.setValueAndUpdate(DataDaily.GET_CUP_FLOOR, 1);
        return wire;
    }

    public static void apply(Player victim, Player killer) {
        if (victim == null || killer == null)
            return;

        MyUser victimUser = victim.getMUser();
        MyUser killerUser = killer.getMUser();
        if (victimUser == null || killerUser == null || victimUser.getUser() == null || killerUser.getUser() == null)
            return;
        if (victimUser.getUserId() == killerUser.getUserId())
            return;

        int victimCup = victimUser.getUser().getCup();
        int killerCup = killerUser.getUser().getCup();
        int amount = CfgUser.calcPvpCupAmount(victimCup, killerCup);
        if (amount <= 0)
            return;

        int maxLoss = Math.max(0, victimCup - CfgUser.getCupFloor());
        int transfer = Math.min(amount, maxLoss);
        if (transfer <= 0)
            return;

        String victimDetail = DetailActionType.PVP_KILL_LOOT.getKey(killerUser.getUserId());
        String killerDetail = DetailActionType.PVP_KILL_LOOT.getKey(victimUser.getUserId());

        grantCup(killerUser, killer, transfer, killerDetail);
        grantCup(victimUser, victim, -transfer, victimDetail);
    }

    static void grantCup(MyUser mUser, Player player, int delta, String detailAction) {
        if (delta == 0)
            return;
        int userId = mUser.getUser().getId();
        if (Online.isOnline(userId)) {
            List<Long> wire = Bonus.receiveListItem(mUser, detailAction, Bonus.viewCup(delta));
            if (!wire.isEmpty() && player != null)
                player.protoStatus(Pbmethod.SubStateType.ADD_BONUS, wire);
            return;
        }
        applyCupOffline(mUser, delta, detailAction);
    }

    static void applyCupOffline(MyUser mUser, int delta, String detailAction) {
        UserEntity user = mUser.getUser();
        int newCup = Math.max(CfgUser.getCupFloor(), user.getCup() + delta);
        if (!DBJPA.update("user", Arrays.asList("cup", newCup), Arrays.asList("id", user.getId())))
            return;
        user.setCup(newCup);
        if (CfgServer.isRealServer())
            Actions.save(user, Actions.GRECEIVE, detailAction, "type", "cup", "value", newCup, "addValue", delta);
    }
}
