package game.monitor;

import game.config.CfgServer;
import game.treasure.mapping.main.ConfigEntity;
import ozudo.base.database.DBJPA;

public class MaintenanceChecker {
    private static volatile boolean cachedActive = false;
    private static volatile String cachedMipMessage = "";
    private static volatile long lastFetchMs = 0;
    private static final long TTL_MS = 10_000;

    private MaintenanceChecker() {
    }

    public static boolean isMaintenance() {
        refreshIfNeeded();
        return cachedActive;
    }

    public static String getMipMessage() {
        refreshIfNeeded();
        return cachedMipMessage;
    }

    public static void invalidate() {
        lastFetchMs = 0;
    }

    private static void refreshIfNeeded() {
        long now = System.currentTimeMillis();
        if (now - lastFetchMs < TTL_MS) {
            return;
        }
        synchronized (MaintenanceChecker.class) {
            now = System.currentTimeMillis();
            if (now - lastFetchMs < TTL_MS) {
                return;
            }
            try {
                ConfigEntity mip = (ConfigEntity) DBJPA.getUnique(
                        CfgServer.DB_MAIN + "config_api", ConfigEntity.class, "k", "maintenance_in_progress");
                cachedActive = mip != null && "1".equals(mip.getV().trim());

                ConfigEntity msg = (ConfigEntity) DBJPA.getUnique(
                        CfgServer.DB_MAIN + "config_api", ConfigEntity.class, "k", "mip_message");
                cachedMipMessage = msg != null && msg.getV() != null ? msg.getV().trim() : "";
            } catch (Exception e) {
                cachedActive = false;
                cachedMipMessage = "";
            }
            lastFetchMs = System.currentTimeMillis();
        }
    }
}
