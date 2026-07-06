package game.treasure.service.trading;

import game.treasure.mapping.UserTradingEntity;
import ozudo.base.log.Logs;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/** Cache listing chợ theo server — sync khi đăng / hủy / mua. */
public class TradingMarketCache {
    private static final Map<Integer, List<UserTradingEntity>> byServer = new ConcurrentHashMap<>();

    public static void init() {
        byServer.clear();
        try {
            List<UserTradingEntity> all = ozudo.base.database.DBJPA.getList(
                    "user_trading", UserTradingEntity.class);
            if (all == null)
                return;
            for (UserTradingEntity row : all) {
                byServer.computeIfAbsent(row.getServer(), k -> Collections.synchronizedList(new ArrayList<>()))
                        .add(row);
            }
        } catch (Exception ex) {
            Logs.error(ex);
        }
    }

    public static void add(UserTradingEntity row) {
        if (row == null)
            return;
        byServer.computeIfAbsent(row.getServer(), k -> Collections.synchronizedList(new ArrayList<>()))
                .add(row);
    }

    public static void remove(UserTradingEntity row) {
        if (row == null)
            return;
        List<UserTradingEntity> list = byServer.get(row.getServer());
        if (list != null)
            list.removeIf(r -> r.getId() == row.getId());
    }

    public static UserTradingEntity get(long tradingId) {
        for (List<UserTradingEntity> list : byServer.values()) {
            for (UserTradingEntity row : list) {
                if (row.getId() == tradingId)
                    return row;
            }
        }
        UserTradingEntity db = UserTradingEntity.getById(tradingId);
        if (db != null)
            add(db);
        return db;
    }

    public static List<UserTradingEntity> list(int server, int tab, int userIdFilter, int page) {
        List<UserTradingEntity> src = byServer.getOrDefault(server, Collections.emptyList());
        List<UserTradingEntity> filtered = src.stream()
                .filter(r -> r.getTab() == tab)
                .filter(r -> userIdFilter <= 0 || r.getUserId() == userIdFilter)
                .sorted(Comparator.comparingLong(UserTradingEntity::getId))
                .collect(Collectors.toList());
        int from = page * game.config.CfgTrading.PAGE_SIZE;
        if (from >= filtered.size())
            return Collections.emptyList();
        int to = Math.min(from + game.config.CfgTrading.PAGE_SIZE, filtered.size());
        return new ArrayList<>(filtered.subList(from, to));
    }

    public static int count(int server, int tab, int userIdFilter) {
        List<UserTradingEntity> src = byServer.getOrDefault(server, Collections.emptyList());
        return (int) src.stream()
                .filter(r -> r.getTab() == tab)
                .filter(r -> userIdFilter <= 0 || r.getUserId() == userIdFilter)
                .count();
    }
}
