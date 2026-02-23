package game.dragonhero.service.resource;

import game.config.CfgServer;
import game.dragonhero.mapping.UserPartyEntity;
import ozudo.base.database.DBResource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResParty {
    public static final int MAX_MEMBER =3;
   public static Map<Integer, UserPartyEntity> mPartyMap = new HashMap<>();


    public static UserPartyEntity getParty(int partyId) {
        return mPartyMap.get(partyId);
    }

    public static void init() {
        List<UserPartyEntity> aParty = DBResource.getInstance().getList(CfgServer.DB_DSON + "user_party", UserPartyEntity.class);
        aParty.forEach(party -> mPartyMap.put(party.getUserId(), party));
    }
}
