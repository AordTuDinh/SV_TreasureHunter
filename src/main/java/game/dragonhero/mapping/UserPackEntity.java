package game.dragonhero.mapping;


import game.config.aEnum.PackType;
import game.dragonhero.mapping.main.ResPackEntity;
import game.dragonhero.service.resource.ResEvent;
import game.object.DataDaily;
import game.object.MyUser;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ozudo.base.database.DBJPA;
import ozudo.base.helper.DateTime;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_pack")
@Builder
public class UserPackEntity implements Serializable {
    @Id
    int userId, packId;
    int serverId;
    long timeBuy;// time ms
    Date dateCreated;
    int number;


    public boolean buyPack(MyUser mUser, int number) {
        this.timeBuy = Calendar.getInstance().getTimeInMillis();
        this.dateCreated = Calendar.getInstance().getTime();
        this.number = number;
        // k cho nhận ngày hôm nay nữa
        switch (PackType.get(packId)) {
            case THE_TUAN -> {
                DataDaily data = mUser.getUserDaily().getUDaily();
                data.setValueAndUpdate(DataDaily.GET_CARD_WEEK, 1);
            }
            case THE_THANG -> {
                DataDaily data = mUser.getUserDaily().getUDaily();
                data.setValueAndUpdate(DataDaily.GET_CARD_MONTH, 1);
            }
            case THE_VINH_VIEN -> {
                DataDaily data = mUser.getUserDaily().getUDaily();
                data.setValueAndUpdate(DataDaily.GET_CARD_VINH_VIEN, 1);
            }
        }


        if (DBJPA.saveOrUpdate(this)) {
            mUser.getResources().addPack(this);
            return true;
        }
        return false;
    }

    public long getDayDiff() {
        return DateTime.getDayDiff(dateCreated, Calendar.getInstance().getTime());
    }

    public boolean hasHSD() {
        boolean hsd = false;
        if (getRes().getTime() == 0) {
            hsd = true;
        } else if (getRes().getTime() == -1) {
            hsd = DateTime.getDayDiff(Calendar.getInstance().getTime(), dateCreated) == 0;
        } else if (getRes().getTime() == -2) {
            hsd = DateTime.equalsWeek(Calendar.getInstance().getTime(), dateCreated);
        } else if (getRes().getTime() == -3) {
            hsd = DateTime.equalsMonth(Calendar.getInstance().getTime(), dateCreated);
        } else hsd = timeBuy + getRes().getTime() > Calendar.getInstance().getTimeInMillis();
        // clear number
        if (!hsd && number > 0) {
            this.number = 0;
        }
        return hsd;
    }

    public ResPackEntity getRes() {
        return ResEvent.getResPack(packId);
    }
}
