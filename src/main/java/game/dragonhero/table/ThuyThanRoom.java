package game.dragonhero.table;

import game.battle.effect.EffectRoom;
import game.battle.effect.SkillEffect;
import game.battle.model.BossGod;
import game.battle.model.Character;
import game.battle.model.ThuyThan;
import game.battle.object.Bullet;
import game.battle.type.CharacterType;
import game.dragonhero.mapping.main.BaseMap;
import game.dragonhero.mapping.main.ResBossEntity;
import game.dragonhero.service.battle.EffectStatus;
import game.dragonhero.service.battle.EffectType;
import game.dragonhero.service.resource.ResEnemy;

import java.util.ArrayList;
import java.util.List;


public class ThuyThanRoom extends BossGodRoom {
    //    private static final int delayInitTotem = 6;
    List<Float> dameSkill1 = List.of(100f, 100f);
    List<Float> sizeElip = List.of(1.7f, 0.6f);

    public ThuyThanRoom(BaseMap mapInfo, List<Character> aPlayer, String keyRoom, int mode) {
        super(mapInfo, aPlayer, keyRoom, mode);
    }

    @Override
    protected BossGod bossData() {
        ResBossEntity bossData = ResEnemy.getBoss(Math.toIntExact(getBossId()));
        return new ThuyThan(bossData, bossData.getInstancePos(), team,  this);
    }

    @Override
    protected void processTriggerDestroy(Bullet bullet) {
        super.processTriggerDestroy(bullet);
        if (bullet.getOwner().getType() == CharacterType.BOSS_GOD) {
            SkillEffect skill = new SkillEffect();
            skill.setTime(0);
            skill.setHasEffect(true);
            skill.setEffectType(EffectType.THUY_THAN_1);
            skill.setTimeDelayDame(0f);
            skill.setValues(new ArrayList<>(dameSkill1));
            EffectRoom effectRoom = new EffectRoom(bullet.getOwner(), bullet.getPos().clone(), skill);
            effectRoom.setSizeElip(sizeElip);
            addEffectRoom(effectRoom);
        }
    }
}
