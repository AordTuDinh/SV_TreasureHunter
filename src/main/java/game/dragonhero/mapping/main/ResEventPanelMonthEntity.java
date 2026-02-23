package game.dragonhero.mapping.main;

import game.config.lang.Lang;
import game.dragonhero.service.resource.ResEvent;
import game.object.MyUser;
import lombok.Getter;
import lombok.NoArgsConstructor;
import ozudo.base.helper.GsonUtil;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Transient;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;


@Entity
@NoArgsConstructor
public class ResEventPanelMonthEntity {
    @Getter
    @Id
    int id;
    @Getter
    int addGoto;
    int packId;
    @Getter
    String imageBanner, keyHelp, tabName, tabImage;
    String titleEventName, titleNameNormal, titleNameVip, titleBanner, exp, bonusNormal, bonusVip;
    Date timeStart, timeEnd;
    @Transient
    @Getter
    List<Integer> aExp;
    @Transient
    @Getter
    List<List<Long>> aBonusNormal, aBonusVip;// nhớ clone list


    public long getCD() {
        long cd = timeEnd.getTime() - Calendar.getInstance().getTime().getTime();
        return cd < 0 ? 0 : cd / 1000;
    }

    public List<Long> getBonusNormalIndex(int index) {
        return new ArrayList<>(aBonusNormal.get(index));
    }

    public List<Long> getBonusVipIndex(int index) {
        return new ArrayList<>(aBonusVip.get(index));
    }

    public List<Integer> getExp() {
        return GsonUtil.strToListInt(exp);
    }

    public boolean isActive() {
        return timeEnd.after(Calendar.getInstance().getTime()) && timeStart.before(Calendar.getInstance().getTime());
    }

    public void init() {
        aExp = GsonUtil.strToListInt(exp);
        aBonusNormal = GsonUtil.strTo2ListLong(bonusNormal);
        aBonusVip = GsonUtil.strTo2ListLong(bonusVip);
    }

    public ResPackEntity getPack() {
        return ResEvent.getResPack(packId);
    }

    public String getTitleEventName(MyUser myUser) {
        return Lang.getTitle( myUser,titleEventName);
    }

    public String getTitleNameNormal(MyUser myUser) {
        return Lang.getTitle(myUser, titleNameNormal);
    }

    public String getTitleNameVip(MyUser myUser) {
        return Lang.getTitle(myUser, titleNameVip);
    }

    public String getTitleBanner(MyUser myUser) {
        return Lang.getTitle(myUser,titleBanner);
    }
}
