package game.battle.model;

import game.battle.effect.SkillEffect;
import game.battle.object.Coroutine;
import game.battle.object.PanelMap;
import game.battle.object.Point;
import game.battle.object.Pos;
import game.battle.type.CharacterType;
import game.battle.type.StateType;
import game.config.aEnum.FactionType;
import game.dragonhero.BattleConfig;
import game.dragonhero.mapping.UserPetEntity;
import game.dragonhero.mapping.main.ResPetEntity;
import game.dragonhero.server.Constans;
import ozudo.base.helper.DateTime;
import protocol.Pbmethod;

import java.io.Serializable;
import java.util.List;

public class Pet extends Character implements Serializable {
    Player owner;
    SkillEffect petSkills;
    float timePetActive;

    public Pet(UserPetEntity uPet, Player owner) {
        this.type = CharacterType.PET;
        this.model = uPet.getPetId();
        this.direction = Pos.RandomDirection();
        this.teamId = 0;
        this.pos = owner.getPos().clone();
        this.point = new Point();
        this.point.setMoveSpeed(100);
        this.owner = owner;
        ResPetEntity res = uPet.getResPet();
        this.faction = FactionType.get(res.getFaction());
        this.petSkills = new SkillEffect(res.getPetSkill(), uPet.getStar());
        this.timePetActive = res.getTimeActive();
    }

    @Override
    public Pbmethod.PbUnitAdd.Builder toProtoAdd() {
        Pbmethod.PbUnitAdd.Builder pb = Pbmethod.PbUnitAdd.newBuilder();
        pb.setType(Constans.TYPE_PET);
        pb.setId(id);
        pb.setIsAdd(true);
        pb.setPos(pos.toProto());
        pb.setDirection(direction.toProto());
        pb.setBotLeft(owner.panelMap.botLeft.toProto());
        pb.setTopRight(owner.panelMap.topRight.toProto());
        pb.setTeamId(teamId);
        pb.addAvatar(model);
        pb.setOwnerId(owner.id);
        pb.setSpeed((int) point.getMoveSpeed());
        pb.setFaction(FactionType.NULL.value);
        return pb;
    }


    public boolean isMove() {
        this.isMove = !DateTime.isAfterTime(timeActionMove, BattleConfig.P_timeNoMove);
        return isMove;
    }

    public void setPosAndDirection(Pos newPos, Pos newDirection) {
        this.pos = newPos.round();
        this.direction = newDirection.normalized();
        this.setMove(true);
    }

    public void processSkill() {
        SkillEffect eff = petSkills;
        if (eff != null && DateTime.isAfterTime(getTimeActionAttack(), eff.getTime())) {
            activeSkill(0);
            owner.getRoom().addCoroutine(new Coroutine(eff.getTimeDelayDame(), () -> {
                switch (eff.getEffectType()) {
                    case PET_RE_HP -> { // create
                        long reHp1 = (long) (eff.getFirstPer() * owner.getPoint().getMaxHp());
                        long reMaxHp = (long) (BattleConfig.S_maxReHp75 / 100f * owner.getPoint().getMaxHp());
                        reHp1 = reHp1 > reMaxHp ? reMaxHp : reHp1;
                        owner.reHp(reHp1);
//                        owner.protoStatus(StateType.RE_HP, reHp1);
                    }
                    case PET_BUFF_SHELL -> {//
                        long shell = (long) (eff.getFirstPer() * owner.getPoint().getMaxHp());
                        long maxShell = (long) (BattleConfig.S_maxReHp75 / 100f * owner.getPoint().getMaxHp());
                        shell = shell > maxShell ? maxShell : shell;
                        owner.buffShell(shell);
                        owner.getRoom().addCoroutine(new Coroutine(timePetActive, () -> {
                            long curShell = owner.getPoint().getCurShell();
                            if (curShell > 0) owner.protoBuffPoint(Point.SHELL, -curShell);
                        }));
                    }
                    case PET_BUFF_ATK -> buffPointPet(owner, Point.CHANGE_ATTACK, eff, BattleConfig.S_maxReduce90);
                    case PET_BUFF_MATK ->
                            buffPointPet(owner, Point.CHANGE_MAGIC_ATTACK, eff, BattleConfig.S_maxReduce90);
                    case PET_BUFF_DEF -> buffPointPet(owner, Point.CHANGE_DEFENSE, eff, BattleConfig.S_maxReduce90);
                    case PET_BUFF_MAGIC_RESIST ->
                            buffPointPet(owner, Point.CHANGE_MAGIC_RESIST, eff, BattleConfig.S_maxReduce90);
                    case PET_BUFF_CRIT -> buffPointPet(owner, Point.CHANGE_CRIT, eff, BattleConfig.S_maxReduce90);
                    case PET_BUFF_CRIT_DAME ->
                            buffPointPet(owner, Point.CHANGE_CRIT_DAMAGE, eff, BattleConfig.S_maxReduce90);
                    case PET_BUFF_DEC_DAME -> buffPointPet(owner, Point.CHANGE_DAME, eff, BattleConfig.S_maxReduce90);
                    case PET_BUFF_TRUE_DAME -> {
                        owner.getPoint().setTrueDame(true);
                        owner.getRoom().addCoroutine(new Coroutine(timePetActive, () -> owner.getPoint().setTrueDame(false)));
                    }
                    case PET_BUFF_DODGE -> {
                        owner.protoBuffPoint(Point.DOGE, (long) eff.getFirstValues());
                        owner.getRoom().addCoroutine(new Coroutine(timePetActive, () -> {
                            owner.protoBuffPoint(Point.DOGE, -(long) eff.getFirstValues());
                        }));
                    }
                    case PET_BUFF_SPD ->
                            buffPointPet(owner, Point.CHANGE_ATTACK_SPEED, eff, BattleConfig.S_maxReduce90);
                    case PET_BUFF_DAMAGE -> {//
                        long realBuff1 = owner.getPoint().buffChange(Point.CHANGE_ATTACK, (long) eff.getFirstValues(), BattleConfig.S_maxReduce90);
                        long realBuff2 = owner.getPoint().buffChange(Point.CHANGE_MAGIC_ATTACK, (long) eff.getValueIndex(1), BattleConfig.S_maxReduce90);

                        owner.getRoom().addCoroutine(new Coroutine(timePetActive, () -> {
                            owner.getPoint().buffChange(Point.CHANGE_ATTACK, -realBuff1, BattleConfig.S_maxReduce90);
                            owner.getPoint().buffChange(Point.CHANGE_MAGIC_ATTACK, -realBuff2, BattleConfig.S_maxReduce90);
                        }));
                    }
                    case PET_DEF_REST -> {//
                        long realBuff1 = owner.getPoint().buffChange(Point.CHANGE_DEFENSE, (long) eff.getFirstValues(), BattleConfig.S_maxReduce90);
                        long realBuff2 = owner.getPoint().buffChange(Point.CHANGE_MAGIC_RESIST, (long) eff.getValueIndex(1), BattleConfig.S_maxReduce90);
                        owner.getRoom().addCoroutine(new Coroutine(timePetActive, () -> {
                            owner.getPoint().buffChange(Point.CHANGE_DEFENSE, -realBuff1, BattleConfig.S_maxReduce90);
                            owner.getPoint().buffChange(Point.CHANGE_MAGIC_RESIST, -realBuff2, BattleConfig.S_maxReduce90);
                        }));
                    }
                }
            }));
        }
    }

    private void buffPointPet(Character heroBuff, int pointId, SkillEffect eff, int reMax) {
        long realBuff = heroBuff.getPoint().buffChange(pointId, (long) eff.getFirstValues(), reMax);
        owner.getRoom().addCoroutine(new Coroutine(timePetActive, () -> {
            heroBuff.getPoint().buffChange(pointId, -realBuff, reMax);
        }));
    }

    public Pbmethod.PbUnitAdd.Builder toProtoRemove() {
        Pbmethod.PbUnitAdd.Builder builder = Pbmethod.PbUnitAdd.newBuilder();
        builder.setType(Constans.TYPE_PET);
        builder.setId(id);
        builder.setIsAdd(false);
        return builder;
    }

    @Override
    public void activeSkill(int skillId) {
        setTimeAttack();
        protoStatus(StateType.PET_USE_SKILL, List.of((long) teamId));
    }
}
