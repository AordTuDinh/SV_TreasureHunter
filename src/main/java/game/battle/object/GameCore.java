package game.battle.object;

import game.battle.calculate.MathLab;
import game.battle.effect.Effect;
import game.battle.effect.EffectRoom;
import game.battle.model.Character;
import game.battle.model.Enemy;
import game.battle.model.Player;
import game.battle.type.EffectBodyType;
import game.battle.type.GeometryType;
import game.battle.type.RoomState;
import game.battle.type.StateType;
import game.config.CfgBattle;
import game.dragonhero.BattleConfig;
import game.dragonhero.table.BaseBattleRoom;
import game.object.Geometry;
import game.object.PointBuff;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@NoArgsConstructor
public class GameCore {
    public Pos findTargetForPlayer(List<Character> lstEnemy, Player currentPlayer) {
        if (lstEnemy.size() <= 0) return Pos.zero();
        if (currentPlayer.getTargetAttack() != null && currentPlayer.getTargetAttack().isAlive() && currentPlayer.targetInSizeAttack())
            return currentPlayer.getTargetAttack().getPos();

        float min = 10000f; // đặt to vì còn phải check enemy alive
        Pos ret = Pos.zero();
        Character target = null;
        for (int i = 0; i < lstEnemy.size(); i++) {
            if (lstEnemy.get(i).isAlive() && min >= currentPlayer.getPos().distance(lstEnemy.get(i).getPos()) && lstEnemy.get(i).isReady()) {
                min = (float) currentPlayer.getPos().distance(lstEnemy.get(i).getPos());
                target = lstEnemy.get(i);
                ret = lstEnemy.get(i).getPos();
            }
        }
        // tim duoc con gan nhat nhung khoang cach qua tam danh thi tra ve 0
        if (min > currentPlayer.getRangeAttack() || target == null) return Pos.zero();
        currentPlayer.setTargetAttack(target);
        return ret;
    }


    public static Pos checkWall(PanelMap map, Pos move) {
        if (move.x > map.getTopRight().x - BattleConfig.P_Width / 2)
            move.x = map.getTopRight().x - BattleConfig.P_Width / 2;
        if (move.x < map.getBotLeft().x + BattleConfig.P_Width / 2)
            move.x = map.getBotLeft().x + BattleConfig.P_Width / 2;
        if (move.y > map.getTopRight().y - BattleConfig.P_Height) move.y = map.getTopRight().y - BattleConfig.P_Height;
        if (move.y < map.getBotLeft().y) move.y = map.getBotLeft().y;
        return move;
    }

    public static Pos checkWall2(PanelMap map, Pos addPos, Pos curPos) {
        Pos move = new Pos(curPos.x + addPos.x, curPos.y + addPos.y);
        if (move.x > map.getTopRight().x - BattleConfig.P_Width / 2)
            move.x = map.getTopRight().x - BattleConfig.P_Width / 2;
        if (move.x < map.getBotLeft().x + BattleConfig.P_Width / 2)
            move.x = map.getBotLeft().x + BattleConfig.P_Width / 2;
        if (move.y > map.getTopRight().y - BattleConfig.P_Height) move.y = map.getTopRight().y - BattleConfig.P_Height;
        if (move.y < map.getBotLeft().y) move.y = map.getBotLeft().y;
        return move;
    }


    static boolean checkHasMoveTopographic(BaseBattleRoom room, Pos move) {
        List<Geometry> geos = new ArrayList<>();// room.getCacheBattle().getMapInfo().getMapData().getGeos();
        if (geos == null) return true;
        boolean hasMove = true;
        for (int i = 0; i < geos.size(); i++) {
            Geometry geo = geos.get(i);
            if (!geo.isInSize()) { // k cho di chuyển vào hinh
                // flash check
                if (geo.getCenter().distance(move) < geo.getRadius()) {
                    // details check
                    if (geo.getType() == GeometryType.Circle) {
                        hasMove = geo.isInSize() == MathLab.pointInCircle(move, geo.getRadius(), geo.getCenter());
                        if (!hasMove) return false;
                    } else if (geo.getType() == GeometryType.Triangle) {
                        hasMove = geo.isInSize() == MathLab.pointInTriangle(move, geo.getPos());
                        if (!hasMove) return false;
                    }
                }
            } else {
                hasMove = false;
                if (geo.getType() == GeometryType.Circle) {
                    hasMove = MathLab.pointInCircle(move, geo.getRadius(), geo.getCenter());
                    if (hasMove) return true;
                } else if (geo.getType() == GeometryType.Triangle) {
                    hasMove = MathLab.pointInTriangle(move, geo.getPos());
                    if (hasMove) return true;
                }
            }

        }
        return hasMove;
    }

    //public static boolean wallCollisionBullet(BaseBattleRoom room, Bullet bullet) {
    //    if (bullet.getPos().x > room.getMapInfo().getMapData().getTopRight().x - bullet.radius) return true;
    //    if (bullet.getPos().x < room.getMapInfo().getMapData().getBotLeft().x + bullet.radius) return true;
    //    if (bullet.getPos().y > room.getMapInfo().getMapData().getTopRight().y - bullet.radius) return true;
    //    if (bullet.getPos().y < room.getMapInfo().getMapData().getBotLeft().y + -bullet.radius) return true;
    //    //return checkTopographicHasMove(room, curPos);
    //    return false;
    //}

    public void reviveEnemy(List<Character> aEnemy) {
        for (int i = 0; i < aEnemy.size(); i++) {
            aEnemy.get(i).revive();
        }
    }


//    public Player findTargetForEnemy(Enemy enemy, List<Player> aPlayer) {
//        float min = (float) enemy.getPos().distance(aPlayer.get(0).getPos());
//        Player target = null;
//        for (int i = 0; i < aPlayer.size(); i++) {
//            double distance = enemy.getPos().distance(aPlayer.get(i).getPos());
//            if (aPlayer.get(i).isAlive() && min > distance) {
//                min = (float) distance;
//                target = aPlayer.get(i);
//            }
//        }
//        //System.out.println("min = " + min);
//        if (min > enemy.getRangeView()) return null;
//        return target;
//    }


    public void process_bullet(BaseBattleRoom room) {
        for (int i = 0; i < room.getABullets().size(); i++) {
            Bullet b = room.getABullets().get(i);
            if (b.isAlive()) {
                b.move();
            } else {
                room.removeBullet(b);
            }
        }
    }

    public void checkHit(BaseBattleRoom room) {
        if (room.getRoomState() != RoomState.ACTIVE) return;
        checkHitLongRange(room, room.getABullets(), room.getAPlayer(), room.getAEnemy());
        // check hit melee
        for (int i = 0; i < room.getAPlayer().size(); i++) {
            Character player = room.getAPlayer().get(i);
            //System.out.println("player.getPoint().getCurHP() = " + player.getPoint().getCurHP());
            for (int j = 0; j < room.getAEnemy().size(); j++) {
                Character enemy = room.getAEnemy().get(j);
                if (enemy.isHitMelee(player) && player.hasReceiveEffMelee(enemy)) {
//                    chỉ thằng player bị ăn đòn =))
                    player.beAttackCollider(enemy);
                }
//                checkToxicBoy(room, enemy);
            }
//            checkToxicBoy(room, player);
        }
    }

    private void checkHitLongRange(BaseBattleRoom room, List<Bullet> bullets, List<Character> aPlayer, List<Character> aEnemy) {
        // check hit long range
        for (int i = 0; i < bullets.size(); i++) {
            Bullet b = bullets.get(i);
            for (int j = 0; j < aPlayer.size(); j++) {
                Character player = aPlayer.get(j);
                if (player.inSizeHitBullet(b) && b.getPenetration() > 0) {
                    player.beAttackBullet(b);
                    if (b.initDone() && b.getPenetration() <= 0) {
                        b.setHit();
                        b.isAlive = false;
                        room.removeBullet(b);
                    }
                }
            }
            for (int j = 0; j < aEnemy.size(); j++) {
                Character enemy = aEnemy.get(j);
                if (enemy.inSizeHitBullet(b) && b.getPenetration() > 0) {
                    enemy.beAttackBullet(b);
                    if (b.initDone() && b.getPenetration() <= 0) {
                        b.isAlive = false;
                        room.removeBullet(b);
                    }
                }
            }
        }
    }

//    public void checkHit(BaseBattleRoom room, List<Character> checkPlayer) {
//        if (room.getRoomState() != RoomState.ACTIVE) return;
//        checkHitLongRange(room, room.getABullets(), checkPlayer, room.getAEnemy());
//        // check hit melee
//        for (int i = 0; i < checkPlayer.size(); i++) {
//            Character character = checkPlayer.get(i);
//            for (int j = 0; j < room.getAEnemy().size(); j++) {
//                Character enemy = room.getAEnemy().get(j);
//                if (enemy.isHitMelee(character) && character.hasReceiveEffMelee(enemy)) {
//                    character.beAttackCollider(enemy);
//                }
////                checkToxicBoy(room, enemy);
//            }
////            checkToxicBoy(room, character);
//        }
//    }

//    void checkToxicBoy(BaseBattleRoom room, Character beCheck) {
//        if (room.getAToxicBoy().isEmpty()) return;
//        for (int i = 0; i < room.getAToxicBoy().size(); i++) {
//            Character toxicBoy = room.getAToxicBoy().get(i);
//            Effect toxic = toxicBoy.getToxic();
//            if (toxic != null) {
//                if (toxicBoy.sameTeam(beCheck) && toxicBoy.isHitMelee(beCheck) && beCheck.hasReceiveEffMelee(toxicBoy)) {
//                    beCheck.f0Toxic.add(toxicBoy.getId());
//                    beCheck.addEffectTime(toxic);
//                    beCheck.protoStatus(StateType.EFFECT_BODY, Arrays.asList(EffectBodyType.TOXIC.value, (long) (toxic.getTimeExits() * 1000)));
//                }
//            } else {
//                room.removeCharacterMelee(room.getAToxicBoy().get(i));
//            }
//        }
//        for (int i = 0; i < room.getAToxicBoy().size(); i++) {
//            Character toxicBoy = room.getAToxicBoy().get(i);
//            Effect toxic = room.getAToxicBoy().get(i).getToxic();
//            if (toxic == null) {
//                room.removeCharacterMelee(room.getAToxicBoy().get(i));
//            } else {
//                if (toxicBoy.sameTeam(beCheck) && room.getAToxicBoy().get(i).isHitMelee(beCheck) && beCheck.hasReceiveEffMelee(toxicBoy)) {
//                    beCheck.f0Toxic.add(toxicBoy.getId());
//                    beCheck.addEffectTime(toxic);
//                }
//            }
//        }
//    }


    //Fixme DATE: 7/31/2022 LƯU Ý ---> Gọi trong update: process eff room - effect dạng tác dụng 1 lần
    public void Update(BaseBattleRoom room) {
        if (room.getRoomState() != RoomState.ACTIVE) return;
        for (int i = 0; i < room.getAEffectRoom().size(); i++) {
            EffectRoom eff = room.getAEffectRoom().get(i);
            processEffInRoom(room, eff, room.getAPlayer(), room.getAEnemy());
        }
    }

    //Fixme DATE: 7/31/2022 LƯU Ý ---> Lưu ý đã update trong effect in room thì không có trong room by time nữa
    public void EffectUpdate(BaseBattleRoom room) { // 0.5s gọi 1 lần
        if (room.getRoomState() != RoomState.ACTIVE) return;
        for (int i = 0; i < room.getAEffectRoom().size(); i++) {
            EffectRoom eff = room.getAEffectRoom().get(i);
            if (eff != null) processRoomByTime(room, eff);
        }
    }

    //Fixme DATE: 7/31/2022 LƯU Ý ---> 0.5s gọi 1 lần nên sát thương sẽ chia 2 còn point thì giữ nguyên
    public void processRoomByTime(BaseBattleRoom room, EffectRoom eff) { // 0.5s gọi 1 lần
        if (room.getRoomState() != RoomState.ACTIVE) return;
        boolean hasRemove = false; // lười thêm vào list nên làm cách này cho tiện - có thể check theo effect type nhưng mà khi thêm eff lại phải thêm vào list => lười
        switch (eff.getSkill().getEffectType()) {
            case SANDSTORM: // process in room
                if (!eff.checkActiveTime()) {
                    for (int i = 0; i < room.getAEnemy().size(); i++) {
                        if (canHitEffectRoom(eff, room.getAEnemy().get(i))) {
                            room.getAEnemy().get(i).addEffectTime(eff.clone());
                        }
                    }
                }
                long magDame = (long) (eff.getSkill().getFirstPer() * eff.getOwner().getPoint().getMagicDamage());
                for (int i = 0; i < room.getAEnemy().size(); i++) {
                    if (canHitEffectRoom(eff, room.getAEnemy().get(i))) {
                        room.getAEnemy().get(i).beAttackEffect(eff, 0L, magDame / 2);
                    }
                }
                hasRemove = true;
                break;
            case BLIZZARD: // process in room
                if (!eff.checkActiveTime()) {
                    for (int i = 0; i < room.getAEnemy().size(); i++) {
                        if (canHitEffectRoom(eff, room.getAEnemy().get(i))) {
                            Effect effDec = eff.clone(BattleConfig.S_timeDecBlizzard);
                            room.getAEnemy().get(i).addEffectTime(effDec);
                        }
                    }
                }
                if (eff.canActiveByAnim()) {
                    long atkBliz = (long) (eff.getSkill().getFirstPer() * eff.getOwner().getPoint().getMagicDamage());
                    for (int i = 0; i < room.getAEnemy().size(); i++) {
                        if (canHitEffectRoom(eff, room.getAEnemy().get(i))) {
                            room.getAEnemy().get(i).beAttackEffect(eff, 0l, atkBliz / 2);
                        }
                    }
                }
                hasRemove = true;
                break;
            case INF: // process in room
                eff.checkActiveTime();
                if (eff.canActiveByAnim()) { // delay by animation
                    for (int i = 0; i < room.getAPlayer().size(); i++) {
                        if (canHitEffectRoom(eff, room.getAPlayer().get(i))) {
                            Effect effBody = eff.clone(BattleConfig.S_timePoinson);
                            room.getAPlayer().get(i).addEffectTime(effBody); // phải add eff khác để còn trừ time khác nhau
                        }
                    }

                    for (int i = 0; i < room.getAEnemy().size(); i++) {
                        if (canHitEffectRoom(eff, room.getAEnemy().get(i))) {
                            Effect effBody = eff.clone(BattleConfig.S_timePoinson);
                            room.getAEnemy().get(i).addEffectTime(effBody); // phải add eff khác để còn trừ time khác nhau
                        }
                    }
                }
                hasRemove = true;
                break;
//            case OGAMA_SKILL_1: // process in room
//                if (!eff.isActive()) {
//                    eff.getOwner().protoStatus(StateType.EFFECT, eff.toStateType());
//                    eff.active();
//                    eff.setTimeRealActive();
//                }
//                if (eff.canActiveByAnim()) { // delay by animation
//                    for (Character player : room.getAPlayer()) {
//                        if (canHitEffectRoom(eff, player)) {
//                            Effect effBody = eff.clone(BattleConfig.S_timePoisonOgama);
//                            player.addEffectTime(effBody); // phải add eff khác để còn trừ time khác nhau
//                        }
//                    }
//                }
//                hasRemove = true;
//                break;

            case SMOKE: // process in room
                eff.checkActiveTime();
                if (eff.canActiveByAnim()) {
                    for (int i = 0; i < room.getAPlayer().size(); i++) {
                        if (canHitEffectRoom(eff, room.getAPlayer().get(i))) {
                            Effect effBody = eff.clone();
                            room.getAPlayer().get(i).addEffectTime(effBody);
                        }
                    }

                }
                hasRemove = true;
                break;
        }
        //Fixme DATE: 7/31/2022 LƯU Ý ---> kiểm tra tồn tại, cần check có đang chờ dame không, nếu đang chò dame thì k đc xóa
        boolean checkExits = eff.checkExist(CfgBattle.decTimeEffRoom);
        if (eff.canActiveByAnim() && !checkExits && hasRemove) {
            room.removeEffect(eff);
        }
    }

    private boolean canHitEffectRoom(EffectRoom eff, Character target) {
        return target.canReceiveEffect(eff.getSkill().getEffectType()) && !eff.sameTeam(target) && target.isAlive() && target.inSizeHit(eff.getInstancePos(), eff.getSkill().getEffectType().radius);
    }

    private boolean canHitEffectRoomSameTeam(EffectRoom eff, Character target) {
        return target.canReceiveEffect(eff.getSkill().getEffectType()) && target.isAlive() && target.inSizeHit(eff.getInstancePos(), eff.getSkill().getEffectType().radius);
    }

    private boolean canHitEffectRoomElip(EffectRoom eff, Character target) {
        return target.canReceiveEffect(eff.getSkill().getEffectType()) && !eff.sameTeam(target) && target.isAlive() && MathLab.inSizeElip(target.getPos(), eff.getInstancePos(), eff.getSizeElip().get(0), eff.getSizeElip().get(1));
    }

    public void processEffInRoom(BaseBattleRoom room, EffectRoom eff, List<Character> aPlayer, List<Character> aEnemy) { // effect thực thi ngay
        if (room.getRoomState() != RoomState.ACTIVE) return;
        //Fixme DATE: 7/31/2022 LƯU Ý --->  process eff room - effect dạng tác dụng 1 lần
        float radius = eff.getSkill().getEffectType().radius;
        switch (eff.getSkill().getEffectType()) {
            case DAME_MAGIC2:
            case DAME_MAGIC3:
//                System.out.println("DAME_MAGIC2--------------------22222");
                eff.checkActiveOne();
                room.removeEffect(eff);
                break;
            case EXPLODE: //Process in room
                eff.checkActiveOne();
                if (eff.canActiveByAnim()) {
                    for (int i = 0; i < aPlayer.size(); i++) {
                        if (!aPlayer.get(i).sameTeam(eff.getOwner()) && aPlayer.get(i).inSizeHit(eff.getInstancePos(), radius)) {
                            long magDame = (long) (eff.getSkill().getFirstPer() * eff.getOwner().getPoint().getMagicDamage());
                            aPlayer.get(i).beAttackEffect(eff, 0, magDame <= 0 ? 1 : magDame);
                        }
                    }
                    for (int i = 0; i < aEnemy.size(); i++) {
                        if (!aEnemy.get(i).sameTeam(eff.getOwner()) && aEnemy.get(i).inSizeHit(eff.getInstancePos(), radius)) {
                            long magDame = (long) (eff.getSkill().getFirstPer() * eff.getOwner().getPoint().getMagicDamage());
                            aEnemy.get(i).beAttackEffect(eff, 0, magDame <= 0 ? 1 : magDame);
                        }
                    }
                    room.removeEffect(eff);
                }
                break;
            case HOA_THAN_NORMAL: // process in room
                eff.checkActiveTime();

                if (eff.canActiveByAnim()) { // delay by animation
                    for (int i = 0; i < aPlayer.size(); i++) {
                        if (canHitEffectRoom(eff, aPlayer.get(i))) {
                            Effect effBody = eff.clone(BattleConfig.S_timeDot);
                            aPlayer.get(i).addEffectTime(effBody); // phải add eff khác để còn trừ time khác nhau
                        }
                    }
                    room.removeEffect(eff);
                }
                break;
            case BOMB: //Process in room
                eff.checkActiveOne();
                if (eff.canActiveByAnimOne()) {
                    long atkBomb = (long) (eff.getSkill().getFirstPer() * eff.getOwner().getPoint().getAttackDamage());
                    long magBomb = (long) (eff.getSkill().getNextPer() * eff.getOwner().getPoint().getMagicDamage());
                    PointBuff pb = new PointBuff(Point.MAGIC_RESIST, (long) -eff.getSkill().getValueIndex(2));
                    for (int i = 0; i <aEnemy.size() ; i++) {
                        Character enemy =aEnemy.get(i);
                        if (!enemy.sameTeam(eff.getOwner()) && enemy.inSizeHit(eff.getInstancePos(), radius)) {
                            enemy.beAttackEffect(eff, atkBomb, magBomb, pb);
                            Effect newEff = eff.clone();
                            enemy.addEffectTime(newEff);
                        }
                    }

                    room.removeEffect(eff);
                }
                break;
//            case SPINNING: //Process in room
//            {
//                if (!eff.isActive()) {
//                    eff.active();
//                    eff.getOwner().protoStatus(StateType.EFFECT, eff.toStateOne());
//                    eff.setTimeRealActive();
//                }
//                if (eff.canActiveByAnim()) {
//                    long atkSpin = (long) (eff.getSkill().getFirstPer() * eff.getOwner().getPoint().getAttackDamage());
//                    long magSpin = (long) (eff.getSkill().getNextPer() * eff.getOwner().getPoint().getMagicDamage());
//                    for (Character enemy : room.getAEnemy()) {
//                        if (!enemy.sameTeam(eff.getOwner()) && (enemy.inSizeHit(eff.getSpin1(), radius) || enemy.inSizeHit(eff.getSpin2(), radius))) {
//                            enemy.beAttackEffect(eff, atkSpin, magSpin);
//                            System.out.println("Be attack by effect SPINNING ======== " + (atkSpin + magSpin));
//                        }
//                    }
//                }
//                boolean checkExits = eff.checkExist(CfgBattle.decTimeEff);
//                if (eff.canActiveByAnim() && !checkExits) {
//                    room.removeEffect(eff);
//                }
//                break;
//            }
            case UP_LEVEL: //Process in room
            {
                eff.checkActiveOne();
                if (eff.canActiveByAnim()) {
                    long atkDame = (long) (eff.getSkill().getFirstPer() * eff.getOwner().getPoint().getAttackDamage());
                    long magDame = (long) (eff.getSkill().getNextPer() * eff.getOwner().getPoint().getMagicDamage());
                    for (int i = 0; i < aEnemy.size(); i++) {
                        if (!aEnemy.get(i).sameTeam(eff.getOwner()) && aEnemy.get(i).inSizeHit(eff.getInstancePos(), radius)) {
                            aEnemy.get(i).beAttackEffect(eff, atkDame, magDame);
                        }
                    }
                }
                boolean checkExits = eff.checkExist(CfgBattle.decTimeEff);
                if (eff.canActiveByAnim() && !checkExits) {
                    room.removeEffect(eff);
                }
                break;
            }
            case HOA_THAN_2: //Process in room
            {
                eff.checkActiveOne();
                if (eff.canActiveByAnim()) {
                    long atkDame = (long) (eff.getSkill().getFirstPer() * eff.getOwner().getPoint().getAttackDamage());
                    long magDame = (long) (eff.getSkill().getNextPer() * eff.getOwner().getPoint().getMagicDamage());
                    for (int i = 0; i < aPlayer.size(); i++) {
                        Character player = aPlayer.get(i);
                        if (!player.sameTeam(eff.getOwner()) && player.inSizeHit(eff.getInstancePos(), radius)) {
                            Effect newEff = eff.clone(2f);
                            player.beAttackEffect(eff, atkDame, magDame);
                            player.addEffectTime(newEff);
                        }
                    }
                }
                boolean checkExits = eff.checkExist(CfgBattle.decTimeEff);
                if (eff.canActiveByAnim() && !checkExits) {
                    room.removeEffect(eff);
                }
                break;
            }
            case KIM_THAN_SKILL_1: //Process in room
            {
                eff.checkActiveOne();
                if (eff.canActiveByAnim()) {
                    for (int i = 0; i < aPlayer.size(); i++) {
                        if (!aPlayer.get(i).sameTeam(eff.getOwner()) && aPlayer.get(i).inSizeHit(eff.getInstancePos(), radius)) {
                            Effect newEff = eff.clone(2f);
                            aPlayer.get(i).addEffectNow(newEff);
                        }
                    }
                }
                boolean checkExits = eff.checkExist(CfgBattle.decTimeEff);
                if (eff.canActiveByAnim() && !checkExits) {
                    room.removeEffect(eff);
                }
                break;
            }
            case THO_THAN_2: //Process in room
            {
                eff.checkActiveOne();
                if (eff.canActiveByAnim()) {
                    for (int i = 0; i < aPlayer.size(); i++) {
                        Character player = aPlayer.get(i);
                        if (!player.sameTeam(eff.getOwner()) && canHitEffectRoomElip(eff, player)) {
                            Effect newEff = eff.clone(2f);
                            player.addEffectTime(newEff);
                        }
                    }
                }
                boolean checkExits = eff.checkExist(CfgBattle.decTimeEff);
                if (eff.canActiveByAnim() && !checkExits) {
                    room.removeEffect(eff);
                }
                break;
            }
            case THUY_THAN_1: //Process in room
            {
                eff.checkActiveOne();
                if (eff.canActiveByAnim()) {
                    for (int i = 0; i < aPlayer.size(); i++) {
                        Character player = aPlayer.get(i);
                        if (!player.sameTeam(eff.getOwner()) && MathLab.inSizeElip(player.getPos(), eff.getInstancePos(), eff.getSizeElip().get(0), eff.getSizeElip().get(1))) {
                            Effect newEff = eff.clone(1f);
                            player.addEffectTime(newEff);
                        }
                    }
                }
                boolean checkExits = eff.checkExist(CfgBattle.decTimeEff);
                if (eff.canActiveByAnim() && !checkExits) {
                    room.removeEffect(eff);
                }
                break;
            }
//            case KAGU_SKILL_3: //Process in room
//            {
//                if (!eff.isActive()) {
//                    eff.active();
//                    eff.getOwner().protoStatus(StateType.EFFECT, eff.toStateOne());
//                    eff.setTimeRealActive();
//                }
//                if (eff.canActiveByAnim()) {
//                    long atkDame = (long) (eff.getSkill().getFirstPer() * eff.getOwner().getPoint().getAttackDamage());
//                    long magDame = (long) (eff.getSkill().getNextPer() * eff.getOwner().getPoint().getMagicDamage());
//                    for (Character player : room.getAPlayer()) {
//                        if (!player.sameTeam(eff.getOwner()) && player.inSizeHit(eff.getInstancePos(), radius)) {
//                            Effect newEff = eff.clone(2f);
//                            player.addEffectTime(newEff);
//                            player.beAttackEffect(eff, atkDame, magDame);
//                        }
//                    }
//                }
//                boolean checkExits = eff.checkExist(CfgBattle.decTimeEff);
//                if (eff.canActiveByAnim() && !checkExits) {
//                    room.removeEffect(eff);
//                }
//                break;
//            }
            case KIM_THAN_SKILL_2: //Process in room
            {
                eff.checkActive(EffectRoom.EFFECT_ONE);
                if (eff.canActiveByAnim()) {
                    long atkDame = (long) (eff.getSkill().getFirstPer() * eff.getOwner().getPoint().getAttackDamage());
                    long magDame = (long) (eff.getSkill().getNextPer() * eff.getOwner().getPoint().getMagicDamage());
                    for (int i = 0; i < aPlayer.size(); i++) {
                        Character player = aPlayer.get(i);
                        if (!player.sameTeam(eff.getOwner()) && player.inSizeHit(eff.getInstancePos(), radius)) {
                            player.beAttackEffect(eff, atkDame, magDame);
                        }
                    }
                }
                boolean checkExits = eff.checkExist(CfgBattle.decTimeEff);
                if (eff.canActiveByAnim() && !checkExits) {
                    room.removeEffect(eff);
                }
                break;
            }
            case KIM_THAN_SKILL_3: //Process in room
                if (!eff.isActive()) {
                    eff.active();
                    Pos rand = Pos.randomPos(eff.getTarget().getPos().x - 1f, eff.getTarget().getPos().y - 1f, eff.getTarget().getPos().x + 1f, eff.getTarget().getPos().y + 1f);
                    eff.setInstancePos(rand);
                    eff.getOwner().protoStatus(StateType.EFFECT, eff.toStateOne());
                    eff.setTimeRealActive();
                    //random diểm đánh xuống
                }
                if (eff.canActiveByAnim()) {
                    long atkDame = (long) (eff.getSkill().getFirstPer() * eff.getOwner().getPoint().getAttackDamage());
                    long magDame = (long) (eff.getSkill().getNextPer() * eff.getOwner().getPoint().getMagicDamage());
                    if (!eff.getTarget().sameTeam(eff.getOwner()) && eff.getTarget().inSizeHit(eff.getInstancePos(), radius)) {
                        eff.getTarget().beAttackEffect(eff, atkDame, magDame);
                        Effect newEff = eff.clone();
                        eff.getTarget().addEffectTime(newEff);
                    }
                    room.removeEffect(eff);
                }
                break;
        }

    }

    public synchronized void FixedUpdate(BaseBattleRoom room) {
        if (room.getAPlayer().size() > 0) {
            checkHit(room);
            process_bullet(room);
        }
    }

    public void LastUpdate(BaseBattleRoom room) {
        if (room.getRoomState() != RoomState.ACTIVE || !room.isAllowReviveEnemy()) return;
        reviveEnemy(room.getAEnemy());
    }
    //endregion
}
