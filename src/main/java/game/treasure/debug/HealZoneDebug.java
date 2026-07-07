package game.treasure.debug;

import ozudo.base.log.Logs;

public final class HealZoneDebug {
    public static final boolean ENABLED = true;

    private HealZoneDebug() {
    }

    public static void log(String msg) {
        if (ENABLED) Logs.debug("[HealZone] " + msg);
    }
}
