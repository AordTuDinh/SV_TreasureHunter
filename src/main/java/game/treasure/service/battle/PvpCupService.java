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
import java.util.List;

/**
 * Chuyển cup khi player hạ player khác.
 * Online: ADD_BONUS (BONUS_CUP) → BattleUI textBonus.
 * Offline: chỉ cập nhật cột cup bảng user.
 * Xong hết nhiệm vụ ngày: +1 cup, một lần mỗi ngày.
 */
public final class PvpCupService {
    /** Nạn nhân thấp hơn người giết từ mức này thì không cướp, không trừ. */
    public static final int CUP_GAP_BLOCK = 50;
    static final int PVP_CUP_DELTA = 1;

    private PvpCupService() {
    }

    /**
     * Khi mọi nhiệm vụ ngày đã đủ chỉ tiêu: +1 cup, một lần trong ngày.
     */
    public static void tryGrantDailyQuestCup(MyUser mUser) {
        if (mUser == null || mUser.getUser() == null || mUser.getUserDaily() == null)
            return;
        DataDaily data = mUser.getUserDaily().getUDaily();
        if (data == null || data.getValue(DataDaily.GET_CUP_FLOOR) != 0)
            return;
        if (!allDailyQuestsCompleted(mUser.getUQuest()))
            return;

        List<Long> wire = Bonus.receiveListItem(mUser, DetailActionType.DAILY_QUEST_CUP.getKey(), Bonus.viewCup(1));
        if (wire.isEmpty())
            return;
        data.setValueAndUpdate(DataDaily.GET_CUP_FLOOR, 1);
        Player player = mUser.getPlayer();
        if (player != null)
            player.protoStatus(Pbmethod.SubStateType.ADD_BONUS, wire);
    }

    static boolean allDailyQuestsCompleted(game.treasure.mapping.UserQuestEntity uQuest) {
        if (uQuest == null || uQuest.getDataQuest() == null)
            return false;
        List<Integer> quests = uQuest.getQuest();
        if (quests == null || quests.size() < 2)
            return false;
        game.object.DataQuest data = uQuest.getDataQuest();
        for (int i = 0; i < quests.size(); i += 2) {
            if (quests.get(i + 1) == game.config.aEnum.StatusType.DONE.value)
                continue;
            game.treasure.mapping.main.ResQuestEntity quest = game.treasure.service.resource.ResQuest.mQuest.get(quests.get(i));
            if (quest == null || data.getValue(quest.getId()) < quest.getNumber())
                return false;
        }
        return true;
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
        if (victimCup <= 0)
            return;
        if (killerCup - victimCup >= CUP_GAP_BLOCK)
            return;

        int maxLoss = Math.max(0, victimCup - CfgUser.getCupFloor());
        int transfer = Math.min(PVP_CUP_DELTA, maxLoss);
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
