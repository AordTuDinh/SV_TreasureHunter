package game.dragonhero.service.resource;

import game.config.CfgServer;
import game.config.aEnum.AvatarIndex;
import game.dragonhero.mapping.main.ResAvatarEntity;
import game.dragonhero.mapping.main.ResChatFrameEntity;
import game.dragonhero.mapping.main.ResDameSkinEntity;
import game.dragonhero.mapping.main.ResEffectTrialEntity;
import ozudo.base.database.DBResource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResAvatar {
    public static Map<Integer, ResAvatarEntity> mAvatar = new HashMap<>();
    public static Map<Integer, ResDameSkinEntity> mDameSkin = new HashMap<>();
    public static Map<Integer, ResChatFrameEntity> mChatFrame = new HashMap<>();
    public static Map<Integer, ResEffectTrialEntity> mTrial = new HashMap<>();
    public static List<ResAvatarEntity> aAvatar = new ArrayList<>();
    public static List<ResAvatarEntity> aAvatarHero = new ArrayList<>();


    public static ResAvatarEntity getAvatar(int id) {
        return mAvatar.get(id);
    }

    public static void init() {
        aAvatar = DBResource.getInstance().getList(CfgServer.DB_MAIN + "res_avatar", ResAvatarEntity.class);
        mAvatar.clear();
        aAvatar.forEach(avatar -> {
            mAvatar.put(avatar.getId(), avatar);
            if (avatar.getType() == AvatarIndex.HERO) {
                aAvatarHero.add(avatar);
            }
        });
        // dame skin
        List<ResDameSkinEntity> aDameSkin = DBResource.getInstance().getList(CfgServer.DB_MAIN + "res_dame_skin", ResDameSkinEntity.class);
        mDameSkin.clear();
        aDameSkin.forEach(avatar -> mDameSkin.put(avatar.getId(), avatar));
        // chat frame
        List<ResChatFrameEntity> aChatFrame = DBResource.getInstance().getList(CfgServer.DB_MAIN + "res_chat_frame", ResChatFrameEntity.class);
        mChatFrame.clear();
        aChatFrame.forEach(chat -> mChatFrame.put(chat.getId(), chat));
        // chat frame
        List<ResEffectTrialEntity> aTrial = DBResource.getInstance().getList(CfgServer.DB_MAIN + "res_effect_trial", ResEffectTrialEntity.class);
        mTrial.clear();
        aTrial.forEach(trial -> mTrial.put(trial.getId(), trial));
    }
}
