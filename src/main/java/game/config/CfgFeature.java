package game.config;

import com.google.gson.Gson;
import game.config.aEnum.FeatureBlockType;
import game.config.aEnum.FeatureType;
import game.config.lang.Lang;
import game.dragonhero.controller.AHandler;
import game.object.MyUser;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CfgFeature {
    public static DataConfig config;
    public static Map<FeatureType, FeatureData> mFeature = new HashMap();
    public static final int TOAST_SUCCESS = 1;
    public static final int TOAST_FAIL = 2;

    public static void loadConfig(String strJson) {
        config = new Gson().fromJson(strJson, DataConfig.class);
        mFeature.clear();
        config.features.forEach(fea -> {
            mFeature.put(FeatureType.get(fea.id), fea);
        });
    }

    public static boolean isOpenFeature(FeatureType type, MyUser mUser, AHandler handler) {
        if (CfgFeature.mFeature.get(type.value) == null) return true;
        List<Integer> require = CfgFeature.mFeature.get(type.value).require;
        int curNumber = 0;
        switch (FeatureBlockType.get(require.get(0))) {
            case BLOCK_NULL -> curNumber = -1;
            case BLOCK_BY_LEVEL -> curNumber = mUser.getUser().getLevel();
            case BLOCK_BY_TUT_QUEST -> curNumber = mUser.getUData().getQuestTutorial();
        }
        if (curNumber != -1 && mUser.getUser().getLevel() < require.get(1)) {
            handler.addErrResponse(String.format(Lang.instance(mUser).get(Lang.err_feature_lock), require.get(1)));
            return false;
        }
        return true;
    }

    public class DataConfig {
        public List<FeatureData> features;
    }

    public class FeatureData {
        public int id;
        public String name;
        public List<Integer> require;
    }
}
