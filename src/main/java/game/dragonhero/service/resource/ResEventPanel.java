package game.dragonhero.service.resource;

import game.config.CfgServer;
import game.dragonhero.mapping.main.ResEventPanelMonthEntity;
import ozudo.base.database.DBResource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResEventPanel {
    static Map<Integer, ResEventPanelMonthEntity> mPanelMonth = new HashMap<>();
    public static List<ResEventPanelMonthEntity> aPanelMonth = new ArrayList<>();

    public static ResEventPanelMonthEntity getPanelMonth(int eventId) {
        return mPanelMonth.get(eventId);
    }


    public static List<Integer> getLevelPanelMonth(List<Integer> exps, int curPoint) {
        int curLevel = exps.size();
        int maxExp = exps.get(curLevel - 1);
        for (int i = 1; i <= exps.size(); i++) {
            if (curPoint < exps.get(i - 1)) {
                curLevel = i - 1;
                maxExp = exps.get(curLevel - 1);
                break;
            }
        }
        return List.of(curLevel, maxExp);
    }

    public static void init() {
        // panel event month
        aPanelMonth = DBResource.getInstance().getList(CfgServer.DB_MAIN + "res_event_panel_month", new ArrayList<>(), "where time_end > Now()", ResEventPanelMonthEntity.class);
        mPanelMonth.clear();
        aPanelMonth.forEach(event -> {
            event.init();
            mPanelMonth.put(event.getId(), event);
        });
    }
}
