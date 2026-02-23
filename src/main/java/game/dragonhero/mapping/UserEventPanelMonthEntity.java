package game.dragonhero.mapping;


import game.config.aEnum.StatusType;
import game.dragonhero.mapping.main.ResEventPanelMonthEntity;
import game.dragonhero.service.resource.ResEventPanel;
import lombok.Data;
import lombok.NoArgsConstructor;
import ozudo.base.database.DBJPA;
import ozudo.base.helper.GsonUtil;
import ozudo.base.helper.NumberUtil;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

@Entity
@NoArgsConstructor
@Table(name = "user_event_panel_month")
@Data
public class UserEventPanelMonthEntity implements Serializable {
    @Id
    int userId, eventId;
    String statusNormal, statusVip;
    int point, exp;

    public UserEventPanelMonthEntity(int userId, int eventId) {
        this.userId = userId;
        this.eventId = eventId;
        ResEventPanelMonthEntity rEvent = ResEventPanel.getPanelMonth(eventId);
        this.statusNormal = NumberUtil.genListStringInt(rEvent.getExp().size(), StatusType.PROCESSING.value);
        this.statusVip = NumberUtil.genListStringInt(rEvent.getExp().size(), StatusType.PROCESSING.value);
        this.point = 0;
        this.exp = 0;
    }

    public List<Integer> getStatusNormal() {
        return GsonUtil.strToListInt(statusNormal);
    }

    public List<Integer> getStatusVip() {
        return GsonUtil.strToListInt(statusVip);
    }


    public boolean update(List<Object> updateData) {
        return DBJPA.update("user_event_panel_month", updateData, Arrays.asList("user_id", userId, "event_id", eventId));
    }
}
