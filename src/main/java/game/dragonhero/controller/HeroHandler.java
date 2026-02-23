package game.dragonhero.controller;

import game.config.aEnum.AvatarIndex;
import game.battle.type.StateType;
import game.config.lang.Lang;
import game.dragonhero.mapping.UserHeroEntity;
import game.dragonhero.mapping.UserItemEquipmentEntity;
import game.dragonhero.server.IAction;
import game.dragonhero.service.resource.ResItem;
import game.protocol.CommonProto;
import io.netty.channel.Channel;
import ozudo.base.helper.GsonUtil;
import ozudo.base.helper.NumberUtil;
import ozudo.base.helper.StringHelper;
import ozudo.base.log.Logs;
import protocol.Pbmethod;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class HeroHandler extends AHandler {
    @Override
    public void initAction(Map<Integer, AHandler> mHandler) {
        List<Integer> actions = Arrays.asList(HERO_LIST, CHANGE_HERO);
        actions.forEach(action -> mHandler.put(action, this));
    }

    @Override
    public AHandler newInstance() {
        return new HeroHandler();
    }

    static HeroHandler instance;

    public static HeroHandler getInstance() {
        if (instance == null) {
            instance = new HeroHandler();
        }
        return instance;
    }

    @Override
    public void handle(Channel channel, String session, int actionId, byte[] requestData) {
        super.handle(channel, session, actionId, requestData);
        try {
            switch (actionId) {
                case IAction.HERO_LIST -> heroList();
                case IAction.CHANGE_HERO -> changeHero();
            }
        } catch (Exception ex) {
            Logs.error(ex);
        }
    }

    private void heroList() {
        Pbmethod.PbListHero.Builder lstHero = Pbmethod.PbListHero.newBuilder();
        List<UserHeroEntity> aHero = mUser.getResources().getHeroes();
        for (int i = 0; i < aHero.size(); i++) {
            lstHero.addAHero(aHero.get(i).toProto());
        }
        addResponse(lstHero.build());
    }

    private void changeHero() {
        int idHero = (int) CommonProto.parseCommonVector(requestData).getALong(0);
        UserHeroEntity hero = mUser.getResources().getHero(idHero);
        if (hero == null) {
            addErrResponse(getLang(Lang.hero_not_own));
            return;
        }

        List<Integer> avatar = user.getAvatar();
        int mainHero = avatar.get(AvatarIndex.HERO.value);
        if (mainHero == idHero) {
            addErrResponse(getLang(Lang.err_hero_use));
            return;
        }
        // chuyển trang bị sang thằng kia -- ban update sau
//        UserHeroEntity heroMain = mUser.getResources().getHero(mainHero);
//        List<Integer> lst = NumberUtil.genListInt(ResItem.sizeItemEquipment, 0) ;;
//        List<Integer> lstItem = heroMain.getListIdEquipmentEquip();
//        for (int i = 0; i < lstItem.size(); i++) {
//            UserItemEquipmentEntity uItem = mUser.getResources().getItemEquipment(lstItem.get(i));
//            if (uItem != null) uItem.unEquip();
//        }
//        if (hero.updateItemEquip(lst)) {
//            hero.setItemEquipment(lst.toString());
//            Pbmethod.ListCommonVector.Builder pb = Pbmethod.ListCommonVector.newBuilder();
//            mUser.getUser().updateItemEquip(lst);
//            pb.addAVector(user.reCalculatePoint(mUser).toCommonVector());
//            pb.addAVector(getCommonIntVector(hero.getListIdEquipmentEquip()));
//            addResponse(IAction.ITEM_EQUIPMENT_UN_EQUIP, pb.build());
//        } else {
//            addErrResponse();
//            return;
//        }


        avatar.set(AvatarIndex.HERO.value, idHero);
        if (user.update(Arrays.asList("avatar", StringHelper.toDBString(avatar), "item_equipment", StringHelper.toDBString(hero.getItemEquipment())))) {
            user.setAvatar(avatar.toString());
            user.setItemEquipment(hero.getItemEquipment().toString());
            mUser.getPlayer().protoStatus(StateType.UPDATE_AVATAR, GsonUtil.toListLong(avatar));
            addResponse(getCommonVector(idHero));
            mUser.reCalculatePoint();


        } else addErrResponse();


    }
}
