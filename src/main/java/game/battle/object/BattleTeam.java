package game.battle.object;


import game.dragonhero.BattleConfig;
import lombok.Data;
import lombok.NoArgsConstructor;
import protocol.Pbmethod;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class BattleTeam implements Serializable {
    HeroBattle[] battleHeroes; //size5:  hero 1,2,3 pet, monster

    public BattleTeam() {
        this.battleHeroes = new HeroBattle[BattleConfig.maxSlotArena];
    }

    public BattleTeam(HeroBattle[] battleHeroes) {
        this.battleHeroes = battleHeroes;
    }


    public protocol.Pbmethod.PbBattleListArenaHero toProto() {
        protocol.Pbmethod.PbBattleListArenaHero.Builder pb = protocol.Pbmethod.PbBattleListArenaHero.newBuilder();
        for (int i = 0; i < battleHeroes.length; i++) {
            if (battleHeroes[i] != null) {
                Pbmethod.PbBattleArenaHero hero = battleHeroes[i].toArenaProto();
                if (hero != null) {
                    Pbmethod.PbBattleArenaHero ret = hero;
                    if (ret != null) pb.addTeam(ret);
                }
            }
        }
        return pb.build();
    }
}
