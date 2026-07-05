package game.treasure.mapping;

import game.battle.calculate.IMath;
import game.config.ArtifactDataSlot;
import game.config.CfgItem;
import game.treasure.mapping.main.ResArtifactEntity;
import game.treasure.service.resource.ResArtifact;
import lombok.Data;
import lombok.NoArgsConstructor;
import ozudo.base.database.DBJPA;
import ozudo.base.helper.GsonUtil;

import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Data
@Entity
@NoArgsConstructor
@Table(name = "user_artifact")
public class UserArtifactEntity implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id;
    int userId;
    int artifactId;
    int level;
    int tier;
    int isCraft;
    String craftBy;
    String data;

    @Transient
    int bagSlot = -1;

    public UserArtifactEntity(int userId, int artifactId) {
        this(userId, artifactId, 1);
    }

    public UserArtifactEntity(int userId, int artifactId, int tier) {
        this.userId = userId;
        this.artifactId = artifactId;
        this.level = 1;
        this.isCraft = 0;
        this.tier = tier > 0 ? Math.min(tier, 4) : 1;
        this.data = rollDataFromRes(artifactId, this.tier);
    }

    static String rollDataFromRes(int artifactId, int tier) {
        ResArtifactEntity res = ResArtifact.get(artifactId);
        if (res == null)
            return "[]";
        return res.getRollData(tier);
    }

    public ResArtifactEntity getRes() {
        return ResArtifact.get(artifactId);
    }

    public List<Float> getDataListFloat() {
        if (data == null || data.isEmpty() || "[]".equals(data))
            return new ArrayList<>();
        return GsonUtil.strToListFloat(data);
    }

    /**
     * Base tại level 1 (đã roll tier) — giống user_equipment.data.
     */
    public float getBaseSlot(int idx) {
        List<Float> list = getDataListFloat();
        if (idx < 0 || idx >= list.size())
            return 0f;
        return list.get(idx);
    }

    /**
     * Hiệu dụng sau scale level — time/value/range/person × 1.1^(L-1); cd không đổi.
     */
    public float getEffectiveSlot(int idx) {
        float base = getBaseSlot(idx);
        if (base == 0f && idx != ArtifactDataSlot.IDX_CD)
            return 0f;
        if (!ArtifactDataSlot.scalesWithLevel(idx))
            return base;
        int itemLevel = level > 0 ? level : 1;
        float scaled = base * CfgItem.getStatLevelMultiplier(itemLevel);
        if (idx == ArtifactDataSlot.IDX_VALUE) {
            int pointMain = getRes() != null ? getRes().getPointMain() : 0;
            return CfgItem.formatPointStat(pointMain, scaled);
        }
        if (idx == ArtifactDataSlot.IDX_CD)
            return Math.round(base);
        return IMath.round1(scaled);
    }

    public List<Float> getEffectiveDataList() {
        List<Float> out = new ArrayList<>(ArtifactDataSlot.LENGTH);
        for (int i = 0; i < ArtifactDataSlot.LENGTH; i++)
            out.add(getEffectiveSlot(i));
        return out;
    }

    public protocol.Pbmethod.PbArtifact.Builder toProto() {
        protocol.Pbmethod.PbArtifact.Builder pb = protocol.Pbmethod.PbArtifact.newBuilder();
        pb.setId(id);
        pb.setArtifactId(artifactId);
        pb.setLevel(level);
        pb.setTier(tier);
        pb.setTime(getBaseSlot(ArtifactDataSlot.IDX_TIME));
        pb.setCooldown(getBaseSlot(ArtifactDataSlot.IDX_CD));
        ResArtifactEntity res = getRes();
        pb.setPointID(res.getPointMain());
        pb.setValue(getBaseSlot(ArtifactDataSlot.IDX_VALUE));
        pb.setRange(getBaseSlot(ArtifactDataSlot.IDX_RANGE));
        pb.setPerson(getBaseSlot(ArtifactDataSlot.IDX_PERSON));
        pb.setIsCraft(isCraft);
        if (craftBy != null && !craftBy.isEmpty())
            pb.setCraftBy(craftBy);
        return pb;
    }

    public boolean update(List<Object> updateData) {
        return DBJPA.update("user_artifact", updateData, Arrays.asList("id", id));
    }

    public boolean deleteFromDb() {
        return DBJPA.delete("user_artifact", "id", id, "user_id", userId);
    }
}
