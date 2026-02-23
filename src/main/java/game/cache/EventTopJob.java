package game.cache;

import game.config.CfgServer;
import game.config.aEnum.ItemKey;
import game.config.aEnum.PetType;
import game.config.aEnum.TopType;
import game.dragonhero.controller.UserEventTopEntity;
import game.dragonhero.mapping.UserItemEntity;
import game.dragonhero.mapping.UserPetEntity;
import game.dragonhero.mapping.main.ResPetEntity;
import game.dragonhero.server.App;
import game.dragonhero.server.AppInit;
import ozudo.base.database.DBJPA2;
import ozudo.base.database.DBResource;
import ozudo.base.helper.GUtil;
import ozudo.base.log.Logs;

import javax.persistence.EntityManager;
import java.security.SecureRandom;
import java.util.*;

public class EventTopJob {

    public static void main(String args[]) throws Exception {
        new EventTopJob().process();
        System.exit(0);
    }

    private void process() {
        try {
            AppInit.initAll();
            App.initConfig();
            processGiftCode();
        } catch (Exception ex) {
            String exception = GUtil.exToString(ex);
            Logs.error(exception);
        }
    }


    void processGiftCode(){

        Map<Integer, ResPetEntity> mPet = new HashMap<>();
        List<ResPetEntity> aPet = DBResource.getInstance().getList(CfgServer.DB_MAIN + "res_pet", ResPetEntity.class);
        aPet.forEach(pet -> {
            pet.init();
            mPet.put(pet.getId(), pet);
        });

        Map<Integer,Integer> map = new HashMap<>();
        List<UserPetEntity> uPet = DBResource.getInstance().getList(CfgServer.DB_DSON + "user_pet", Arrays.asList( "type" ,2 , "server",1),"", UserPetEntity.class);

        for (int i = 0; i < uPet.size(); i++) {
            ResPetEntity res=  mPet.get(uPet.get(i).getPetId());
            int addPoint = res.getRank() * (uPet.get(i).getStar()+1);
            map.merge(uPet.get(i).getUserId(), addPoint, Integer::sum);
        }

        map.forEach((userId,point)->{
            UserEventTopEntity uTop = new UserEventTopEntity(userId, TopType.PET_POINT.value, 1, point);
            DBResource.getInstance().saveOrUpdate(uTop);
        });

    }

    protected EntityManager getEntityManager() {
        return DBJPA2.getEntityManager();
    }

    protected void closeSession(EntityManager session) {
        DBJPA2.closeSession(session);
    }
    
}
