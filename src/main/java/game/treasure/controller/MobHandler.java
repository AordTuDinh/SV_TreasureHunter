package game.treasure.controller;

import game.battle.model.Enemy;
import game.battle.model.Player;
import game.battle.object.Pos;
import game.config.lang.Lang;
import game.object.MyUser;
import game.treasure.mapping.UserMobEntity;
import game.treasure.mapping.main.ResMobEntity;
import game.treasure.server.IAction;
import game.treasure.service.resource.ResMob;
import game.treasure.service.user.Bonus;
import game.treasure.table.BaseRoom;
import io.netty.channel.Channel;
import ozudo.base.log.Logs;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class MobHandler extends AHandler {
    static MobHandler instance;

    public static MobHandler getInstance() {
        if (instance == null)
            instance = new MobHandler();
        return instance;
    }

    @Override
    public void initAction(Map<Integer, AHandler> mHandler) {
        mHandler.put(IAction.MOB_USE, this);
    }

    @Override
    public AHandler newInstance() {
        return new MobHandler();
    }

    @Override
    public void handle(Channel channel, String session, int actionId, byte[] requestData) {
        super.handle(channel, session, actionId, requestData);
        try {
            if (actionId == IAction.MOB_USE)
                useMob();
        } catch (Exception ex) {
            Logs.error(ex);
        }
    }

    private void useMob() {
        List<Long> inputs = getInputALong();
        if (inputs.isEmpty()) {
            addErrParam();
            return;
        }
        long rowId = inputs.get(0);
        UserMobEntity uMob = mUser.getResources().getMob(rowId);
        if (uMob == null) {
            addErrResponse(getLang(Lang.item_not_own));
            return;
        }
        ResMobEntity res = ResMob.getMob(uMob.getMobId());
        if (res == null) {
            addErrResponse(getLang(Lang.err_params));
            return;
        }
        Player player = mUser.getPlayer();
        if (player == null || player.getRoom() == null) {
            addErrResponse(getLang(Lang.err_params));
            return;
        }
        BaseRoom room = player.getRoom();
        int mobId = uMob.getMobId();
        int tier = uMob.getTier();
        if (!removeUserMob(mUser, uMob))
            return;

        Pos posInit = Pos.randomPos(player.getPos(), 1f, 1f);
        Enemy enemy = new Enemy(mobId, player, posInit, tier);
        room.addUnit(enemy);

        List<Long> removed = Arrays.asList(
                (long) Bonus.BONUS_MOB, rowId, (long) -mobId, (long) tier);
        addResponse(getCommonVector(removed));
    }

    static boolean removeUserMob(MyUser mUser, UserMobEntity uMob) {
        long rowId = uMob.getId();
        Bonus.clearItemFromSlot(mUser, Bonus.BONUS_MOB, rowId);
        if (!uMob.deleteFromDb())
            return false;
        mUser.getResources().removeMob(rowId);
        return true;
    }
}
