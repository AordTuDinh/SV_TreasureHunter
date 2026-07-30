package game.treasure.service.day;

import game.object.MyUser;
import game.protocol.CommonProto;
import game.treasure.server.IAction;
import lombok.Getter;
import ozudo.base.helper.Util;
import protocol.Pbmethod;

import java.util.ArrayList;
import java.util.List;

/** Ngữ cảnh chạy pipeline qua ngày — handler có thể queue sync im lặng xuống client. */
public final class ServerDayContext {
    @Getter
    private final MyUser mUser;
    private final List<Long> privateBonusWire = new ArrayList<>();

    public ServerDayContext(MyUser mUser) {
        this.mUser = mUser;
    }

    /** Gom wire {@link IAction#UPDATE_BONUS_PRIVATE} — flush một lần cuối pipeline. */
    public void queuePrivateBonus(List<Long> wire) {
        if (wire != null && !wire.isEmpty())
            privateBonusWire.addAll(wire);
    }

    public void flushPrivateBonusToClient() {
        if (mUser == null || privateBonusWire.isEmpty() || mUser.getChannel() == null)
            return;
        Pbmethod.CommonVector pb = CommonProto.getCommonVector(privateBonusWire);
        Util.sendProtoData(mUser.getChannel(), pb, IAction.UPDATE_BONUS_PRIVATE);
        privateBonusWire.clear();
    }
}
