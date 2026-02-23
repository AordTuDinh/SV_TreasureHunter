package game.dragonhero.service.resource;

import game.config.CfgServer;
import game.dragonhero.mapping.main.ConfigHelpEntity;
import ozudo.base.database.DBResource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResHelp {
    public static Map<String, ConfigHelpEntity> mHelp = new HashMap<>();

    public static void init() {
        List<ConfigHelpEntity> aHelp = DBResource.getInstance().getList(CfgServer.DB_MAIN + "config_help", ConfigHelpEntity.class);
        mHelp.clear();
        aHelp.forEach(help -> {
            mHelp.put(help.getK(), help);
        });
    }
}
