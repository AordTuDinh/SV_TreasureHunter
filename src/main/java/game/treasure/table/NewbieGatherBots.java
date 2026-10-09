package game.treasure.table;

import game.battle.calculate.IMath;
import game.battle.model.CellObject;
import game.battle.model.ChunkObject;
import game.battle.model.FakeGatherPlayer;
import game.battle.model.Player;
import game.battle.model.Unit;
import game.battle.object.Point;
import game.battle.object.Pos;
import game.battle.type.RoomState;
import game.config.aEnum.MapType;
import game.treasure.mapping.UserEntity;
import game.treasure.mapping.UserEquipmentEntity;
import game.treasure.service.item.EquipmentStatRollService;
import game.treasure.service.resource.PlayerBasePoint;
import game.treasure.service.resource.ResItem;
import ozudo.base.helper.NumberUtil;
import protocol.Pbmethod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Làng tân thủ: nếu người thật ít hơn {@link #REAL_THRESHOLD} thì thêm {@link #BOT_COUNT} người giả
 * đang đứng khai thác. Không phá ô, không rơi đồ.
 */
final class NewbieGatherBots {
    static final int REAL_THRESHOLD = 20;
    static final int BOT_COUNT = 20;
    /** Ô gần làng để người mới vào là thấy. */
    static final int NEAR_CELL_POOL = 80;
    static final float STAND_OFFSET = 0.42f;
    /** Tầm chủ động tìm quái khi đang đào. */
    static final float HUNT_RANGE = 4.5f;
    /** Quái vừa đánh mình thì đuổi xa hơn. */
    static final float CHASE_RANGE = 8f;

    static final String[] NAMES = {
            "MinhKhoai", "LanChi", "TuanMap", "HaDao", "KhoaDat",
            "MyLinh", "DucAn", "NgocHa", "PhucLoc", "BaoNam",
            "ThaoVy", "QuangHuy", "LinhDan", "HoangAnh", "TrangMoon",
            "KietVang", "YenNhi", "SonTung", "MaiPhuong", "DatViet",
            "CuongRock", "HanhPhuc", "TienDat", "ThuHa", "LongDia",
            "KimNgan", "VietAnh", "DaoXanh", "NamKho", "BichNgoc"
    };

    private NewbieGatherBots() {
    }

    static void sync(BaseRoom room) {
        if (!isNewbie(room)) return;
        int real = countReal(room);
        if (real <= 0 || real >= REAL_THRESHOLD) {
            removeBots(room);
            return;
        }
        int have = countBots(room);
        int need = BOT_COUNT - have;
        if (need <= 0) return;
        List<CellObject> pool = nearMineCells(room);
        if (pool.isEmpty()) return;
        Set<Integer> used = usedCellIds(room);
        Set<String> usedNames = usedNames(room);
        int userSeq = nextFakeUserId(room);
        for (int i = 0; i < need; i++) {
            CellObject cell = pickCell(pool, used);
            if (cell == null) break;
            used.add(cell.getId());
            Pos stand = standPos(cell, userSeq);
            if (!room.isValidChunkId(room.worldPosToChunkId(stand))) {
                stand = cell.getPos().clone();
            }
            if (!room.isValidChunkId(room.worldPosToChunkId(stand))) continue;
            List<Integer> equip = rollEquip();
            FakeGatherPlayer bot = new FakeGatherPlayer(nextName(usedNames), userSeq, bodyKey(equip), equip, buildPoint(equip));
            bot.setPos(stand.clone());
            bot.setRoom(room);
            bot.setPanelMap(room.getMapInfo());
            bot.assignCell(cell, stand, System.currentTimeMillis() + mineDuration());
            room.addUnit(bot);
            userSeq--;
        }
    }

    static void tick(BaseRoom room, float dt) {
        if (!isNewbie(room) || dt <= 0f) return;
        long now = System.currentTimeMillis();
        List<Long> ids = new ArrayList<>(room.aPlayerIds);
        for (int i = 0; i < ids.size(); i++) {
            Unit unit = room.mUnit.get(ids.get(i));
            if (!(unit instanceof FakeGatherPlayer bot)) continue;
            if (!bot.isAlive()) {
                if (bot.getReviveAtMs() > 0 && now >= bot.getReviveAtMs()) reviveBot(room, bot);
                continue;
            }
            step(room, bot, dt, now);
        }
    }

    static void step(BaseRoom room, FakeGatherPlayer bot, float dt, long now) {
        Unit foe = combatTarget(room, bot);
        if (foe != null) {
            float stop = Math.max(0.35f, bot.getRangeAttack() * 0.85f);
            if (moveToward(room, bot, foe.getPos(), dt, stop)) room.hitUnit(bot, foe);
            return;
        }
        CellObject cell = bot.getGatherCell();
        if (cell == null || !cell.canAttack()) {
            retarget(room, bot, now);
            cell = bot.getGatherCell();
            if (cell == null) return;
        }
        Pos stand = bot.getStandPos() != null ? bot.getStandPos() : cell.getPos();
        if (!moveToward(room, bot, stand, dt, 0.22f)) return;
        if (now >= bot.getNextRetargetMs()) {
            retarget(room, bot, now);
            return;
        }
        room.hitCell(bot, cell);
    }

    static boolean moveToward(BaseRoom room, FakeGatherPlayer bot, Pos target, float dt, float stopDist) {
        if (target == null || bot.getPos() == null) return false;
        double dist = bot.getPos().distance(target);
        if (dist <= stopDist) return true;
        float step = (float) Math.min(dist - stopDist, bot.getCurSpeed() * dt);
        if (step < 0.001f) return true;
        Pos dir = bot.getPos().getDirectionTo(target);
        if (dir == null || (dir.x == 0f && dir.y == 0f)) dir = Pos.right();
        Pos next = new Pos(bot.getPos().x + dir.x * step, bot.getPos().y + dir.y * step).round();
        if (!room.isValidChunkId(room.worldPosToChunkId(next))) return false;
        bot.setPosAndDirection(next, dir);
        return false;
    }

    static Unit combatTarget(BaseRoom room, FakeGatherPlayer bot) {
        Unit threat = room.mUnit.get(bot.getThreatId());
        if (threat != null && threat.isEnemy() && threat.isAlive()
                && bot.getPos().distance(threat.getPos()) <= CHASE_RANGE) {
            return threat;
        }
        bot.clearThreat();
        Unit best = null;
        double bestDist = HUNT_RANGE;
        for (Unit unit : room.mUnit.values()) {
            if (unit == null || !unit.isEnemy() || !unit.isAlive() || unit.getPos() == null) continue;
            double dist = bot.getPos().distance(unit.getPos());
            if (dist < bestDist) {
                bestDist = dist;
                best = unit;
            }
        }
        return best;
    }

    static void reviveBot(BaseRoom room, FakeGatherPlayer bot) {
        Pos center = room.getMapInfo() != null ? room.getMapInfo().getHeathCenter() : null;
        if (center == null) center = Pos.zero();
        int salt = bot.getFakeUserId();
        Pos spawn = new Pos(center.x + ((Math.abs(salt) % 5) - 2) * 0.55f, center.y + ((Math.abs(salt) % 3) - 1) * 0.4f).round();
        if (!room.isValidChunkId(room.worldPosToChunkId(spawn))) spawn = center.clone();
        bot.reviveAtVillage(spawn);
    }

    static Point buildPoint(List<Integer> equip) {
        Point point = PlayerBasePoint.getBase();
        if (equip != null) {
            for (int i = 0; i + 2 < equip.size(); i += UserEntity.EQUIP_FIELDS_PER_SLOT) {
                int itemId = equip.get(i);
                if (ResItem.getItemEquipment(itemId) == null) continue;
                UserEquipmentEntity gear = new UserEquipmentEntity(0, itemId);
                gear.setTier(1);
                EquipmentStatRollService.rollStatsIfNeeded(gear);
                List<Long> stats = gear.getPoint();
                if (stats == null) continue;
                for (int p = 0; p + 1 < stats.size(); p += 2) {
                    IMath.addPointData(point, stats.get(p).intValue(), stats.get(p + 1).floatValue());
                }
            }
        }
        point.calculatorPower();
        point.resetHp();
        return point;
    }

    static void retarget(BaseRoom room, FakeGatherPlayer bot, long now) {
        List<CellObject> pool = nearMineCells(room);
        Set<Integer> used = usedCellIds(room);
        if (bot.getGatherCell() != null) used.remove(bot.getGatherCell().getId());
        CellObject cell = pickCell(pool, used);
        if (cell == null) return;
        Pos stand = standPos(cell, bot.getFakeUserId());
        if (!room.isValidChunkId(room.worldPosToChunkId(stand))) stand = cell.getPos().clone();
        bot.assignCell(cell, stand, now + mineDuration());
    }

    static boolean isNewbie(BaseRoom room) {
        return room != null && room.getRoomType() == MapType.NEWBIE
                && (room.getRoomState() == RoomState.ACTIVE || room.getRoomState() == RoomState.PAUSE);
    }

    static int countReal(BaseRoom room) {
        int n = 0;
        for (int i = 0; i < room.aPlayerIds.size(); i++) {
            Unit unit = room.mUnit.get(room.aPlayerIds.get(i));
            if (unit instanceof Player p && p.getMUser() != null) n++;
        }
        return n;
    }

    static int countBots(BaseRoom room) {
        int n = 0;
        for (int i = 0; i < room.aPlayerIds.size(); i++) {
            if (room.mUnit.get(room.aPlayerIds.get(i)) instanceof FakeGatherPlayer) n++;
        }
        return n;
    }

    static void removeBots(BaseRoom room) {
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < room.aPlayerIds.size(); i++) {
            long id = room.aPlayerIds.get(i);
            if (room.mUnit.get(id) instanceof FakeGatherPlayer) ids.add(id);
        }
        for (int i = 0; i < ids.size(); i++) room.removeUnit(ids.get(i));
    }

    static Set<Integer> usedCellIds(BaseRoom room) {
        Set<Integer> used = new HashSet<>();
        for (int i = 0; i < room.aPlayerIds.size(); i++) {
            Unit unit = room.mUnit.get(room.aPlayerIds.get(i));
            if (unit instanceof FakeGatherPlayer bot && bot.getGatherCell() != null) {
                used.add(bot.getGatherCell().getId());
            }
        }
        return used;
    }

    static Set<String> usedNames(BaseRoom room) {
        Set<String> names = new HashSet<>();
        for (int i = 0; i < room.aPlayerIds.size(); i++) {
            Unit unit = room.mUnit.get(room.aPlayerIds.get(i));
            if (unit instanceof FakeGatherPlayer bot && bot.getName() != null) names.add(bot.getName());
        }
        return names;
    }

    static int nextFakeUserId(BaseRoom room) {
        int id = -1001;
        Set<Integer> used = new HashSet<>();
        for (int i = 0; i < room.aPlayerIds.size(); i++) {
            Unit unit = room.mUnit.get(room.aPlayerIds.get(i));
            if (unit instanceof FakeGatherPlayer bot) used.add(bot.getFakeUserId());
        }
        while (used.contains(id)) id--;
        return id;
    }

    static String nextName(Set<String> used) {
        List<String> bag = new ArrayList<>();
        for (String name : NAMES) {
            if (!used.contains(name)) bag.add(name);
        }
        String name;
        if (bag.isEmpty()) name = NAMES[NumberUtil.getRandom(NAMES.length)] + (100 + NumberUtil.getRandom(900));
        else name = bag.get(NumberUtil.getRandom(bag.size()));
        used.add(name);
        return name;
    }

    static long mineDuration() {
        return 8000L + NumberUtil.getRandom(10000);
    }

    static Pos standPos(CellObject cell, int salt) {
        float side = (salt & 1) == 0 ? STAND_OFFSET : -STAND_OFFSET;
        float along = ((salt / 3) % 2 == 0) ? 0.08f : -0.08f;
        return new Pos(cell.getPos().x + side, cell.getPos().y + along).round();
    }

    static CellObject pickCell(List<CellObject> pool, Set<Integer> used) {
        if (pool == null || pool.isEmpty()) return null;
        List<CellObject> free = new ArrayList<>();
        for (int i = 0; i < pool.size(); i++) {
            CellObject cell = pool.get(i);
            if (cell != null && cell.canAttack() && !used.contains(cell.getId())) free.add(cell);
        }
        List<CellObject> src = free.isEmpty() ? pool : free;
        return src.get(NumberUtil.getRandom(src.size()));
    }

    static List<CellObject> nearMineCells(BaseRoom room) {
        List<CellObject> all = new ArrayList<>();
        if (room.mChunk == null) return all;
        for (ChunkObject chunk : room.mChunk.values()) {
            if (chunk == null || chunk.getMCells() == null) continue;
            for (CellObject cell : chunk.getMCells().values()) {
                if (cell == null || cell.getPos() == null || !cell.canAttack()) continue;
                Pbmethod.CellObjectType type = cell.getObjectType();
                if (type == Pbmethod.CellObjectType.CHEST || type == Pbmethod.CellObjectType.SIGN) continue;
                all.add(cell);
            }
        }
        if (all.isEmpty()) return all;
        Pos anchor = room.getMapInfo() != null ? room.getMapInfo().getHeathCenter() : null;
        if (anchor == null) anchor = Pos.zero();
        Pos origin = anchor;
        all.sort((a, b) -> Double.compare(a.getPos().distance(origin), b.getPos().distance(origin)));
        int n = Math.min(NEAR_CELL_POOL, all.size());
        List<CellObject> near = new ArrayList<>(all.subList(0, n));
        Collections.shuffle(near);
        return near;
    }

    static int bodyKey(List<Integer> equip) {
        int idx = UserEntity.equipSlotIndex(Pbmethod.EquipSlotType.BODY.getNumber());
        if (idx < 0 || idx >= equip.size()) return 2000;
        int key = equip.get(idx);
        return key > 0 ? key : 2000;
    }

    /** Vũ khí, mũ, áo, giày, áo choàng — chỉ các id trong danh sách rank 1. */
    static final int[] WEAPONS = {1302000, 1302005, 1312004};
    static final int[] HATS = {1002330, 1002363, 1002383};
    static final int[] ARMORS = {1040010, 1040038, 1040057};
    static final int[] SHOES = {1070003, 1072033, 1072052};
    static final int[] CLOAKS = {1102004, 1102018, 1102030};

    static List<Integer> rollEquip() {
        List<Integer> equip = new ArrayList<>(UserEntity.EQUIP_LIST_SIZE);
        for (int i = 0; i < UserEntity.EQUIP_LIST_SIZE; i++) equip.add(0);
        putSlot(equip, Pbmethod.EquipSlotType.BODY.getNumber(), 2000);
        putSlot(equip, Pbmethod.EquipSlotType.HEAD.getNumber(), 12000);
        putSlot(equip, Pbmethod.EquipSlotType.WEAPON.getNumber(), pick(WEAPONS));
        putSlot(equip, Pbmethod.EquipSlotType.HAT.getNumber(), pick(HATS));
        putSlot(equip, Pbmethod.EquipSlotType.ARMOR.getNumber(), pick(ARMORS));
        putSlot(equip, Pbmethod.EquipSlotType.SHOES.getNumber(), pick(SHOES));
        putSlot(equip, Pbmethod.EquipSlotType.CLOAK.getNumber(), pick(CLOAKS));
        return equip;
    }

    static void putSlot(List<Integer> equip, int slotType, int itemKey) {
        int idx = UserEntity.equipSlotIndex(slotType);
        if (idx < 0 || itemKey <= 0) return;
        equip.set(idx, itemKey);
        equip.set(idx + 1, 1);
    }

    static int pick(int[] ids) {
        return ids[NumberUtil.getRandom(ids.length)];
    }
}
