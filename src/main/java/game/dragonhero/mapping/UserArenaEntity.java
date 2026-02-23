package game.dragonhero.mapping;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import game.battle.object.BattleTeam;
import game.battle.object.HeroBattle;
import game.battle.object.WeaponBattle;
import game.config.CfgArena;
import game.config.lang.Lang;
import game.dragonhero.BattleConfig;
import game.monitor.Online;
import game.object.DataDaily;
import game.object.MyUser;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.checkerframework.checker.units.qual.m;
import ozudo.base.database.DBJPA;
import ozudo.base.helper.DateTime;
import ozudo.base.helper.GsonUtil;
import ozudo.base.helper.NumberUtil;
import ozudo.base.helper.StringHelper;
import protocol.Pbmethod;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@Table(name = "user_arena")
@Entity
public class UserArenaEntity implements Serializable {
    @Id
    int userId;
    int server;
    int arenaPoint, activeArena;
    String defenseTeam; // [id-point]

    @Transient
    BattleTeam defTeam;
    @Transient
    List<Integer> opps;
    @Transient
    int rank;
    @Transient
    int hasSetDef =-1;

    public UserArenaEntity(UserEntity user) {
        this.userId = user.getId();
        this.server = user.getServer();
        this.activeArena = 0;
        this.arenaPoint = 1000;
    }

    public BattleTeam getDefTeam() {
        if (defenseTeam ==null ||  defenseTeam.isEmpty()) return null;
        if (defTeam == null) defTeam = new Gson().fromJson(defenseTeam, new TypeToken<BattleTeam>() {
        }.getType());

        for (int i = 0; i < 3; i++) {
            if (defTeam.getBattleHeroes()[i] != null) defTeam.getBattleHeroes()[i].getPoint().resetHpMp();
        }
        return defTeam;
    }

    public void addArenaPoint(int point) {
        arenaPoint += point;
    }

    public protocol.Pbmethod.PbArena toProto(MyUser mUser) {
        protocol.Pbmethod.PbArena.Builder pb = protocol.Pbmethod.PbArena.newBuilder();
        pb.setTimeRemain(DateTime.getSecondsToNextWeek());
        pb.setMyPoint(getArenaPoint());
        pb.setMyRank(getRank());
        pb.setFeeTicket(CfgArena.config.feeTicket);
        pb.setCurBuyTicket(mUser.getDataDaily().getValue(DataDaily.NUMBER_BUY_TICKET_ARENA));
        pb.setMaxBuyTicket(CfgArena.config.maxBuyTicket);
        if (opps == null || opps.isEmpty() || opps.size() < 8) {
            opps = findOpponents(mUser.getUser());
        }
        for (int i = 0; i < opps.size(); i++) {
            UserEntity uOp = Online.getDbUser(opps.get(i));
            UserArenaEntity uArena = Online.getDbUserArena(opps.get(i));
            if (uOp != null && uArena != null) {
                pb.addOpponents(uOp.toProto(uArena.arenaPoint));
            }
        }
        boolean isSetDef = getHasSetDefTeam();
        if(!isSetDef){ // set team mac dinh
            setDefDefault(mUser);
            hasSetDef = 1;
        }
        pb.setHasDefense(true);
        pb.addAllDefenseTeam(toProtoDefTeamId());
        return pb.build();
    }

    private void setDefDefault(MyUser mUser) {
        List<Integer> inputs = NumberUtil.genListInt(14,0);
        inputs.set(0,mUser.getUser().getHeroMain());
        inputs.set(1,1);
        inputs.set(2,2);
        inputs.set(3,3);
        inputs.set(3,3);
        inputs.set(13,1);
        int team = 2; // team def
        BattleTeam battleTeam = new BattleTeam();
        // monster
        UserPetEntity monster = mUser.getResources().getMPetMonster().get(inputs.get(BattleConfig.monsterIndex).intValue());
        if (monster != null) {
            battleTeam.getBattleHeroes()[4] = monster.toHeroBattle(team, CfgArena.SLOT_T2_MONSTER);
        }
        // hero
        int step = 4;//heroId  - [weapon]x3
        for (int i = 0; i < BattleConfig.sizeHeroArena; i += step) { // hero ID - weponId x3
            UserHeroEntity userHero = mUser.getResources().getHero(inputs.get(i));
            if (userHero == null) continue;
            WeaponBattle[] weaponBattles = new WeaponBattle[3];
            for (int j = 0; j < 3; j++) {
                UserWeaponEntity u = mUser.getResources().getWeapon(Math.toIntExact(inputs.get(i + j + 1)));
                weaponBattles[j] = u.toWeaponBattle(mUser.getUser().getCachePoint(), j);
            }
            int slot = i / step;
            battleTeam.getBattleHeroes()[slot] = userHero.toHeroBattle(team, 6 + slot, slot, weaponBattles, monster);
        }
        String data = StringHelper.toDBString(battleTeam);
        if (update(List.of("defense_team", data, "active_arena", 1))) {
            setDefenseTeam(data);
            setActiveArena(1);
            setDefTeam(battleTeam);
            Online.cacheUserArena(this);
        }
    }


    public boolean getHasSetDefTeam() {
        if (hasSetDef == -1) {
            BattleTeam battleTeam = getDefTeam();
            if (battleTeam == null) hasSetDef = 0;
            else {
                hasSetDef = Arrays.stream(battleTeam.getBattleHeroes()).map(i -> i.getAvatar() != 0).count() > 0 ? 1 : 0; // check da set def chua
            }

        }
        return hasSetDef == 1;
    }

    public List<Integer> toProtoDefTeamId() {
        List<Integer> ret = new ArrayList<>();
        BattleTeam data = getDefTeam();
        if (data == null) return NumberUtil.genListInt(BattleConfig.maxSizeInputArena, 0);
        for (int i = 0; i < 3; i++) {
            HeroBattle hero = data.getBattleHeroes()[i];
            if (hero != null) {
                WeaponBattle[] shurikens = hero.getWeaponBattles();
                if (shurikens != null && hero.getWeaponBattles().length > 2)
                    ret.addAll(List.of(hero.getAvatar(), hero.getWeaponBattles()[0].getId(), hero.getWeaponBattles()[1].getId(), hero.getWeaponBattles()[2].getId()));
                else ret.addAll(List.of(hero.getAvatar(), 0, 0, 0));
            } else {
                ret.addAll(NumberUtil.genListInt(4, 0));
            }
        }
        if (data.getBattleHeroes()[3] != null) ret.add(data.getBattleHeroes()[3].getAvatar()); //pet
        else ret.add(0);
        if (data.getBattleHeroes()[4] != null) ret.add(data.getBattleHeroes()[4].getAvatar()); //monster
        else ret.add(0);
        return ret;
    }

    public boolean isActive() {
        return activeArena == 1;
    }

    public void setDefenseTeam(String data) {
        this.defenseTeam = data;
    }

    public List<Integer> findOpponents(UserEntity user) {
        //todo find opp
        List<UserArenaEntity> users = DBJPA.getSelectQuery("SELECT * FROM dson.user_arena WHERE SERVER =" + user.getServer() + " AND active_arena = 1 AND arena_point BETWEEN " + (getArenaPoint() - 1000) + " AND " + (getArenaPoint() + 1000) + " AND user_id!=" + userId + " ORDER BY RAND() LIMIT 10", UserArenaEntity.class);
        return users.stream().map(UserArenaEntity::getUserId).collect(Collectors.toList());
    }

    public int getRank() {
        if (rank == 0) {
            rank = DBJPA.getIntNumber("SELECT count(*) number FROM dson.user_arena WHERE server=" + server + " and arena_point > " + arenaPoint) + 1;
        }
        return rank;
    }

    public boolean update(List<Object> updateData) {
        return DBJPA.update("user_arena", updateData, Arrays.asList("user_id", userId));
    }

    public boolean updateDefTeam(BattleTeam battleTeam) {
        if (update(List.of("defense_team", StringHelper.toDBString(battleTeam)))) {
            this.defenseTeam = battleTeam.toString();
            this.defTeam = battleTeam;
            return true;
        }
        return false;
    }

    public Pbmethod.PbBattleListArenaHero toProtoDefTeam() {
        Pbmethod.PbBattleListArenaHero.Builder pb = Pbmethod.PbBattleListArenaHero.newBuilder();
        BattleTeam team = getDefTeam();
        if (team == null) return null;
        for (int i = 0; i < team.getBattleHeroes().length; i++) {
            if (team.getBattleHeroes()[i] != null) {
                Pbmethod.PbBattleArenaHero pbBattleArenaHero = team.getBattleHeroes()[i].toArenaProto();
                if (pbBattleArenaHero != null) pb.addTeam(pbBattleArenaHero);
            }
        }
        return pb.build();
    }
}
