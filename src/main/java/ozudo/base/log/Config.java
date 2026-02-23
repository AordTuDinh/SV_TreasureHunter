package ozudo.base.log;

public class Config {
    public final static String SERVER_PORT = "config.server.port";
    public final static String SERVER_WELCOME = "config.server.welcome";
    public final static String SERVER_IDLE = "config.server.idle";
    public final static String REMOTE_CONNECT_TIMEOUT = "config.server.remoteConnectTimeout";
    public final static String REMOTE_TIMEOUT = "config.server.remoteTimeout";
    public final static String REMOTE_TIMEOUT_SHORT = "config.server.remoteTimeoutShort";
    public final static String SESSION_CLIENT_TIMEOUT = "config.client.timeout";
    public final static String MAX_CLIENTS = "config.client.maxClients";
    public final static String LINE_DELIMITER = "config.client.lineDelimiter";
    public final static String CACHE_MAX = "config.cache.maxcache";
    public final static String CACHE_TIME = "config.cache.time";

    public final static String CACHE_SERVER = "config.cache.server";
    public final static String CACHE_PORT = "config.cache.port";
    public final static String CACHE_REDIS = "config.cache.redis";

    public final static String MONITOR_KOIN = "config.monitor.koin";
    public final static String MONITOR_USERONLINE = "config.monitor.useronline";
    public final static String TUVI_CONFIG = "config_tuvi";
    public final static String SERVER_ID = "config.server.id";
    public static int idRoomBar = 0, idRoomPark = 0;
    /**
     * Internal variable to hold the properties
     */
    private static slib_Properties mProps;

    /*
     * Override methods
     */

    /**
     * This method loads properties from specify configuration file. It must be called before any getXXX() method is called.
     *
     * @param aSource full path of configuration file
     * @throws Exception
     */
    public static void load(Object aSource) throws Exception {
        String filename = (String) aSource;
        mProps = new slib_Properties(filename);
    }

    public static String getConfigPath() {
        return ""; // it is the same folder of app
    }

    public static boolean getBoolean(String aKey) {
        try {
            return mProps.getBoolean(aKey);
        } catch (Exception ex) {

        }
        return false;
    }

    public static int getInt(String aKey) {
        return mProps.getInt(aKey);
    }

    public static String getString(String aKey) {
        return mProps.getString(aKey);
    }

    public static int getServerPort() {
        return getInt(SERVER_PORT);
    }

    public static String getStringServerPort() {
        return getString(SERVER_PORT);
    }

    public static int getServerIdleTime() {
        return getInt(SERVER_IDLE);
    }

    public static int getRemoteTimeout() {
        return getInt(REMOTE_TIMEOUT);
    }

    public static int getRemoteTimeoutShort() {
        return getInt(REMOTE_TIMEOUT_SHORT);
    }

    public static int getMaxClients() {
        return getInt(MAX_CLIENTS);
    }

    public static int getSessionClientTimeout() {
        return getInt(SESSION_CLIENT_TIMEOUT);
    }

    public static String getWelcomeMessage() {
        String result = getString(SERVER_WELCOME);
        if (result == null) {
            return "";
        }

        return result;
    }

    public static int getMaxCacheItems() {
        return getInt(CACHE_MAX);
    }

    public static int getCachePort() {
        return getInt(CACHE_PORT);
    }

    public static String getCacheServer() {
        return getString(CACHE_SERVER);
    }

    public static String getRedisServer() {
        return getString(CACHE_REDIS);
    }

    public static int getMaxCacheTime() {
        return getInt(CACHE_TIME);
    }

    public static int getRemoteConnectTimeout() {
        return getInt(REMOTE_CONNECT_TIMEOUT);
    }

    public static void setString(String aKey, String aValue) {
        mProps.setString(aKey, aValue);
    }

    public static boolean isMonitorKoin() {
        return getBoolean(MONITOR_KOIN);
    }

    public static boolean isMonitorUserOnline() {
        return getBoolean(MONITOR_USERONLINE);
    }

    public static int getServerId() {
        return getInt(SERVER_ID);
    }
}
