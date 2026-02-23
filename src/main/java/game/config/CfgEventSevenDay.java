package game.config;

import com.google.gson.Gson;
import game.config.aEnum.StatusType;
import game.config.lang.Lang;
import game.dragonhero.mapping.UserEventSevenDayEntity;
import game.object.MyUser;
import ozudo.base.helper.DateTime;
import ozudo.base.helper.GsonUtil;
import protocol.Pbmethod;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class CfgEventSevenDay {
    public static DataConfig config;

    public static List<PanelDay> panelDays = new ArrayList<>();


    public static List<Pbmethod.PbPosReward.Builder> pbPosReward(int curValue, List<Integer> status) {
        List<Pbmethod.PbPosReward.Builder> pbPosReward = new ArrayList<>();
        for (int i = 0; i < config.posRewards.size(); i++) {
            PosReward pw = config.posRewards.get(i);
            Pbmethod.PbPosReward.Builder posRw = Pbmethod.PbPosReward.newBuilder();
            posRw.setId(pw.id);
            posRw.setPoint(pw.point);
            posRw.setStatus(status.get(i) == StatusType.PROCESSING.value && curValue >= pw.point ? StatusType.RECEIVE.value : status.get(i));
            posRw.addAllBonus(pw.getBonus());
            pbPosReward.add(posRw);
        }
        return pbPosReward;
    }

    public static void loadConfig(String json) {
        config = new Gson().fromJson(json, DataConfig.class);
        panelDays.add(config.day1);
        panelDays.add(config.day2);
        panelDays.add(config.day3);
        panelDays.add(config.day4);
        panelDays.add(config.day5);
        panelDays.add(config.day6);
        panelDays.add(config.day7);
    }

    public static Pbmethod.PbEvent7Day.Builder toProto(UserEventSevenDayEntity uEvent, MyUser mUser) {
        Pbmethod.PbEvent7Day.Builder pb = Pbmethod.PbEvent7Day.newBuilder();
        long timeRemain = (uEvent.getEnd().getTime() - Calendar.getInstance().getTimeInMillis()) / 1000;
        int curDay = (int) DateTime.getDayDiff(uEvent.getStart(), Calendar.getInstance().getTime());
        pb.setTimeRemain(timeRemain < 0 ? 0 : timeRemain);
        pb.setCurValue(uEvent.getPoint());
        pb.setMaxValue(config.maxPoint);
        List<Pbmethod.PbPosReward.Builder> lstPosReward = pbPosReward(uEvent.getPoint(), uEvent.getSlider());
        for (int i = 0; i < lstPosReward.size(); i++) {
            pb.addPosReward(lstPosReward.get(i));
        }
        List<List<Integer>> status = uEvent.getStatus();
        for (int i = 0; i < panelDays.size(); i++) {
            pb.addDays(panelDays.get(i).toProto(i, uEvent, mUser, status, i != 0 && i > curDay, curDay + 1));
        }
        return pb;
    }


    public class DataConfig {
        public PanelDay day1;
        public PanelDay day2;
        public PanelDay day3;
        public PanelDay day4;
        public PanelDay day5;
        public PanelDay day6;
        public PanelDay day7;
        public int timeAlive;
        public int maxPoint;
        public List<PosReward> posRewards;
    }

    public class PosReward {
        public int id;
        public int point;
        List<Long> bonus;

        public List<Long> getBonus() {
            return new ArrayList<>(bonus);
        }
    }

    public class PanelDay {
        public TabDay tab1;
        public TabDay tab2;
        public TabDay tab3;
        public TabDay tab4;

        public TabDay getTab(int tabId) {
            switch (tabId) {
                case 0 -> {
                    return tab1;
                }
                case 1 -> {
                    return tab2;
                }
                case 2 -> {
                    return tab3;
                }
                case 3 -> {
                    return tab4;
                }

            }
            return tab1;
        }

        public Pbmethod.PbPanelEvent7Day.Builder toProto(int day, UserEventSevenDayEntity uEvent, MyUser mUser, List<List<Integer>> status, boolean isLock, int curDay) {
            Pbmethod.PbPanelEvent7Day.Builder pb = Pbmethod.PbPanelEvent7Day.newBuilder();
            pb.setIsLock(isLock);
            pb.setTab1(tab1.toProto(day, uEvent, mUser, status.get(day * 4 + 0), curDay).build());
            pb.setTab2(tab2.toProto(day, uEvent, mUser, status.get(day * 4 + 1), curDay).build());
            pb.setTab3(tab3.toProto(day, uEvent, mUser, status.get(day * 4 + 2), curDay).build());
            pb.setTab4(tab4.toProto(day, uEvent, mUser, status.get(day * 4 + 3), curDay).build());
            return pb;
        }
    }

    public class TabDay {
        public int id;
        public String name;
        public List<CellItemDay> cells;

        public Pbmethod.PbTabEvent7Day.Builder toProto(int day, UserEventSevenDayEntity uEvent, MyUser mUser, List<Integer> status, int numCur) {
            Pbmethod.PbTabEvent7Day.Builder pb = Pbmethod.PbTabEvent7Day.newBuilder();
            pb.setId(id);
            pb.setName(Lang.getTitle(mUser,name));
            if (id == 0) {
                pb.addCells(cells.get(0).toProto(mUser,mUser.getUEvent().getNumBuyIap(), status.get(0)));
                pb.addCells(cells.get(1).toProto(mUser,numCur, status.get(1)));
            } else {
                int curValue = uEvent.getCurValue(day, id, mUser);
                for (int i = 0; i < cells.size(); i++) {
                    pb.addCells(cells.get(i).toProto(mUser,curValue, status.get(i)));
                }
            }
            return pb;
        }


    }

    public class CellItemDay {
        public int id;
        public String name;
        public String desc;
        public int maxValue;
        List<Long> bonus;
        public int btnGo;
        public List<Long> oldPrice;
        List<Long> newPrice;
        public int xu;


        public List<Long> getBonus() {
            return new ArrayList<>(bonus);
        }

        public List<Long> getNewPrice() {
            return new ArrayList<>(newPrice);
        }

        public Pbmethod.PbCellEvent7Day.Builder toProto(MyUser mUser ,  int curValue, int status) {
            Pbmethod.PbCellEvent7Day.Builder pb = Pbmethod.PbCellEvent7Day.newBuilder();
            pb.setId(id);
            pb.setName(Lang.getTitle( mUser,name));
            pb.setDesc(desc);
            pb.setCurValue(curValue);
            pb.setMaxValue(maxValue);
            pb.addAllBonus(getBonus());
            pb.setButtonStatus(status != StatusType.DONE.value && curValue >= maxValue ? StatusType.RECEIVE.value : status);
            pb.setButtonGoto(btnGo);
            pb.setXu(xu);
            pb.addAllOldPrice(oldPrice);
            pb.addAllNewPrice(newPrice);
            return pb;
        }
    }
}
