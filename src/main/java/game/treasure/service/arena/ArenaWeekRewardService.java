package game.treasure.service.arena;

import com.google.gson.Gson;
import game.config.CfgArena;
import game.config.CfgServer;
import game.config.lang.Lang;
import game.treasure.mapping.TopArenaEntity;
import game.treasure.mapping.UserMailEntity;
import game.treasure.task.dbcache.MailCreatorCache;
import ozudo.base.database.DBJPA;
import ozudo.base.helper.DateTime;
import ozudo.base.helper.StringHelper;
import ozudo.base.log.Logs;

import java.util.ArrayList;
import java.util.List;

/**
 * Chủ nhật 23:55 — top 100 {@code user_week.arena_win} mỗi server → mail thưởng + lưu {@code top_arena}.
 */
public final class ArenaWeekRewardService {
    private static final Gson GSON = new Gson();

    private ArenaWeekRewardService() {
    }

    public static void run() {
        int weekId = DateTime.getNumberWeek();
        List<Integer> servers = CfgServer.serverOpens;
        if (servers == null || servers.isEmpty()) {
            Logs.warn("ArenaWeekReward: no serverOpens");
            return;
        }
        for (Integer serverId : servers) {
            if (serverId == null || serverId <= 0)
                continue;
            try {
                rewardServer(serverId, weekId);
            } catch (Exception ex) {
                Logs.error(ex);
            }
        }
    }

    static void rewardServer(int serverId, int weekId) {
        if (alreadyRewarded(serverId, weekId)) {
            Logs.info("ArenaWeekReward skip exists server=" + serverId + " week=" + weekId);
            return;
        }
        List<Integer> topIds = queryTop100(serverId, weekId);
        // Claim trước (PK) để tránh trả thưởng 2 lần nếu job chạy trùng
        if (!saveTopArena(serverId, weekId, topIds)) {
            Logs.error("ArenaWeekReward save top_arena fail server=" + serverId + " week=" + weekId);
            return;
        }
        if (topIds.isEmpty()) {
            Logs.info("ArenaWeekReward empty top server=" + serverId + " week=" + weekId);
            return;
        }

        String lang = CfgServer.config != null ? CfgServer.config.mainLanguage : Lang.LOCALE_VI;
        String title = Lang.getTitle(lang, Lang.mail_arena_week);
        String sender = Lang.getTitle(lang, Lang.mail_sender_system);
        String msgFmt = Lang.getTitle(lang, Lang.mail_arena_top);

        List<UserMailEntity> mails = new ArrayList<>();
        for (int i = 0; i < topIds.size(); i++) {
            int rank = i + 1;
            List<Long> bonus = CfgArena.weekBonusForRank(rank);
            if (bonus == null || bonus.isEmpty())
                continue;
            int userId = topIds.get(i);
            String message = String.format(msgFmt, rank);
            mails.add(UserMailEntity.builder()
                    .senderId(0)
                    .userId(userId)
                    .senderName(sender)
                    .title(title)
                    .message(message)
                    .bonus(StringHelper.toDBString(bonus))
                    .build()
                    .initDefault());
        }
        if (!mails.isEmpty())
            MailCreatorCache.sendMail(mails);

        Logs.info("ArenaWeekReward done server=" + serverId + " week=" + weekId + " size=" + topIds.size()
                + " mails=" + mails.size());
    }

    static boolean alreadyRewarded(int serverId, int weekId) {
        return DBJPA.count("top_arena", "server_id", serverId, "week", weekId) > 0;
    }

    static List<Integer> queryTop100(int serverId, int weekId) {
        List<Integer> out = new ArrayList<>();
        String sql = "SELECT user_id FROM dson.user_week WHERE server=" + serverId
                + " AND week_id=" + weekId
                + " AND arena_win > 0 ORDER BY arena_win DESC LIMIT 100";
        List<?> rows = DBJPA.getList(sql);
        if (rows == null)
            return out;
        for (Object row : rows) {
            int userId;
            if (row instanceof Object[])
                userId = ((Number) ((Object[]) row)[0]).intValue();
            else if (row instanceof Number)
                userId = ((Number) row).intValue();
            else
                continue;
            out.add(userId);
        }
        return out;
    }

    static boolean saveTopArena(int serverId, int weekId, List<Integer> topIds) {
        TopArenaEntity entity = new TopArenaEntity(serverId, weekId, GSON.toJson(topIds));
        return DBJPA.save(entity);
    }
}
