package game.treasure.mapping;

import game.treasure.mapping.main.ResSkinEntity;
import game.treasure.service.resource.ResAvatar;
import lombok.Data;
import lombok.NoArgsConstructor;
import ozudo.base.database.DBJPA;
import ozudo.base.helper.GsonUtil;
import protocol.Pbmethod;

import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@Entity
@NoArgsConstructor
@Table(name = "user_skin")
public class UserSkinEntity implements Serializable {
    public static final int SKIN_MIN = 8;
    public static final int PART_COUNT = 7;
    public static final int EQUIPPED_SIZE = PART_COUNT * 2;
    /** Format cũ: HAIR, FACE, EYE, BODY. */
    private static final int OLD_PART_COUNT = 4;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id;
    int userId;
    int skinId;
    String data;
    int type;
    int tier;
    int isCraft;
    int isTrading;
    int inMarket;
    @Temporal(TemporalType.TIMESTAMP)
    Date dateCreated;

    public UserSkinEntity(UserEntity user, int skinId, int type) {
        this.userId = user.getId();
        this.skinId = skinId;
        this.type = type;
        this.tier = 1;
        this.data = "[]";
        this.dateCreated = new Date();
    }

    public ResSkinEntity getResSkin() {
        return ResAvatar.getSkin(skinId);
    }

    public protocol.Pbmethod.PbSkin toProto() {
        try {
            protocol.Pbmethod.PbSkin.Builder pb = toProtoBuilder();
            byte[] bytes = pb.build().toByteArray();
            bytes = game.treasure.service.item.ProtoTradingWire.appendSkinTrading(bytes, isCraft, isTrading, inMarket);
            return protocol.Pbmethod.PbSkin.parseFrom(bytes);
        } catch (Exception ex) {
            return toProtoBuilder().build();
        }
    }

    public protocol.Pbmethod.PbSkin.Builder toProtoBuilder() {
        protocol.Pbmethod.PbSkin.Builder pb = protocol.Pbmethod.PbSkin.newBuilder();
        pb.setId(id);
        pb.setTier(tier);
        pb.setType(type);
        pb.setSkinId(skinId);
        pb.addAllPoint(getData());
        return pb;
    }

    public List<Long> getData() {
        return GsonUtil.strToListLong(data);
    }

    public boolean update(List<Object> lst) {
        return DBJPA.update("user_skin", lst, List.of("id", id));
    }

    public static int skinSlotIndex(int type) {
        int i = type - SKIN_MIN;
        if (i < 0 || i >= PART_COUNT) return -1;
        return i * 2;
    }

    public static List<Integer> normalize(List<Integer> skins) {
        List<Integer> result = skins != null ? new ArrayList<>(skins) : new ArrayList<>();
        if (result.size() == OLD_PART_COUNT || result.size() == OLD_PART_COUNT * 2) {
            result = migrateOldSkins(result);
        }
        while (result.size() < EQUIPPED_SIZE) result.add(0);
        if (result.size() > EQUIPPED_SIZE) {
            return new ArrayList<>(result.subList(0, EQUIPPED_SIZE));
        }
        return result;
    }

    /** Cũ: HAIR, FACE, EYE, BODY. Mới: BODY, HEAD, HAIR, FACE, ACCESSORY, GLASSES, BRACELET. */
    private static List<Integer> migrateOldSkins(List<Integer> old) {
        int[] user = new int[OLD_PART_COUNT];
        int[] res = new int[OLD_PART_COUNT];
        if (old.size() == OLD_PART_COUNT) {
            for (int i = 0; i < OLD_PART_COUNT; i++) res[i] = old.get(i);
        } else {
            for (int i = 0; i < OLD_PART_COUNT; i++) {
                user[i] = old.get(i * 2);
                res[i] = old.get(i * 2 + 1);
            }
        }
        List<Integer> neu = new ArrayList<>();
        for (int i = 0; i < EQUIPPED_SIZE; i++) neu.add(0);
        place(neu, 0, user[3], res[3]); // BODY
        place(neu, 2, user[0], res[0]); // HAIR
        place(neu, 3, user[1], res[1]); // FACE
        return neu;
    }

    private static void place(List<Integer> list, int partIndex, int userSkinId, int resSkinId) {
        int index = partIndex * 2;
        list.set(index, userSkinId);
        list.set(index + 1, resSkinId);
    }

    public static int getResSkinId(List<Integer> skins, Pbmethod.SkinType part) {
        List<Integer> normalized = normalize(skins);
        int index = skinSlotIndex(part.getNumber());
        if (index < 0) return 0;
        return normalized.get(index + 1);
    }

    public static int getPart(List<Integer> skins, Pbmethod.SkinType part) {
        return getResSkinId(skins, part);
    }

    public static int getBodyId(List<Integer> skins) {
        return getResSkinId(skins, Pbmethod.SkinType.BODY);
    }

    public static void setEquipped(List<Integer> skins, Pbmethod.SkinType part, long userSkinId, int resSkinId) {
        List<Integer> normalized = normalize(skins);
        int index = skinSlotIndex(part.getNumber());
        if (index < 0) return;
        normalized.set(index, (int) userSkinId);
        normalized.set(index + 1, resSkinId);
        skins.clear();
        skins.addAll(normalized);
    }
}
