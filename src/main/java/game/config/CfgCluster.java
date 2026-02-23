package game.config;

public class CfgCluster {
    public static int getShowServer(int serverId) {
        return serverId >= 4 ? serverId - 3 : serverId;
    }
}
