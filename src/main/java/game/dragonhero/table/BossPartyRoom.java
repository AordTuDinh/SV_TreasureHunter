package game.dragonhero.table;

import game.battle.model.*;
import game.battle.model.Character;
import game.battle.object.Coroutine;
import game.battle.object.Point;
import game.battle.object.Pos;
import game.battle.type.CharacterType;
import game.battle.type.RoomState;
import game.config.CfgWorldBoss;
import game.config.aEnum.DetailActionType;
import game.config.aEnum.PopupType;
import game.dragonhero.mapping.UserWeekEntity;
import game.dragonhero.mapping.main.BaseMap;
import game.dragonhero.server.IAction;
import game.dragonhero.service.Services;
import game.dragonhero.service.resource.ResEnemy;
import game.dragonhero.service.user.Bonus;
import game.protocol.CommonProto;
import ozudo.base.helper.DateTime;
import ozudo.base.helper.GsonUtil;
import ozudo.base.helper.Util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BossPartyRoom extends BossGodRoom {
    protected static final float timeDelayInstanceBoss = 3f; // 3s sau thi cho boss xuat hien
    protected static final float timeOut = 123;

    List<Pos> aBossPos = List.of(new Pos(3, 5), new Pos(5, 2), new Pos(-5, 2), new Pos(-3, 5));
    int indexBoss = 0;
    boolean isSolo;
    Map<Integer, ResultInfo> dameByPlayer = new HashMap<>();

    public static List<List<Integer>> lstIdBoss = List.of(
            List.of(1, 5, 9, 13, 17),
            List.of(2, 6, 10, 14, 18),
            List.of(3, 7, 11, 15, 19),
            List.of(4, 8, 12, 16, 20)
    );
    List<BossGod> aBoss;

    public BossPartyRoom(BaseMap mapInfo, List<Character> aPlayer, String keyRoom, boolean isSolo) {
        super(mapInfo, aPlayer, keyRoom, 0);
        for (int i = 0; i < aPlayer.size(); i++) {
            aPlayer.get(i).addBuffBossGod(1.5f);
            dameByPlayer.put(aPlayer.get(i).getId(), new ResultInfo(aPlayer.get(i).getId()));
        }
       this.isSolo = isSolo;
    }


    @Override
    public void LastUpdate() {
        super.LastUpdate();
        if (aBoss != null) {
            for (int i = 0; i < aBoss.size(); i++) {
                if (aBoss.get(i) != null) aBoss.get(i).LastUpdate();
            }
        }
    }

    @Override
    public void Update1s() {
        super.Update1s();
        // check end game
        if (this.roomState == RoomState.END) return;
        boolean end = true;
        for (int i = 0; i < aPlayer.size(); i++) {
            if (aPlayer.get(i) != null && aPlayer.get(i).isPlayer()) {
                Player p = aPlayer.get(i).getPlayer();
                if (p.isAlive()) end = false;
            }
        }
        if (end) lostGame(false); // chết hết user
    }

    @Override
    protected BossGod bossData() {
        return null;
    }

    @Override
    protected Coroutine initBoss() {
        return new Coroutine(timeDelayInstanceBoss, () -> {
            // gen all boss
            aBoss = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                BossGod boss = genBossByIdGroup(i, lstIdBoss.get(i).get(0));
                aBoss.add(boss);
                aProtoAdd.add(boss.toProtoAdd());
            }
            // send proto
            aEnemy.addAll(aBoss);

            timeStartGame = System.currentTimeMillis();
            for (int i = 0; i < aPlayer.size(); i++) {
                if (aPlayer.get(i).isPlayer())
                    aPlayer.get(i).getPlayer().toPbStartGame(timeOut - timeDelayInstanceBoss);
            }
        });
    }


    private BossGod genBossByIdGroup(int group, int id) {
        BossGod boss = null;
        switch (group) {
            case 0:
                boss = new KimThan(ResEnemy.getBoss(id), aBossPos.get(0), team, this);
                break;
            case 1:
                boss = new ThuyThan(ResEnemy.getBoss(id), aBossPos.get(1), team, this);
                break;
            case 2:
                boss = new HoaThan(ResEnemy.getBoss(id), aBossPos.get(2), team, this);
                break;
            case 3:
                boss = new ThoThan(ResEnemy.getBoss(id), aBossPos.get(3), team, this);
                break;
        }
        return boss;
    }


    @Override
    public void characterDie(Character character) {
        if (character.getType() == CharacterType.BOSS_GOD) {
            addCoroutine(bossDie(1f, (BossGod) character));
            addCoroutine(initNewBoss(3f, (BossGod) character));
            ResultInfo ret = dameByPlayer.get(character.getKillById());
            if (ret != null) ret.killNum++;
        } else if (character.getType() == CharacterType.PLAYER) {
            if (aPlayer.stream().noneMatch(Character::isAlive)) {
                lostGame(false); // chết user
            }
        }
    }

    @Override
    public void EffectUpdate() {
        if (roomState != RoomState.ACTIVE) return;
        controller.EffectUpdate(this);
        if (isBattleRoom) {
            for (int i = 0; i < aPet.size(); i++) {
                aPet.get(i).getPet().processSkill();
            }
        }
        if (roomState == RoomState.ACTIVE && DateTime.isAfterTime(timeCreateRoom, timeOut)) {
            lostGame(true); // hết thời gian
        }
    }

    protected void lostGame(boolean isTimeUp) {
        setEndGameState();
        int maxKill = aPlayer.stream()
                .filter(Character::isPlayer)
                .map(Character::getPlayer)
                .filter(player -> player != null && player.getMUser().getChannel() != null)
                .mapToInt(player -> dameByPlayer.get(player.getId()).killNum)
                .sum();
        int rank = CfgWorldBoss.getRank(maxKill);
        List<Long> bonus = CfgWorldBoss.getBonusByRank(rank);

        for (int i = 0; i < aPlayer.size(); i++) {
            if (!aPlayer.get(i).isPlayer()) continue;
            Player player = aPlayer.get(i).getPlayer();
            if (player != null && player.getMUser().getChannel() != null) {
                ResultInfo ret = dameByPlayer.get(player.getId());
                int per = (int) ((ret.killNum) * 100 / (float) maxKill);
                player.removeBuff();
                player.toPbEndGame(isTimeUp, per, Bonus.receiveListItem(player.getMUser(), DetailActionType.BONUS_BOSS_PARTY.getKey(), bonus), getTimeAttack(), PopupType.POPUP_END_BOSS_PARTY, List.of(isSolo?maxKill:ret.killNum, maxKill, rank));
                player.resetData();

                // add vào bảng xếp hạng
                if(isSolo){
                    UserWeekEntity uWeek = Services.userDAO.getUserWeek(player.getMUser());
                    uWeek.addDameBoss(maxKill);
                }
            }
        }

    }


    public Coroutine bossDie(float time, BossGod boss) {
        return new Coroutine(time, () -> {
            aBoss.remove(boss);
            aProtoAdd.add(boss.toProtoRemove());
            aEnemy.remove(boss);
        });
    }

    @Override
    public synchronized void FixedUpdate() {
        super.FixedUpdate();
        for (int i = 0; i < aPlayer.size(); i++) {
            if (aPlayer.get(i).getType() == CharacterType.BOT_PLAYER) ((BotPlayer) aPlayer.get(i)).processAuto();
        }
    }

    public Coroutine initNewBoss(float time, BossGod boss) {
        return new Coroutine(time, () -> {
            // thêm boss mới vào
            List<Integer> nextId = getNextId(boss.getBossId());
            indexBoss++; // boss hỏa thì cộng thêm dame
            BossGod newBoss = genBossByIdGroup(nextId.get(1), nextId.get(0));
            aBoss.add(newBoss);
            aProtoAdd.add(newBoss.toProtoAdd());
            aEnemy.add(newBoss);
        });
    }

    public void removeSupport(Support sp) {
        super.removeSupport(sp);
    }

    public void addSupport(Support sp) {
        supports.add(sp);
        aEnemy.add(sp);
        aProtoAdd.add(sp.toProtoAdd());
        for (int i = 0; i < aBoss.size(); i++) {
            aBoss.get(i).getPoint().add(Point.ATTACK, 10 * (1 + indexBoss));
        }
    }

    public static List<Integer> getNextId(int id) {
        for (int g = 0; g < lstIdBoss.size(); g++) {
            List<Integer> group = lstIdBoss.get(g);
            int index = group.indexOf(id);
            if (index != -1) {
                int next = (index + 1 < group.size())
                        ? group.get(index + 1)
                        : group.get(group.size() - 1);

                return List.of(next, g); // trả [nextId, groupIndex]
            }
        }
        return List.of(id, -1); // không thuộc group nào
    }

}


class ResultInfo {
    public int userId;
    public int killNum;

    public ResultInfo(int userId) {
        this.userId = userId;
        this.killNum = 0;
    }
}
