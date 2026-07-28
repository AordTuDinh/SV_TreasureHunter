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
    public static final int PART_COUNT = 4;
    public static final int EQUIPPED_SIZE = PART_COUNT * 2;

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

    public static List<Integer> normalize(List<Integer> skins) {
        List<Integer> result = skins != null ? new ArrayList<>(skins) : new ArrayList<>();
        if (result.size() == PART_COUNT) {
            List<Integer> migrated = new ArrayList<>(EQUIPPED_SIZE);
            for (int i = 0; i < PART_COUNT; i++) {
                migrated.add(0);
                migrated.add(result.get(i));
            }
            result = migrated;
        }
        while (result.size() < EQUIPPED_SIZE) result.add(0);
        if (result.size() > EQUIPPED_SIZE) {
            return new ArrayList<>(result.subList(0, EQUIPPED_SIZE));
        }
        return result;
    }


    public static int getResSkinId(List<Integer> skins, Pbmethod.SkinType part) {
        List<Integer> normalized = normalize(skins);
        int index = part.getNumber() * 2 + 1;
        if (index < 0 || index >= normalized.size()) return 0;
        return normalized.get(index);
    }

    public static int getPart(List<Integer> skins, Pbmethod.SkinType part) {
        return getResSkinId(skins, part);
    }

    public static int getBodyId(List<Integer> skins) {
        return getResSkinId(skins, Pbmethod.SkinType.BODY);
    }

    public static void setEquipped(List<Integer> skins, Pbmethod.SkinType part, long userSkinId, int resSkinId) {
        List<Integer> normalized = normalize(skins);
        int index = part.getNumber() * 2;
        normalized.set(index, (int) userSkinId);
        normalized.set(index + 1, resSkinId);
        skins.clear();
        skins.addAll(normalized);
    }
}
