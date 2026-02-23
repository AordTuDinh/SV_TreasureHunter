package game.dragonhero.mapping;


import game.config.aEnum.StatusType;
import game.dragonhero.mapping.main.ResEventClockEntity;
import game.dragonhero.service.resource.ResEvent;
import lombok.Data;
import lombok.NoArgsConstructor;
import ozudo.base.database.DBJPA;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Entity
@NoArgsConstructor
@Table(name = "user_event_clo")
@Data
public class UserEventCloEntity implements Serializable {
    @Id
    int userId, eventId;
    Date timeStart;
    int status;
    Date timeEnd;


    public UserEventCloEntity(int userId, ResEventClockEntity resEvent) {
        this.userId = userId;
        this.eventId = resEvent.getId();
        this.timeStart = Calendar.getInstance().getTime();
        Calendar curDate = Calendar.getInstance();
        curDate.add(Calendar.HOUR, resEvent.getTimeAlive());
        this.timeEnd = curDate.getTime();
        this.status = StatusType.PROCESSING.value;
    }

    public boolean isAlive() {
        return Calendar.getInstance().getTime().before(timeEnd);
    }

        public protocol.Pbmethod.PbEventTimer.Builder toProto() {
        protocol.Pbmethod.PbEventTimer.Builder pb = protocol.Pbmethod.PbEventTimer.newBuilder();
        ResEventClockEntity res = getRes();
        if (res != null) {
            pb.setId(res.getId());
            pb.setStatus(status);
            long timeRemain = (timeEnd.getTime() - Calendar.getInstance().getTime().getTime()) / 1000;
            pb.setTimeRemain(timeRemain);
            pb.addAllBonus(res.getBonus());
            pb.setName(res.getName());
            pb.setDesc(res.getDesc());
            pb.setSale(res.getSale());
            pb.setBgrPath(res.getBgr());
            pb.addAllPrice(res.getPriceLong());
            pb.addAllOldPrice(res.getOldPriceLong());
        }
        return pb;
    }

    public ResEventClockEntity getRes() {
        return ResEvent.getResEventTimer(eventId);
    }


    public boolean update(List<Object> data) {
        return DBJPA.update("user_event_clo", data, List.of("user_id", userId, "event_id", eventId));
    }
}
