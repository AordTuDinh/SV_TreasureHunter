package game.monitor;

import game.dragonhero.mapping.ClanEntity;
import lombok.Data;
import lombok.NonNull;
import ozudo.base.database.DBJPA;

@Data
public class ClanSync {
    @NonNull
    private int clanId;
    private ClanEntity clan;

    public synchronized ClanEntity getClan(int clanId) {
        if (clanId == 0) return null;
        if (clan == null) {
            clan = (ClanEntity) DBJPA.getUnique("clan", ClanEntity.class, "id", clanId);
            if (clan != null) {
                clan.initChat();
                int member = DBJPA.count("user", "clan", String.valueOf(clan.getId()));
                if (member > 0) clan.setMember(member);
            }
        }
        return clan;
    }
}
