package game.battle.model;

import game.battle.calculate.MathLab;
import game.battle.effect.Effect;
import game.battle.effect.EffectRoom;
import game.battle.effect.SkillEffect;
import game.battle.object.*;
import game.battle.type.CharacterType;
import game.battle.type.EffectBodyType;
import game.battle.type.StateType;
import game.config.aEnum.FactionType;
import game.config.aEnum.RankType;
import game.dragonhero.BattleConfig;
import game.dragonhero.service.battle.TriggerType;
import game.dragonhero.table.ArenaRoom;
import game.dragonhero.table.BaseBattleRoom;
import lombok.Data;
import ozudo.base.helper.NumberUtil;
import protocol.Pbmethod;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Data
public class ArenaHero extends Character {
    long timeAttack;
    ArenaHero targetAttack;
    long timePetAttack;
    int skill;
    SkillEffect petSkills;
    float timePetActive;

    public ArenaHero(int id, int teamId, Pos pos, Point point, CharacterType type, ArenaRoom room) {
        this.id = id;
        this.room = room;
        this.teamId = teamId;
        this.radius = BattleConfig.C_Collider + 0.2f;
        this.pos = pos;
        this.point = point;
        this.alive = true;
        this.type = type;
        this.effectsBody = new ArrayList<>();
        this.weaponEquip = new ArrayList<>();
        this.skill = -1;
        this.panelMap = new PanelMap(new Pos(-10f, -10f), new Pos(10, 10));
        this.timePetAttack = System.currentTimeMillis();
        this.direction = teamId == 1 ? Pos.right() : Pos.left();
    }

    public SkillEffect getPetSkills() {
        return petSkills;
    }

    public void setPetData(SkillEffect petSkills, float timeActive) {
        this.petSkills = petSkills;
        this.timePetActive = timeActive;
    }

    private void nextSkill() {
        skill++;
        skill = skill >= 3 ? 0 : skill;
        timeAttack = System.currentTimeMillis();
    }

    public boolean isLikeFace(Pos newDirection) { // check lật mặt
        return direction.x * newDirection.x > 0;
    }

    @Override
    public Pbmethod.PbUnitAdd.Builder toProtoAdd() {
        return null;
    }

    public boolean beBlock() {
        return point.beBlock();
    }

    @Override
    public void activeSkill(int skillId) {

    }

    public List<Bullet> attackTarget() {
        nextSkill();
        List<Bullet> bullets = new ArrayList<>();
        Shuriken shu = weaponEquip.get(skill);
        if (getTargetAttack() == null) return bullets;
        Pos direction = MathLab.getDirection(getPos(), getTargetAttack().getPos());
        bullets.addAll(skillArenaActive(shu, direction, pos, shu.getDegree()));
        shu.setActiveSkill();
        return bullets;
    }

    @Override
    public void setDirection(Pos direction) {
        this.direction = direction;
        protoStatus(StateType.CHANGE_HERO_ARENA_DIRECTION, (long) (direction.x * 1000), (long) (direction.y * 1000));
    }

    public List<Bullet> skillArenaActive(Shuriken shuriken, Pos direction, Pos position, int degree) {
        // check trigger init
        List<Bullet> bulletInit = aTriggerInit(shuriken, direction, position);
        if (bulletInit != null) return bulletInit;
        // không có init thì khởi tạo như thường
        int row = shuriken.getShots().get(1);
        List<Bullet> bullets = new ArrayList<>();
        Bullet bullet = null;
        Pos pos = null;
        Pos dir = null;
        float off = 4f, off1 = 6f;
        for (int i = 1; i <= row; i++) {
            dir = MathLab.angle2Direction(30, direction);
            pos = new Pos(position.x - dir.x * i / off, position.y + BattleConfig.P_offsetYColTop);
            bullet = new Bullet(this, direction, pos, shuriken);
            bullets.add(bullet);
        }
        if (bullet != null) {
            SkillEffect effectSkill = bullet.getEffectSkill().clone();
            effectSkill.setMP(0);
            bullet.setEffectSkill(effectSkill);
        }
        Bullet bullet0 = bullets.get(0);
        if (bullet0.isTrigger3TH()) processTriggerLastInit(bullets.get(0), TriggerType.INIT3, shuriken);
        if (bullet0.isTrigger4TH()) processTriggerLastInit(bullets.get(0), TriggerType.INIT4, shuriken);
        if (bullet0.isTrigger5TH()) processTriggerLastInit(bullets.get(0), TriggerType.INIT5, shuriken);
        return bullets;
    }


    void processTriggerLastInit(Bullet bullet, TriggerType triggerType, Shuriken shuriken) {
        SkillEffect eff = bullet.getEffectSkill();
        if (!eff.isHasEffect() || !triggerType.equals(eff.getTriggerType())) return;
        if (bullet.getOwner().point.getCurMP() >= eff.getMP()) {
            switch (triggerType) {
                case INIT3:
                    switch (eff.getEffectType()) {
                        case SMOKE: // create
                            EffectRoom effectRoom = new EffectRoom(this, getPos().clone(), eff);
                            getBattleRoom().addEffectRoom(effectRoom);
                            shuriken.setBlockCountTime(eff.getTime());
                            break;
                        case BERSERK:  // create
                            addEffectNow(new Effect(bullet.getOwner(), eff, bullet.getFaction()));
                            shuriken.setBlockCountTime(eff.getTime());
                            break;
                        case RUNA:  // create
                            addEffectNow(new Effect(bullet.getOwner(), eff, bullet.getFaction()));
                            break;
                    }
                    break;
                case INIT4:
                case INIT5:
                    switch (eff.getEffectType()) {
                        case RE_HP_INIT:  // create
                            Effect effectBody = new Effect(this, eff, FactionType.get(shuriken.getFaction()));
                            addEffectTime(effectBody);
                            long reHp = (long) (eff.getValuePerIndex(2) * point.getMaxHp() + eff.getValuePerIndex(3) * point.getMagicDamage());
                            long reMax = (long) (BattleConfig.S_maxReHp50 / 100f * point.getMaxHp());
                            reHp = reHp > reMax ? reMax : reHp;
                            reHp(reHp);
                            break;
                    }
                    break;
            }
        }
    }

    List<Bullet> aTriggerInit(Shuriken shuriken, Pos direction, Pos position) {
        SkillEffect eff = shuriken.getEffectSkill();
        if (!eff.isHasEffect() || eff.getTriggerType() != TriggerType.INIT) return null;
        if (point.getCurMP() >= eff.getMP()) {
            if (eff.getMP() > 0) updateMp(-eff.getMP());
            switch (eff.getEffectType()) {
                case RANDOM: // create
                    int randSlot = NumberUtil.getRandomDif(3, shuriken.getSlot());
                    if (shuriken.getRank().value < RankType.DIVINE.value) {
                        Shuriken shu = weaponEquip.get(randSlot);
                        return skillActive(shu, direction, position, shu.getDegree());
                    } else {
                        List<Bullet> bullets = new ArrayList<>();
                        int dir = direction.x >= 0 ? 1 : -1;
                        Pos pos = new Pos(position.x + BattleConfig.P_offsetXRow2 * dir, position.y + BattleConfig.P_offsetYColTop);
                        bullets.add(new Bullet(this, direction, pos.clone(), shuriken));
                        return bullets;
                    }
//                case DECO:  // create // giảm mẹ chỉ số luôn cho nhanh, eff loằng ngoằng
//                    List<Long> timeCd = new ArrayList<>();
//                    for (int i = 0; i < weaponEquip.size(); i++) {
//                        weaponEquip.get(i).decCoolDown(eff.getFirstPer());
//                        timeCd.add(weaponEquip.get(i).getCoolDown());
//                    }
                    // trả về effect cho client update time CD
//                    protoStatus(StateType.UPDATE_COOL_DOWN, timeCd);
//                    protoStatus(StateType.EFFECT_BODY, Arrays.asList(EffectBodyType.DECO.value, 1000L));
//                    break;
                case RE_HP_INIT:  // create
                    Effect effectBody = new Effect(this, eff, FactionType.get(shuriken.getFaction()));
                    addEffectTime(effectBody);
                    long reHp = (long) (eff.getValuePerIndex(2) * point.getMaxHp() + eff.getValuePerIndex(3) * point.getMagicDamage());
                    long reMax = (long) (BattleConfig.S_maxReHp50 / 100f * point.getMaxHp());
                    reHp = reHp > reMax ? reMax : reHp;
                    reHp(reHp);
                    break;
            }
        }
        return null;
    }

    public void setTimePetAttack() {
        this.timePetAttack = System.currentTimeMillis();
    }

    public void protoDie(Character killer) {
//        unTargetAll();
        this.killById = killer.getId();
        room.characterDie(this);
        protoStatus(StateType.DIE, (long) FactionType.NULL.value);
    }

    public void addEffectTime(Effect effect) { // k xử lí các effect point dame,magicDame
        if (effect.getSkill().getEffectType().isIncreBody) { // cộng dồn trên character
            effectsBody.add(effect);
        } else { // k cộng dồn trên character
            if (effectsBody.stream().filter(effect1 -> effect1.getSkill().getEffectType().id == effect.getSkill().getEffectType().id).count() == 0) {
//                System.out.println("add effect ko cộng dồn lên character ------------------- " + effect.getSkill().getEffectType());
                effectsBody.add(effect);
            }
        }
    }
}
