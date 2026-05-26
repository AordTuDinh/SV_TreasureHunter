package game.cache;

import game.config.CfgServer;
import game.config.aEnum.TopType;
import game.treasure.controller.UserEventTopEntity;
import game.treasure.mapping.UserPetEntity;
import game.treasure.mapping.main.ResPetEntity;
import game.treasure.server.App;
import game.treasure.server.AppInit;
import ozudo.base.database.DBJPA2;
import ozudo.base.database.DBResource;
import ozudo.base.helper.GUtil;
import ozudo.base.log.Logs;

import javax.persistence.EntityManager;
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
            mPet.put(pet.getId(), pet);
        });

    }

    protected EntityManager getEntityManager() {
        return DBJPA2.getEntityManager();
    }

    protected void closeSession(EntityManager session) {
        DBJPA2.closeSession(session);
    }
    
}
