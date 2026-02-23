package game.dragonhero.service.resource;

import game.config.CfgServer;
import game.config.aEnum.TopType;
import game.dragonhero.controller.UserEventTopEntity;
import game.dragonhero.mapping.UserPetEntity;
import game.dragonhero.mapping.main.ConfigHelpEntity;
import game.dragonhero.mapping.main.ResEventTopEntity;
import game.dragonhero.service.Services;
import game.object.MyUser;
import ozudo.base.database.DBResource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResEventTop {
    public static Map<Integer, ResEventTopEntity> mEventTop = new HashMap<>();
    public static List<ResEventTopEntity> aEvent ;


    public static ResEventTopEntity getResEventTop(int eventType){
       return mEventTop.get(eventType);
    }

    public static void init() {
         aEvent = DBResource.getInstance().getList(CfgServer.DB_MAIN + "res_event_top", ResEventTopEntity.class);
        mEventTop.clear();
        aEvent.forEach(e -> {
            e.init();
            mEventTop.put(e.getEventType(), e);
        });
    }


    public static void checkEvent(MyUser myUser, UserPetEntity uPet, TopType topType) {
        //todo check in event
        boolean hasEvent = aEvent.stream().anyMatch(e -> e.getEventType() == TopType.PET_POINT.value);
        if(hasEvent){
            UserEventTopEntity uEventTop = Services.userDAO.getUserEventTop(myUser,topType.value);
            if(uEventTop!=null){
                int point = uPet.getResPet().getRank();
                uEventTop.addPoint(point);
                uEventTop.update();
            }
        }


    }
}
