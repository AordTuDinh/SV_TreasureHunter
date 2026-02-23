package game.dragonhero.service;

import game.config.CfgArena;
import game.config.CfgServer;
import game.config.aEnum.*;
import game.config.lang.Lang;
import game.dragonhero.controller.UserEventTopEntity;
import game.dragonhero.mapping.*;
import game.dragonhero.mapping.main.CacheUserBuyRubyEntity;
import game.dragonhero.mapping.main.ResIAPEntity;
import game.dragonhero.service.resource.ResIAP;
import game.dragonhero.service.user.Bonus;
import game.object.DataDaily;
import game.object.MyUser;
import ozudo.base.database.DBJPA;
import ozudo.base.database.DBResource;
import ozudo.base.helper.*;
import ozudo.base.log.Logs;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class UserService {
    public void afterLogin(MyUser mUser) {
        CfgServer.checkSystemMail(mUser.getUser());
        // send bonus card month,week,day
        DataDaily data = mUser.getUserDaily().getUDaily();
        // quà thẻ tuần
        UserPackEntity pWeek = mUser.getResources().getPack(PackType.THE_TUAN);
        if (pWeek != null && pWeek.hasHSD() && data.getValue(DataDaily.GET_CARD_WEEK) == 0) {
            if (DBResource.getInstance().rawSQL(DBHelper.sqlMail(mUser.getUser().getId(), Lang.getTitle(mUser,Lang.mail_card_week_daily), pWeek.getRes().getBonusDay()))) {
                data.setValueAndUpdate(DataDaily.GET_CARD_WEEK, 1);
            }
            mUser.getPerReceiveBoss().set(0, (int) (mUser.getPerReceiveBoss().get(0)+ pWeek.getRes().getDataList().get(1)));
        }
        // quà thẻ tháng
        UserPackEntity pMonth = mUser.getResources().getPack(PackType.THE_THANG);
        if (pMonth != null && pMonth.hasHSD() && data.getValue(DataDaily.GET_CARD_MONTH) == 0) {
            if (DBResource.getInstance().rawSQL(DBHelper.sqlMail(mUser.getUser().getId(), Lang.getTitle(mUser,Lang.mail_card_month_daily), pMonth.getRes().getBonusDay()))) {
                data.setValueAndUpdate(DataDaily.GET_CARD_MONTH, 1);
            }
            mUser.getPerReceiveBoss().set(1, (int) (mUser.getPerReceiveBoss().get(1)+ pMonth.getRes().getDataList().get(1)));
        }
        // quà thẻ vĩnh viễn
        UserPackEntity pCard = mUser.getResources().getPack(PackType.THE_VINH_VIEN);
        if (pCard != null && pCard.hasHSD() && data.getValue(DataDaily.GET_CARD_VINH_VIEN) == 0) {
            if (DBResource.getInstance().rawSQL(DBHelper.sqlMail(mUser.getUser().getId(), Lang.getTitle(mUser,Lang.mail_card_forever_daily), pCard.getRes().getBonusDay()))) {
                data.setValueAndUpdate(DataDaily.GET_CARD_VINH_VIEN, 1);
            }
            List<Integer> dataLst= GsonUtil.strToListInt(pCard.getRes().getStringData());
            mUser.getPerReceiveBoss().set(0, mUser.getPerReceiveBoss().get(0)+ dataLst.get(1));
            mUser.getPerReceiveBoss().set(1, mUser.getPerReceiveBoss().get(1)+ dataLst.get(2));
        }
        // check buy qr error
        checkBuyQrError(mUser);
        // check event 7 day
        UserEventSevenDayEntity uEvent = Services.userDAO.getUserSevenDay(mUser);
        if (uEvent.hasEvent() && uEvent.hasActive(6)) {
            int curNumMonster = mUser.getResources().getMPetMonster().size();
            if (curNumMonster != uEvent.getMonster()) {
                uEvent.update(List.of("monster", curNumMonster));
                uEvent.setMonster(curNumMonster);
            }
        }
        if (uEvent.hasEvent() && uEvent.hasActive(5)) {
            int curNumMonster = mUser.getResources().getMLand().size();
            if (curNumMonster != uEvent.getMonster()) {
                uEvent.update(List.of("buy_land", curNumMonster));
                uEvent.setBuyLand(curNumMonster);
            }
        }
        // check den bu qua vip
        if (mUser.getUser().getLevel() > 5 && mUser.getUser().getServer()==1) {
            CacheUserBuyRubyEntity cUser = ResIAP.getCacheUserBuyRubyEntity(mUser.getUser().getMainId());
            if (cUser != null && cUser.getReceive() == 0 && cUser.getBonus()) {
                List<Long> bonus = Bonus.viewVipExp(cUser.getVipExp());
                bonus.addAll(Bonus.viewRuby(cUser.getRuby()));
                if (!DBJPA.rawSQL(DBHelper.sqlMail(mUser.getUser().getId(), Lang.getTitle(mUser,Lang.mail_refund_close_beta), StringHelper.toDBString(bonus)))) {
                    cUser.setReceive(0);
                    cUser.setDateGet(null);
                    DBJPA.update(cUser);
                }
            }
        }
        // check top pet
        AtomicInteger point = new AtomicInteger();
        Map<Integer, UserPetEntity> upets = mUser.getResources().getMPetAnimal();
        upets.forEach((k,v)->{
            point.addAndGet(v.getResPet().getRank() * (v.getStar() + 1));
        });
        UserEventTopEntity uTop = Services.userDAO.getUserEventTop(mUser,TopType.PET_POINT.value);
        if(uTop.getPoint()!=point.intValue()){
            uTop.setPoint(point.intValue());
            uTop.update();
        }
        // check defteam
        CfgArena.reCalDefTeamArena(mUser, true);


    }

    void checkBuyQrError(MyUser mUser) {
        try {
            // mua thành công thì cộng vật phẩm và chức năng
            // check DB
            List<UserBuyQrEntity> uBuyQr = DBJPA.getList("user_buy_qr", Arrays.asList("user_id", mUser.getUser().getId(), "status", StatusType.PROCESSING.value), "", UserBuyQrEntity.class);
            if (uBuyQr.size() == 0) return;
            UserEventEntity uEvent = mUser.getUEvent();
            EventInt uInt = uEvent.getEventInt();
            boolean update = false;
            int day = Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
            List<Integer> nap = uEvent.getQuaNapTien();
            for (int i = 0; i < uBuyQr.size(); i++) {
                UserBuyQrEntity uBuy = uBuyQr.get(i);
                ResIAPEntity rPack = ResIAP.getIAP(uBuy.getPackId());
                if(rPack==null)continue;
                if ( rPack.getId() == ResIAP.IAP_ID_FIRST_PURCHASE)
                    uInt.setValueAndUpdate(EventInt.TIME_BUY_FIRST_PURCHASE, DateTime.getNumberDay());
                List<Long> bonus = rPack.getABonus();
                // check x2
                List<Integer> lstPackBuy = mUser.getUser().getListPackBuy();
                boolean x2 = !lstPackBuy.contains(rPack.getId());
                if (x2) {
                    lstPackBuy.add(rPack.getId());
                    if (mUser.getUser().update(Arrays.asList("pack_buy", StringHelper.toDBString(lstPackBuy)))) {
                        bonus = Bonus.xBonus(bonus, 2);
                        mUser.getUser().setPackBuy(StringHelper.toDBString(lstPackBuy));
                    }
                }
                // add vip exp
                bonus.addAll(Bonus.viewVipExp(rPack.getVipExp()));
                uBuy.setStatus(StatusType.DONE.value);
                if (DBJPA.update(uBuy)) {
                    DBJPA.rawSQL(DBHelper.sqlMail(mUser.getUser().getId(), String.format(Lang.getTitle(mUser,Lang.mail_pack_bonus), rPack.getName()), StringHelper.toDBString(bonus)));
                    // save vào log để thống kê
                    LogBuyIAPEntity pack = LogBuyIAPEntity.builder().userId(mUser.getUser().getId()).packId(rPack.getId()).serverId(mUser.getUser().getServer()).price(rPack.getPrice()).descc("bu qua iap qr").status(YesNo.yes.value).build();
                    pack.save();
                }
            }
            if (uEvent.hasQuaNapTien()) {
                if (uEvent.getDayBuyIap() != day) {
                    for (int j = 0; j < nap.size(); j++) {
                        if (nap.get(j) == StatusType.PROCESSING.value) {
                            nap.set(j, StatusType.RECEIVE.value);
                            update = true;
                            break;
                        }
                    }
                }
            }
            int numBuy = uEvent.getNumBuyIap() + 1;
            if (update && uEvent.update(Arrays.asList("qua_nap_tien", StringHelper.toDBString(nap), "day_buy_iap", day, "num_buy_iap", numBuy))) {
                uEvent.setQuaNapTien(nap.toString());
                uEvent.setNumBuyIap(numBuy);
                uEvent.setDayBuyIap(day);
            } else {
                if (uEvent.update(Arrays.asList("num_buy_iap", numBuy))) {
                    uEvent.setNumBuyIap(numBuy);
                }
            }
        } catch (Exception e) {
            Logs.error(e);
        }
    }
}
