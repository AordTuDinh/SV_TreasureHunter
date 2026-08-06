package game.treasure.mapping.main;

import lombok.Getter;
import lombok.NoArgsConstructor;
import ozudo.base.helper.GsonUtil;
import ozudo.base.helper.NumberUtil;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@Entity
@Table(name = "res_bonus_image")
public class ResBonusImageEntity implements Serializable {
    @Getter
    @Id
    int id;

    @Getter
    int icon;

    @Getter
    int type;

    /** tier hiển thị / tier material nhận được. */
    @Getter
    int tier;

    @Getter
    String name;

    @Getter
    String desc;

    /**
     * - CRAFT_EXP: số exp per roll (vd "1" hoặc "[1]").
     * - MATERIAL: JSON list materialId (vd "[6,7,9]").
     * - SKIN: JSON list skinId.
     * - PET: JSON list petId; tier nhận = {@link #tier}.
     * - MOUNT: JSON list mountId; tier nhận = {@link #tier}.
     * - BONUS_DATA: flat bonus wire (vd "[2,200,13,15,500]"); tier không dùng.
     */
    @Getter
    String data;

    @Getter
    @Transient
    int craftExpPerRoll = 0;

    @Getter
    @Transient
    List<Integer> materialIds = new ArrayList<>();

    @Getter
    @Transient
    List<Integer> skinIds = new ArrayList<>();

    @Getter
    @Transient
    List<Integer> petIds = new ArrayList<>();

    @Getter
    @Transient
    List<Integer> mountIds = new ArrayList<>();

    /** Parsed từ data khi type = BONUS_DATA. */
    @Getter
    @Transient
    List<Long> bonusData = new ArrayList<>();

    public void init() {
        ResBonusImageType t = ResBonusImageType.fromInt(type);
        if (t == null) return;
        switch (t) {
            case CRAFT_EXP -> craftExpPerRoll = parseCraftExpPerRoll(data);
            case MATERIAL -> materialIds = GsonUtil.strToListInt(data);
            case SKIN -> skinIds = GsonUtil.strToListInt(data);
            case PET -> petIds = GsonUtil.strToListInt(data);
            case MOUNT -> mountIds = GsonUtil.strToListInt(data);
            case BONUS_DATA -> bonusData = parseBonusData(data);
        }
    }

    private List<Long> parseBonusData(String raw) {
        if (raw == null || raw.trim().isEmpty())
            return new ArrayList<>();
        List<Long> parsed = GsonUtil.strToListLong(raw);
        return parsed != null ? parsed : new ArrayList<>();
    }

    private int parseCraftExpPerRoll(String raw) {
        if (raw == null) return 0;
        raw = raw.trim();
        if (raw.isEmpty()) return 0;
        if (NumberUtil.isIntNumber(raw))
            return Math.max(0, Integer.parseInt(raw));
        try {
            // fallback: "[1]"
            if (raw.startsWith("[") && raw.endsWith("]")) {
                List<Integer> list = GsonUtil.strToListInt(raw);
                if (list != null && !list.isEmpty())
                    return Math.max(0, list.get(0));
            }
        } catch (Exception ignored) {
        }
        return 0;
    }
}

