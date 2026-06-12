package game.treasure.service.resource;

import game.config.CfgServer;
import game.object.MyUser;
import game.treasure.mapping.UserEntity;
import game.treasure.mapping.UserSkinEntity;
import game.treasure.mapping.main.ResChatFrameEntity;
import game.treasure.mapping.main.ResDameSkinEntity;
import game.treasure.mapping.main.ResEffectTrialEntity;
import game.treasure.mapping.main.ResSkinEntity;
import ozudo.base.database.DBResource;
import protocol.Pbmethod;

import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ResAvatar {
    public static Map<Integer, ResDameSkinEntity> mDameSkin = new HashMap<>();
    public static Map<Integer, ResChatFrameEntity> mChatFrame = new HashMap<>();
    public static Map<Integer, ResEffectTrialEntity> mTrial = new HashMap<>();
    public static Map<Integer, ResSkinEntity> mSkin = new HashMap<>();

    private static final Pbmethod.SkinType[] DEFAULT_SKIN_TYPES = {
            Pbmethod.SkinType.HAIR,
            Pbmethod.SkinType.FACE,
            Pbmethod.SkinType.EYE,
            Pbmethod.SkinType.BODY
    };

    public static void init() {
        List<ResDameSkinEntity> aDameSkin = DBResource.getInstance().getList(CfgServer.DB_MAIN + "res_dame_skin", ResDameSkinEntity.class);
        mDameSkin.clear();
        aDameSkin.forEach(avatar -> mDameSkin.put(avatar.getId(), avatar));

        List<ResChatFrameEntity> aChatFrame = DBResource.getInstance().getList(CfgServer.DB_MAIN + "res_chat_frame", ResChatFrameEntity.class);
        mChatFrame.clear();
        aChatFrame.forEach(chat -> mChatFrame.put(chat.getId(), chat));

        List<ResEffectTrialEntity> aTrial = DBResource.getInstance().getList(CfgServer.DB_MAIN + "res_effect_trial", ResEffectTrialEntity.class);
        mTrial.clear();
        aTrial.forEach(trial -> mTrial.put(trial.getId(), trial));

        List<ResSkinEntity> aSkin = DBResource.getInstance().getList(CfgServer.DB_MAIN + "res_skin", ResSkinEntity.class);
        mSkin.clear();
        aSkin.forEach(skin -> mSkin.put(skin.getId(), skin));
    }

    public static ResSkinEntity getSkin(int skinId) {
        return mSkin.get(skinId);
    }

    public static List<ResSkinEntity> getSkinsByType(Pbmethod.SkinType type) {
        return mSkin.values().stream()
                .filter(s -> s.getType() == type.getNumber())
                .collect(Collectors.toList());
    }

    public static int getDefaultSkinId(Pbmethod.SkinType type) {
        List<ResSkinEntity> list = getSkinsByType(type);
        if (list.isEmpty()) return 0;
        return list.get(0).getId();
    }

    public static void ensureDefaultSkins(MyUser mUser, EntityManager session) {
        if (mUser.getResources().getMSkin() != null && !mUser.getResources().getMSkin().isEmpty()) {
            return;
        }
        UserEntity user = mUser.getUser();
        List<Integer> equipped = UserSkinEntity.normalize(user.getSkins());
        boolean hasEquipped = equipped.stream().anyMatch(v -> v > 0);
        boolean created = false;

        session.getTransaction().begin();
        try {
            for (Pbmethod.SkinType type : DEFAULT_SKIN_TYPES) {
                int resId = getDefaultSkinId(type);
                if (resId <= 0) continue;
                if (mUser.getResources().getSkinByConfigId(resId) != null) continue;

                UserSkinEntity uSkin = new UserSkinEntity(user, resId, type.getNumber());
                session.persist(uSkin);
                session.flush();
                mUser.getResources().addSkin(uSkin);
                created = true;

                if (UserSkinEntity.getResSkinId(equipped, type) <= 0) {
                    UserSkinEntity.setEquipped(equipped, type, uSkin.getId(), resId);
                }
            }
            session.getTransaction().commit();
        } catch (Exception ex) {
            session.getTransaction().rollback();
            throw ex;
        }

        if (created || !hasEquipped) {
            user.updateSkins(equipped);
        }
    }

    public static UserSkinEntity grantSkin(MyUser mUser, EntityManager session, int resSkinId) {
        ResSkinEntity res = getSkin(resSkinId);
        if (res == null) return null;
        UserSkinEntity existing = mUser.getResources().getSkinByConfigId(resSkinId);
        if (existing != null) return existing;

        UserSkinEntity uSkin = new UserSkinEntity(mUser.getUser(), resSkinId, res.getType());
        session.getTransaction().begin();
        session.persist(uSkin);
        session.getTransaction().commit();
        mUser.getResources().addSkin(uSkin);
        return uSkin;
    }
}
