package game.monitor;

import game.cache.CacheStoreBeans;
import game.dragonhero.dao.ClanDAO;
import game.dragonhero.dao.UserDAO;
import game.dragonhero.mapping.ClanEntity;
import game.dragonhero.mapping.UserEntity;
import game.dragonhero.service.Services;
import game.dragonhero.service.user.Actions;
import ozudo.base.database.DBJPA;
import ozudo.base.log.Logs;

import java.util.List;

public class ClanManager {
    static ClanDAO clanDAO = Services.clanDAO;
    static UserDAO userDAO = Services.userDAO;
    public ClanEntity clan;

    public ClanManager(ClanEntity clan) {
        clan.initChat();
        int member = DBJPA.count("user", "clan", String.valueOf(clan.getId()));
        if (member > 0) clan.setMember(member);
        this.clan = clan;
    }

    public static synchronized ClanManager getInstance(int clanId) {
        if (clanId == 0) return null;
        ClanManager clanManager = CacheStoreBeans.cacheClanManager.get(String.valueOf(clanId));
        if (clanManager == null) {
            ClanEntity clan = clanDAO.getClan(clanId);
            if (clan != null) {
                CacheStoreBeans.cacheClanManager.add(String.valueOf(clanId), new ClanManager(clan));
            }
        }
        return CacheStoreBeans.cacheClanManager.get(String.valueOf(clanId));
    }

    public ClanEntity getClan() {
        return clan;
    }

    public void destroyClan(UserEntity user) {
        try {
            if (clanDAO.getListMember(clan.getId()).isEmpty()) {
                int curClan = user.getClan();
                clanDAO.destroyClan(clan.getId());
                Actions.save(user, Actions.GCLAN, Actions.DDESTROY, "clan", curClan, "member", "0");
                CacheStoreBeans.cacheClanManager.remove(String.valueOf(curClan));
            }
        } catch (Exception ex) {
            Logs.error("destroy clan fail id=" + clan.getId(), ex);
        }
    }

    public List<UserEntity> getListMember() {
        return clanDAO.getListMember(clan.getId());
    }

    public static int getClanAvatar(int clanId) {
        ClanManager man = getInstance(clanId);
        return man == null ? 0 : man.clan.getId();
    }
}
