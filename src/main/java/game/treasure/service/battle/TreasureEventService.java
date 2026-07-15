package game.treasure.service.battle;

import game.battle.model.CellObject;
import game.battle.model.Player;
import game.battle.object.Pos;
import game.config.CfgTreasure;
import game.config.aEnum.DetailActionType;
import game.monitor.Online;
import game.object.MyUser;
import game.protocol.CommonProto;
import game.treasure.server.IAction;
import game.treasure.service.user.Bonus;
import ozudo.base.helper.NumberUtil;
import ozudo.base.helper.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Chìa khóa + rương theo {@code user.server} (ServerObject id).
 * Runtime only — chìa không ghi DB túi.
 * Key/chest state broadcast toàn server (IAction 59/60).
 */
public final class TreasureEventService {
    /** rowId giả khi sell/bag sync runtime key. */
    public static final long KEY_RUNTIME_ROW_ID = -9L;

    private static final ConcurrentHashMap<Integer, ServerTreasureState> BY_SERVER = new ConcurrentHashMap<>();

    private TreasureEventService() {
    }

    public static boolean isRuntimeKeySell(long rowId) {
        return rowId == KEY_RUNTIME_ROW_ID;
    }

    public static boolean holderHasKey(MyUser mUser) {
        if (mUser == null || mUser.getUser() == null) return false;
        ServerTreasureState st = BY_SERVER.get(mUser.getUser().getServer());
        return st != null && st.holderUserId == mUser.getUserId() && st.keyExpireAt > System.currentTimeMillis();
    }

    /**
     * Hụt hết drop thường → roll rương; chìa chỉ roll khi đã có rương (config phần nghìn).
     */
    public static void tryDropOnFullMiss(Player player, CellObject cell) {
        if (player == null || player.getMUser() == null || cell == null || cell.getPos() == null) return;
        MyUser mUser = player.getMUser();
        int serverId = mUser.getUser().getServer();
        ServerTreasureState st = BY_SERVER.computeIfAbsent(serverId, id -> new ServerTreasureState());
        synchronized (st) {
            expireIfNeeded(serverId, st);
            if (!hasChest(st) && NumberUtil.getRandom(1000) < CfgTreasure.rateDropChest()) {
                spawnChest(player, st, cell, serverId);
                return;
            }
            if (hasChest(st) && !hasKey(st) && !player.isProtected()
                    && NumberUtil.getRandom(1000) < CfgTreasure.rateDropKey()) {
                grantKey(player, st, cell.getPos(), serverId);
            }
        }
    }

    public static void tickPlayer(Player player) {
        if (player == null || player.getMUser() == null || !player.isAlive()) return;
        MyUser mUser = player.getMUser();
        int serverId = mUser.getUser().getServer();
        ServerTreasureState st = BY_SERVER.get(serverId);
        if (st == null) return;
        synchronized (st) {
            expireIfNeeded(serverId, st);
            tickKeyHolderPos(player, st, serverId);
            tickOpenChannel(player, st, serverId);
        }
    }

    /** Đang channel mở rương (đã qua idle 0.5s, đang đếm openChannel). */
    public static boolean isOpening(Player player) {
        if (player == null || player.getMUser() == null || player.getMUser().getUser() == null)
            return false;
        ServerTreasureState st = BY_SERVER.get(player.getMUser().getUser().getServer());
        if (st == null) return false;
        synchronized (st) {
            return st.openingUserId == player.getMUser().getUserId() && st.openStartAt > 0;
        }
    }

    /**
     * Ưu tiên mở rương: holder đứng cùng ô chest + có chìa → chặn attack.
     * (dwell 0.5s + channel 10s đều nằm trong điều kiện này)
     */
    public static boolean blocksAttackForTreasureOpen(Player player) {
        if (player == null || player.getMUser() == null || player.getPos() == null
                || player.getMUser().getUser() == null)
            return false;
        ServerTreasureState st = BY_SERVER.get(player.getMUser().getUser().getServer());
        if (st == null) return false;
        synchronized (st) {
            if (!hasChest(st) || !hasKey(st) || st.holderUserId != player.getMUser().getUserId())
                return false;
            return sameChestCell(player, st);
        }
    }

    /**
     * Holder đổi ô → broadcast chìa; chỉ hủy dwell/opening khi đã rời ô rương.
     * MOVE trong cùng ô (đứng yên / đánh object) không hủy.
     */
    public static void onPlayerMoved(Player player) {
        if (player == null || player.getMUser() == null || player.getMUser().getUser() == null
                || player.getPos() == null)
            return;
        int serverId = player.getMUser().getUser().getServer();
        ServerTreasureState st = BY_SERVER.get(serverId);
        if (st == null) return;
        synchronized (st) {
            tickKeyHolderPos(player, st, serverId);
            // Chỉ hủy khi rời cell — attack / jitter cùng ô vẫn giữ opening
            if (hasChest(st) && !sameChestCell(player, st))
                resetOpenProgress(player, st);
        }
    }

    /** @deprecated giữ tên cũ. */
    public static void onPlayerAttackOrMoveCancel(Player player) {
        onPlayerMoved(player);
    }

    public static void transferKeyOnPvpKill(Player victim, Player killer) {
        if (victim == null || killer == null || victim.getMUser() == null || killer.getMUser() == null) return;
        int serverId = victim.getMUser().getUser().getServer();
        ServerTreasureState st = BY_SERVER.get(serverId);
        if (st == null) return;
        synchronized (st) {
            if (st.holderUserId != victim.getMUser().getUserId() || !hasKey(st)) return;
            cancelOpening(victim, st);
            // Không broadcast false trước — grantKey sẽ broadcast holder mới
            st.holderUserId = 0;
            st.keyExpireAt = 0;
            st.keyPosX = 0;
            st.keyPosY = 0;
            Pos pos = killer.getPos() != null ? killer.getPos() : victim.getPos();
            grantKey(killer, st, pos != null ? pos : Pos.zero(), serverId);
        }
    }

    public static void clearKeyOnLogout(MyUser mUser) {
        if (mUser == null || mUser.getUser() == null) return;
        int serverId = mUser.getUser().getServer();
        ServerTreasureState st = BY_SERVER.get(serverId);
        if (st == null) return;
        synchronized (st) {
            if (st.holderUserId != mUser.getUserId()) return;
            if (mUser.getPlayer() != null) cancelOpening(mUser.getPlayer(), st);
            clearKeyFields(st);
            broadcastKey(serverId, false, 0, 0, 0, 0);
        }
    }

    public static boolean clearKeyForSell(MyUser mUser) {
        if (mUser == null || mUser.getUser() == null) return false;
        int serverId = mUser.getUser().getServer();
        ServerTreasureState st = BY_SERVER.get(serverId);
        if (st == null) return false;
        synchronized (st) {
            if (st.holderUserId != mUser.getUserId() || !hasKey(st)) return false;
            if (mUser.getPlayer() != null) cancelOpening(mUser.getPlayer(), st);
            clearKeyFields(st);
            broadcastKey(serverId, false, 0, 0, 0, 0);
            return true;
        }
    }

    public static boolean sellKey(MyUser mUser) {
        return clearKeyForSell(mUser);
    }

    /** Sync state khi join map — gửi đủ key+chest cho player vừa vào. */
    public static void syncOnJoin(Player player) {
        if (player == null || player.getMUser() == null) return;
        MyUser mUser = player.getMUser();
        int serverId = mUser.getUser().getServer();
        ServerTreasureState st = BY_SERVER.get(serverId);
        if (st == null) {
            sendKeyStateToUser(mUser, false, 0, 0, 0, 0);
            sendChestStateToUser(mUser, false, 0, 0, 0, 0);
            return;
        }
        synchronized (st) {
            expireIfNeeded(serverId, st);
            if (hasKey(st)) {
                sendKeyStateToUser(mUser, true, remainMs(st.keyExpireAt),
                        cellOf(st.keyPosX), cellOf(st.keyPosY), st.holderUserId);
            } else {
                sendKeyStateToUser(mUser, false, 0, 0, 0, 0);
            }
            if (hasChest(st)) {
                sendChestStateToUser(mUser, true, cellOf(st.chestPosX), cellOf(st.chestPosY),
                        remainMs(st.chestExpireAt), st.chestGlobalCellId);
            } else {
                sendChestStateToUser(mUser, false, 0, 0, 0, 0);
            }
        }
    }

    /** Spawn rương đúng ô cell vừa phá (góc lưới + globalCellId). */
    private static void spawnChest(Player player, ServerTreasureState st, CellObject cell, int serverId) {
        int x = cellOf(cell.getPos().getX());
        int y = cellOf(cell.getPos().getY());
        st.chestPosX = x;
        st.chestPosY = y;
        st.chestGlobalCellId = cell.getId();
        st.chestExpireAt = System.currentTimeMillis() + CfgTreasure.chestTtlMs();
        st.openingUserId = 0;
        st.openStartAt = 0;
        st.dwellUserId = 0;
        st.dwellStartAt = 0;
        st.lastSentRemainSec = -1;
        broadcastChest(serverId, true, x, y, remainMs(st.chestExpireAt), st.chestGlobalCellId);
        String msg = "Người chơi " + player.getName() + " vừa phát hiện rương tại tọa độ (" + x + "," + y + ")";
        Util.sendSliderChat(Online.getUserInServer(serverId), msg);
    }

    private static void grantKey(Player player, ServerTreasureState st, Pos dropPos, int serverId) {
        MyUser mUser = player.getMUser();
        st.holderUserId = mUser.getUserId();
        st.keyPosX = dropPos.getX();
        st.keyPosY = dropPos.getY();
        st.keyExpireAt = System.currentTimeMillis() + CfgTreasure.keyTtlMs();
        st.openingUserId = 0;
        st.openStartAt = 0;
        st.dwellUserId = 0;
        st.dwellStartAt = 0;
        st.lastSentRemainSec = -1;
        int x = cellOf(st.keyPosX);
        int y = cellOf(st.keyPosY);
        broadcastKey(serverId, true, remainMs(st.keyExpireAt), x, y, st.holderUserId);
        String msg = "Người chơi " + player.getName() + " vừa phát hiện chìa khóa tại tọa độ (" + x + "," + y + ")";
        Util.sendSliderChat(Online.getUserInServer(serverId), msg);
    }

    /** Holder đổi ô floor → broadcast pos chìa. */
    private static void tickKeyHolderPos(Player player, ServerTreasureState st, int serverId) {
        if (player.getPos() == null || !hasKey(st) || st.holderUserId != player.getMUser().getUserId())
            return;
        int x = cellOf(player.getPos().getX());
        int y = cellOf(player.getPos().getY());
        int prevX = cellOf(st.keyPosX);
        int prevY = cellOf(st.keyPosY);
        if (x == prevX && y == prevY)
            return;
        st.keyPosX = player.getPos().getX();
        st.keyPosY = player.getPos().getY();
        broadcastKey(serverId, true, remainMs(st.keyExpireAt), x, y, st.holderUserId);
    }

    private static final long IDLE_BEFORE_OPEN_MS = 500L;

    private static void tickOpenChannel(Player player, ServerTreasureState st, int serverId) {
        MyUser mUser = player.getMUser();
        if (!hasChest(st) || st.holderUserId != mUser.getUserId() || !hasKey(st)) {
            resetOpenProgress(player, st);
            return;
        }
        if (player.getPos() == null)
            return;

        long now = System.currentTimeMillis();
        if (!sameChestCell(player, st)) {
            resetOpenProgress(player, st);
            return;
        }

        // Phase 1: đứng cùng ô ≥ 0.5s rồi mới bắt đầu mở
        if (st.openStartAt <= 0 || st.openingUserId != mUser.getUserId()) {
            if (st.dwellStartAt <= 0 || st.dwellUserId != mUser.getUserId()) {
                st.dwellUserId = mUser.getUserId();
                st.dwellStartAt = now;
                return;
            }
            if (now - st.dwellStartAt < IDLE_BEFORE_OPEN_MS)
                return;

            st.openingUserId = mUser.getUserId();
            st.openStartAt = now;
            st.lastSentRemainSec = -1;
            long need = CfgTreasure.openChannelMs();
            sendOpening(mUser, true, need);
            st.lastSentRemainSec = (int) Math.ceil(need / 1000.0);
            return;
        }

        // Phase 2: phải đứng liên tục đủ openChannelMs
        long need = CfgTreasure.openChannelMs();
        long elapsed = now - st.openStartAt;
        if (elapsed >= need) {
            completeOpen(player, st, serverId);
        } else {
            sendOpeningProgress(mUser, st, need - elapsed);
        }
    }

    /** Cùng ô floor với rương (khớp MapService). */
    private static boolean sameChestCell(Player player, ServerTreasureState st) {
        if (player == null || player.getPos() == null || !hasChest(st))
            return false;
        return cellOf(player.getPos().getX()) == cellOf(st.chestPosX)
                && cellOf(player.getPos().getY()) == cellOf(st.chestPosY);
    }

    /** Hủy dwell + opening (rời ô / mất key). */
    private static void resetOpenProgress(Player player, ServerTreasureState st) {
        boolean wasOpening = st.openStartAt > 0 && st.openingUserId > 0;
        st.dwellUserId = 0;
        st.dwellStartAt = 0;
        if (wasOpening && player != null && player.getMUser() != null
                && st.openingUserId == player.getMUser().getUserId()) {
            cancelOpening(player, st);
        } else {
            st.openingUserId = 0;
            st.openStartAt = 0;
            st.lastSentRemainSec = -1;
        }
    }

    private static void completeOpen(Player player, ServerTreasureState st, int serverId) {
        MyUser mUser = player.getMUser();
        List<Long> reward = rollOpenReward();
        String name = player.getName() != null ? player.getName() : "Người chơi";
        clearKeyFields(st);
        st.chestExpireAt = 0;
        st.chestPosX = 0;
        st.chestPosY = 0;
        st.chestGlobalCellId = 0;
        st.openingUserId = 0;
        st.openStartAt = 0;
        st.dwellUserId = 0;
        st.dwellStartAt = 0;
        st.lastSentRemainSec = -1;
        sendOpening(mUser, false, 0);
        broadcastKey(serverId, false, 0, 0, 0, 0);
        broadcastChest(serverId, false, 0, 0, 0, 0);
        Util.sendSliderChat(Online.getUserInServer(serverId),
                "Chúc mừng " + name + " đã mở rương thành công");

        debugTreasure(mUser, "[TreasureOpen] completeOpen user=" + mUser.getUserId()
                + " rewardRaw=" + reward);

        List<Long> toGrant = resolveOpenReward(mUser, reward);
        debugTreasure(mUser, "[TreasureOpen] toGrant=" + toGrant);

        List<Long> applied = new ArrayList<>();
        if (toGrant != null && !toGrant.isEmpty()) {
            applied = Bonus.receiveListItem(mUser,
                    DetailActionType.TREASURE_CHEST_OPEN.getKey(), toGrant);
            debugTreasure(mUser, "[TreasureOpen] applied size=" + applied.size() + " data=" + applied);
        }
        // Id material lạ / apply fail khác → vẫn quy đổi gem 20–80
        if (applied.isEmpty()) {
            List<Long> gem = rollGemReward();
            applied = Bonus.receiveListItem(mUser,
                    DetailActionType.TREASURE_CHEST_OPEN.getKey(), gem);
            debugTreasure(mUser, "[TreasureOpen] applyFail→gem size=" + applied.size() + " data=" + applied);
        }

        if (applied.isEmpty()) {
            debugTreasure(mUser, "[TreasureOpen] FAIL applied empty — không gửi BONUS_TOAST");
            return;
        }
        if (mUser.getChannel() == null) {
            debugTreasure(mUser, "[TreasureOpen] FAIL channel null");
            return;
        }
        Util.sendProtoData(mUser.getChannel(),
                CommonProto.getCommonVector(applied),
                IAction.BONUS_TOAST);
        debugTreasure(mUser, "[TreasureOpen] sent BONUS_TOAST=" + IAction.BONUS_TOAST
                + " payload=" + applied);
    }

    private static void debugTreasure(MyUser mUser, String msg) {
        System.out.println(msg);
        if (mUser != null && mUser.getChannel() != null) {
            Util.sendProtoData(mUser.getChannel(),
                    CommonProto.getCommonVector(msg),
                    IAction.DEBUG_LOG);
        }
    }

    /**
     * Material mà túi nguyên liệu đầy → quy đổi gem random [openGemMin..openGemMax] (20–80).
     */
    private static List<Long> resolveOpenReward(MyUser mUser, List<Long> reward) {
        if (reward == null || reward.isEmpty())
            return rollGemReward();
        if (reward.get(0).intValue() == Bonus.BONUS_MATERIAL) {
            if (mUser == null || mUser.getResources() == null || !mUser.getResources().canAddMaterial(1)) {
                List<Long> gem = rollGemReward();
                debugTreasure(mUser, "[TreasureOpen] túi nguyên liệu đầy → quy đổi gem=" + gem);
                return gem;
            }
        }
        return reward;
    }

    private static List<Long> rollGemReward() {
        int gem = NumberUtil.getRandom(CfgTreasure.openGemMin(), CfgTreasure.openGemMax());
        return Bonus.viewGem(gem);
    }

    private static List<Long> rollOpenReward() {
        if (NumberUtil.getRandom(100) < 50) {
            return rollGemReward();
        }
        int materialId = NumberUtil.getRandom(CfgTreasure.openMaterialIdMin(), CfgTreasure.openMaterialIdMax());
        return Bonus.viewMaterial(materialId, CfgTreasure.openMaterialTier());
    }

    private static void expireIfNeeded(int serverId, ServerTreasureState st) {
        long now = System.currentTimeMillis();
        if (st.chestExpireAt > 0 && now >= st.chestExpireAt) {
            clearChestAndKey(serverId, st);
            return;
        }
        if (st.keyExpireAt > 0 && now >= st.keyExpireAt) {
            clearKeyOnly(serverId, st);
        }
    }

    private static void clearChestAndKey(int serverId, ServerTreasureState st) {
        MyUser holder = findHolderOnline(st.holderUserId);
        if (holder != null && holder.getPlayer() != null)
            cancelOpening(holder.getPlayer(), st);
        clearKeyFields(st);
        st.chestExpireAt = 0;
        st.chestPosX = 0;
        st.chestPosY = 0;
        st.chestGlobalCellId = 0;
        st.openingUserId = 0;
        st.openStartAt = 0;
        st.dwellUserId = 0;
        st.dwellStartAt = 0;
        broadcastKey(serverId, false, 0, 0, 0, 0);
        broadcastChest(serverId, false, 0, 0, 0, 0);
    }

    private static void clearKeyOnly(int serverId, ServerTreasureState st) {
        MyUser holder = findHolderOnline(st.holderUserId);
        if (holder != null && holder.getPlayer() != null)
            cancelOpening(holder.getPlayer(), st);
        clearKeyFields(st);
        st.openingUserId = 0;
        st.openStartAt = 0;
        st.dwellUserId = 0;
        st.dwellStartAt = 0;
        broadcastKey(serverId, false, 0, 0, 0, 0);
    }

    private static void clearKeyFields(ServerTreasureState st) {
        st.holderUserId = 0;
        st.keyExpireAt = 0;
        st.keyPosX = 0;
        st.keyPosY = 0;
    }

    private static void cancelOpening(Player player, ServerTreasureState st) {
        st.openingUserId = 0;
        st.openStartAt = 0;
        st.dwellUserId = 0;
        st.dwellStartAt = 0;
        st.lastSentRemainSec = -1;
        if (player != null && player.getMUser() != null) {
            sendOpening(player.getMUser(), false, 0);
        }
    }

    private static int cellOf(float world) {
        return (int) Math.floor(world);
    }

    private static boolean hasChest(ServerTreasureState st) {
        return st.chestExpireAt > System.currentTimeMillis();
    }

    private static boolean hasKey(ServerTreasureState st) {
        return st.keyExpireAt > System.currentTimeMillis() && st.holderUserId > 0;
    }

    private static long remainMs(long expireAt) {
        return Math.max(0, expireAt - System.currentTimeMillis());
    }

    private static MyUser findHolderOnline(int userId) {
        if (userId <= 0) return null;
        for (io.netty.channel.Channel ch : Online.getAllChanel()) {
            Object o = ozudo.base.helper.ChUtil.get(ch, ozudo.base.helper.ChUtil.KEY_M_USER);
            if (o instanceof MyUser m && m.getUserId() == userId) return m;
        }
        return null;
    }

    /**
     * TREASURE_KEY_STATE: [hasKey, remainMs, itemId, rowId, posX, posY, holderUserId]
     * Broadcast toàn server.
     */
    private static void broadcastKey(int serverId, boolean hasKey, long remainMs, int posX, int posY, int holderUserId) {
        Util.sendProtoDataToListChanel(Online.getUserInServer(serverId),
                CommonProto.getCommonVector(
                        hasKey ? 1L : 0L,
                        remainMs,
                        (long) CfgTreasure.keyItemId(),
                        KEY_RUNTIME_ROW_ID,
                        (long) posX,
                        (long) posY,
                        (long) holderUserId),
                IAction.TREASURE_KEY_STATE);
    }

    private static void sendKeyStateToUser(MyUser mUser, boolean hasKey, long remainMs,
                                          int posX, int posY, int holderUserId) {
        if (mUser == null || mUser.getChannel() == null) return;
        Util.sendProtoData(mUser.getChannel(),
                CommonProto.getCommonVector(
                        hasKey ? 1L : 0L,
                        remainMs,
                        (long) CfgTreasure.keyItemId(),
                        KEY_RUNTIME_ROW_ID,
                        (long) posX,
                        (long) posY,
                        (long) holderUserId),
                IAction.TREASURE_KEY_STATE);
    }

    /** [hasChest, posX, posY, remainMs, globalCellId] */
    private static void broadcastChest(int serverId, boolean has, int x, int y, long remainMs, int globalCellId) {
        Util.sendProtoDataToListChanel(Online.getUserInServer(serverId),
                CommonProto.getCommonVector(has ? 1L : 0L, (long) x, (long) y, remainMs, (long) globalCellId),
                IAction.TREASURE_CHEST_STATE);
    }

    private static void sendChestStateToUser(MyUser mUser, boolean has, int x, int y, long remainMs, int globalCellId) {
        if (mUser == null || mUser.getChannel() == null) return;
        Util.sendProtoData(mUser.getChannel(),
                CommonProto.getCommonVector(has ? 1L : 0L, (long) x, (long) y, remainMs, (long) globalCellId),
                IAction.TREASURE_CHEST_STATE);
    }

    private static void sendOpeningProgress(MyUser mUser, ServerTreasureState st, long remainMs) {
        int remainSec = (int) Math.ceil(Math.max(0, remainMs) / 1000.0);
        if (remainSec == st.lastSentRemainSec)
            return;
        st.lastSentRemainSec = remainSec;
        sendOpening(mUser, true, remainMs);
    }

    private static void sendOpening(MyUser mUser, boolean opening, long remainMs) {
        if (mUser == null || mUser.getChannel() == null) return;
        Util.sendProtoData(mUser.getChannel(),
                CommonProto.getCommonVector(opening ? 1L : 0L, remainMs),
                IAction.TREASURE_OPENING);
    }

    static final class ServerTreasureState {
        int holderUserId;
        float keyPosX;
        float keyPosY;
        long keyExpireAt;
        float chestPosX;
        float chestPosY;
        /** globalCellId ô rương — client snap đúng CellObject. */
        int chestGlobalCellId;
        long chestExpireAt;
        int openingUserId;
        long openStartAt;
        /** Đứng cùng ô trước khi bắt đầu channel. */
        int dwellUserId;
        long dwellStartAt;
        int lastSentRemainSec = -1;
    }
}
