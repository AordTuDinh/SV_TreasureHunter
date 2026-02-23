package game.dragonhero.mapping;

import game.config.CfgGacha;
import lombok.Data;
import lombok.NoArgsConstructor;
import ozudo.base.database.DBJPA;
import ozudo.base.helper.DateTime;
import ozudo.base.helper.GsonUtil;
import ozudo.base.helper.StringHelper;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@Entity
@Table(name = "user_summon")
public class UserSummonEntity {
    @Id
    int userId;
    int levelStone, levelBonusStone, countSummonStone, levelPiece, levelBonusPiece, countSummonPiece;
    String bonusStone, bonusPiece;
    Date freeSummon, adsSummonStone, adsSummonPiece;

    public UserSummonEntity(int userId) {
        this.userId = userId;
        this.levelStone = 1;
        this.levelPiece = 1;
        this.countSummonStone = 0;
        this.countSummonPiece = 0;
        this.levelBonusStone = 0;
        this.levelBonusPiece = 0;
        this.bonusStone = "[]";
        this.bonusPiece = "[]";
    }


    public boolean addCountSumStone(int value, boolean isFree, boolean isAds) {
        countSummonStone += value;
        return checkTotalSummonStone(isFree, isAds);
    }

    public boolean addCountSumPiece(int value, boolean isAds) {
        countSummonPiece += value;
        return checkTotalSummonPiece(isAds);
    }

    public long getCDSummonFree() {
        if (freeSummon == null) return 0;
        long cd = DateTime.DAY_MILLI_SECOND - (System.currentTimeMillis() - freeSummon.getTime());
        return cd < 0 ? 0 : cd / 1000;
    }

    public long getCDSummonStoneAds() {
        if (adsSummonStone == null) return 0;
        long cd = DateTime.MIN_MILLI_SECOND * CfgGacha.config.timeSummonAdsMinutes - (System.currentTimeMillis() - adsSummonStone.getTime());
        return cd < 0 ? 0 : cd / 1000;
    }

    public long getCDSummonPieceAds() {
        return 100000;
//        if (adsSummonPiece == null) return 0;
//        long cd = DateTime.MIN_MILLI_SECOND * CfgGacha.config.timeSummonAdsMinutes - (System.currentTimeMillis() - adsSummonPiece.getTime());
//        return cd < 0 ? 0 : cd / 1000;
    }

    public int hasBonusStone() {
        List<Long> myBonus = GsonUtil.strToListLong(bonusStone);

        return myBonus.size() > 0 ? 1 : 0;
    }

    public int hasBonusPiece() {
        List<Long> myBonus = GsonUtil.strToListLong(bonusPiece);
        return myBonus.size() > 0 ? 1 : 0;
    }

    // cai nay chac la nen goi khi check summon xong thi hon
    public boolean checkTotalSummonStone(boolean isFree, boolean isAds) {
        if (levelStone > CfgGacha.summonDataStone.size()) levelStone = CfgGacha.summonDataStone.size();
        CfgGacha.SummonLevel dataSummon = CfgGacha.summonDataStone.get(levelStone);
        // up level +
        if (countSummonStone >= dataSummon.number && levelStone > levelBonusStone && levelStone < CfgGacha.summonDataStone.size()) {
            List<Long> myBonus = GsonUtil.strToListLong(bonusStone);
            myBonus.addAll(dataSummon.bonus);
            if (isFree) {
                if (update(Arrays.asList("count_summon_stone", countSummonStone, "level_bonus_stone", levelBonusStone + 1, "bonus_stone", StringHelper.toDBString(myBonus), "level_stone", levelStone + 1, "free_summon", Calendar.getInstance().getTime()))) {
                    levelBonusStone++;
                    levelStone++;
                    freeSummon = Calendar.getInstance().getTime();
                    bonusStone = myBonus.toString();
                    return true;
                }
            } else if (isAds) {
                if (update(Arrays.asList("count_summon_stone", countSummonStone, "level_bonus_stone", levelBonusStone + 1, "bonus_stone", StringHelper.toDBString(myBonus), "level_stone", levelStone + 1, "ads_summon_stone", Calendar.getInstance().getTime()))) {
                    levelBonusStone++;
                    levelStone++;
                    adsSummonStone = Calendar.getInstance().getTime();
                    bonusStone = myBonus.toString();
                    return true;
                }
            } else {
                if (update(Arrays.asList("count_summon_stone", countSummonStone, "level_bonus_stone", levelBonusStone + 1, "bonus_stone", StringHelper.toDBString(myBonus), "level_stone", levelStone + 1))) {
                    levelBonusStone++;
                    levelStone++;
                    bonusStone = myBonus.toString();
                    return true;
                }
            }
        } else {
            if (isFree) {
                if (update(Arrays.asList("count_summon_stone", countSummonStone, "free_summon", Calendar.getInstance().getTime()))) {
                    freeSummon = Calendar.getInstance().getTime();
                    return true;
                }
            } else if (isAds) {
                if (update(Arrays.asList("count_summon_stone", countSummonStone, "ads_summon_stone", Calendar.getInstance().getTime()))) {
                    adsSummonStone = Calendar.getInstance().getTime();
                    return true;
                }
            } else return update(Arrays.asList("count_summon_stone", countSummonStone));
        }
        return false;
    }

    public boolean checkTotalSummonPiece(boolean isAds) {
        if (levelPiece > CfgGacha.summonDataPiece.size()) levelPiece = CfgGacha.summonDataPiece.size();

        CfgGacha.SummonLevel dataSummon = CfgGacha.summonDataPiece.get(levelPiece);
        if (countSummonPiece >= dataSummon.number && levelPiece > levelBonusPiece) {
            List<Long> myBonus = GsonUtil.strToListLong(bonusPiece);
            myBonus.addAll(dataSummon.bonus);
            if (isAds) {
                if (update(Arrays.asList("count_summon_piece", countSummonPiece, "level_bonus_piece", levelBonusPiece + 1, "bonus_piece", StringHelper.toDBString(myBonus), "level_piece", levelPiece + 1, "ads_summon_piece", Calendar.getInstance().getTime()))) {
                    levelBonusPiece++;
                    levelPiece++;
                    bonusPiece = myBonus.toString();
                    adsSummonPiece = Calendar.getInstance().getTime();
                    return true;
                }
            } else {
                if (update(Arrays.asList("count_summon_piece", countSummonPiece, "level_bonus_piece", levelBonusPiece + 1, "bonus_piece", StringHelper.toDBString(myBonus), "level_piece", levelPiece + 1))) {
                    levelBonusPiece++;
                    levelPiece++;
                    bonusPiece = myBonus.toString();
                    return true;
                }
            }
        } else return update(Arrays.asList("count_summon_piece", countSummonPiece));
        return false;
    }


    public boolean update(List<Object> updateData) {
        return DBJPA.update("user_summon", updateData, Arrays.asList("user_id", userId));
    }
}
