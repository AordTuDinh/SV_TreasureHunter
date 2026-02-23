package game.dragonhero.table;

import game.battle.model.Character;
import game.battle.model.Enemy;
import game.battle.model.Player;
import game.battle.object.Coroutine;
import game.battle.object.Pos;
import game.battle.type.RoomState;
import game.config.aEnum.DetailActionType;
import game.config.aEnum.ItemKey;
import game.config.aEnum.QuestTutType;
import game.dragonhero.BattleConfig;
import game.dragonhero.mapping.UserItemEntity;
import game.dragonhero.mapping.main.BaseMap;
import game.dragonhero.mapping.main.ResCampaignEntity;
import game.dragonhero.mapping.main.ResEnemyEntity;
import game.dragonhero.server.IAction;
import game.dragonhero.service.resource.ResEnemy;
import game.dragonhero.service.user.Bonus;
import game.object.MyUser;
import game.protocol.CommonProto;
import lombok.NoArgsConstructor;
import ozudo.base.helper.DateTime;
import ozudo.base.helper.Util;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
public class CampaignRoom extends BaseBattleRoom {
    long timeCacheCheckBuff = 0;

    public CampaignRoom(BaseMap mapInfo, List<Character> aPlayer, String keyRoom) {
        super(mapInfo, aPlayer, keyRoom, true);
        timeCacheCheckBuff = System.currentTimeMillis();
    }

    protected void startInit() {
        super.startInit();
        this.aEnemy = initEnemy();
        addBossCampaign();
        roomState = RoomState.ACTIVE;
    }

    @Override
    public void characterDie(Character characterDie) {
        super.characterDie(characterDie);
        if (characterDie.isPlayer()) {
            addCoroutine(new Coroutine(0.8f, () -> characterDie.getPlayer().toPbEndGame()
            ));
        }
    }

    @Override
    public void Update1s() {
        super.Update1s();
        // 10s check buff 1 lần
        if (timeCacheCheckBuff < System.currentTimeMillis() + DateTime.SECOND2_MILLI_SECOND * 10) {
            for (int i = 0; i < aPlayer.size(); i++) {
                aPlayer.get(i).getPlayer().CheckUpdateBuff();
            }
        }
    }

    public void addBossCampaign() { // for tutorial
        ResEnemyEntity enemy = ResEnemy.getEnemy(5);
        int mapId = Integer.parseInt(getKeys()[1]);
        Enemy boss = new Enemy(enemy, new Pos(8, -1), Pos.RandomDirection(), 2, this);
        if (mapId == 1 && enemy.getId() == 5) {  // init custom map tutorial
            aEnemy.add(boss);
            aProtoAdd.add(boss.toProtoAdd());
        }
    }

    public List<Character> initEnemy() {
        List<Character> monsters = new ArrayList<>();
        int indexEnemy = 0;
        ResCampaignEntity map = (ResCampaignEntity) mapInfo;
        List<Integer> listEnemy = map.getListEnemy();
        int mapId = getSubId();
        for (int i = 0; i < listEnemy.size(); i += 2) {
            ResEnemyEntity enemy = ResEnemy.getEnemy(listEnemy.get(i));
            int numberEnemy = listEnemy.get(i + 1);
            for (int j = 0; j < numberEnemy; j++) {
                indexEnemy += 1;
                // dang config 30 quai, hon thi cho ramdom vay
                Pos startPos = BattleConfig.mapEnemy.get(indexEnemy);
                Pos pos = new Pos(startPos.x + map.getMapData().getBotLeft().x, startPos.y + map.getMapData().getBotLeft().y);
                if (mapId == 1 && enemy.getId() == 5) {  // init custom map tutorial
                    pos = new Pos(9, -3);
                }
                Enemy bot = new Enemy(enemy, pos, Pos.RandomDirection(), 2, this);
                monsters.add(bot);
            }
        }
        return monsters;
    }
}
