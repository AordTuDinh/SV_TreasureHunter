package game.treasure.service.battle;

import game.battle.model.Player;
import game.battle.model.Unit;
import game.config.CfgServer;
import game.config.aEnum.DetailActionType;
import game.config.lang.Lang;
import game.object.MyUser;
import game.treasure.BattleConfig;
import game.treasure.mapping.UserEquipmentEntity;
import game.treasure.mapping.UserEntity;
import game.treasure.mapping.UserMailEntity;
import game.treasure.mapping.UserMaterialEntity;
import game.treasure.mapping.main.ResItemEquipmentEntity;
import game.treasure.mapping.main.ResMapEntity;
import game.treasure.service.user.Bonus;
import game.treasure.table.BaseRoom;
import game.treasure.task.dbcache.MailCreatorCache;
import ozudo.base.helper.NumberUtil;
import ozudo.base.helper.StringHelper;
import protocol.Pbmethod;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Penalty khi player chết theo vùng map (typeRoom).
 */
public final class DeathPenaltyService {
    private static final int TIER_DROP = 4;

    private DeathPenaltyService() {
    }

    public static void apply(Player victim, Unit killer) {
        if (victim == null || victim.getMUser() == null || victim.getRoom() == null || victim.getPos() == null)
            return;

        MyUser victimUser = victim.getMUser();
        int typeRoom = resolveTypeRoom(victim);
        long goldPenalty = calcGoldPenalty(victimUser.getUser().getGold(), typeRoom);

        if (goldPenalty > 0) {
            List<Long> goldWire = Bonus.receiveListItem(victimUser, DetailActionType.DEATH_PENALTY.getKey(),
                    Bonus.viewGold(-goldPenalty));
            if (!goldWire.isEmpty())
                victim.protoStatus(Pbmethod.SubStateType.ADD_BONUS, goldWire);
        }

        DropSelection drop = rollItemDrop(victimUser, typeRoom);
        if (drop == null) {
            if (killer != null && killer.isPlayer() && goldPenalty > 0)
                sendKillerMail(victim, killer.getPlayer(), goldPenalty, null);
            return;
        }

        boolean wasEquipped = detachDropFromVictim(victimUser, drop);
        if (wasEquipped)
            syncVictimAfterUnequip(victim);

        if (killer != null && killer.isPlayer()) {
            escrowDropForMail(drop);
            sendKillerMail(victim, killer.getPlayer(), goldPenalty, drop);
        } else {
            destroyDrop(drop);
        }

        notifyVictimItemRemoved(victim, drop);
    }

    static int resolveTypeRoom(Player victim) {
        BaseRoom room = victim.getRoom();
        ResMapEntity map = room.getMapInfo();
        if (map == null)
            return 1;
        int chunkId = room.worldPosToChunkId(victim.getPos());
        int typeRoom = map.getTypeRoom(chunkId);
        return typeRoom > 0 ? typeRoom : 1;
    }

    static long calcGoldPenalty(long currentGold, int typeRoom) {
        if (currentGold <= 0)
            return 0;
        int percent = switch (typeRoom) {
            case 2 -> 20;
            case 3 -> 30;
            default -> 10;
        };
        return (long) Math.floor(currentGold * percent / 100.0);
    }

    static DropSelection rollItemDrop(MyUser victimUser, int typeRoom) {
        int dropRate = switch (typeRoom) {
            case 2 -> 10;
            case 3 -> 20;
            default -> 0;
        };
        if (dropRate <= 0 || NumberUtil.getRandom(100) >= dropRate)
            return null;
        return pickRandomTier4Drop(victimUser);
    }

    static DropSelection pickRandomTier4Drop(MyUser victimUser) {
        List<UserEquipmentEntity> equips = victimUser.getResources().getMEquipment().values().stream()
                .filter(e -> e.getTier() == TIER_DROP)
                .collect(Collectors.toList());
        List<UserMaterialEntity> materials = victimUser.getResources().getMMaterial().values().stream()
                .filter(m -> m.getTier() == TIER_DROP)
                .collect(Collectors.toList());
        if (equips.isEmpty() && materials.isEmpty())
            return null;

        boolean pickEquip = NumberUtil.getRandom(100) < 50;
        if (pickEquip && !equips.isEmpty()) {
            UserEquipmentEntity equip = equips.get(NumberUtil.getRandom(equips.size()));
            DropSelection drop = new DropSelection();
            drop.equipment = true;
            drop.rowId = equip.getId();
            drop.itemKey = equip.getItemId();
            drop.tier = equip.getTier();
            drop.equip = equip;
            return drop;
        }
        if (!materials.isEmpty()) {
            UserMaterialEntity material = materials.get(NumberUtil.getRandom(materials.size()));
            DropSelection drop = new DropSelection();
            drop.equipment = false;
            drop.rowId = material.getId();
            drop.itemKey = material.getMaterialId();
            drop.tier = material.getTier();
            drop.material = material;
            return drop;
        }
        if (equips.isEmpty())
            return null;
        UserEquipmentEntity equip = equips.get(NumberUtil.getRandom(equips.size()));
        DropSelection drop = new DropSelection();
        drop.equipment = true;
        drop.rowId = equip.getId();
        drop.itemKey = equip.getItemId();
        drop.tier = equip.getTier();
        drop.equip = equip;
        return drop;
    }

    static boolean detachDropFromVictim(MyUser victimUser, DropSelection drop) {
        if (drop.equipment)
            return detachEquipment(victimUser, drop.equip);
        detachMaterial(victimUser, drop.material);
        return false;
    }

    static boolean detachEquipment(MyUser victimUser, UserEquipmentEntity equip) {
        boolean wasEquipped = equip.isEquip()
                || victimUser.getUser().getListIdEquipmentEquip().contains((int) equip.getId());
        if (wasEquipped)
            clearItemFromEquipList(victimUser, (int) equip.getId(), equip);
        Bonus.clearItemFromSlot(victimUser, Bonus.BONUS_EQUIPMENT, equip.getId());
        equip.unEquip();
        victimUser.getResources().removeEquipment(equip.getId());
        return wasEquipped;
    }

    static void detachMaterial(MyUser victimUser, UserMaterialEntity material) {
        victimUser.getResources().removeMaterial(material.getId());
    }

    static void escrowDropForMail(DropSelection drop) {
        int escrowId = BattleConfig.P_escrowUserId;
        if (drop.equipment) {
            drop.equip.setUserId(escrowId);
            drop.equip.update(List.of("user_id", escrowId));
        } else {
            drop.material.setUserId(escrowId);
            drop.material.update(List.of("user_id", escrowId));
        }
    }

    static void destroyDrop(DropSelection drop) {
        if (drop.equipment)
            drop.equip.deleteFromDb();
        else
            drop.material.deleteFromDb();
    }

    static void sendKillerMail(Player victim, Player killer, long goldPenalty, DropSelection drop) {
        MyUser killerUser = killer.getMUser();
        if (killerUser == null || killerUser.getUser() == null)
            return;

        String victimName = victim.getName() != null ? victim.getName() : "...";
        String title = "Chiến lợi phẩm của bạn khi hạ " + victimName;

        List<Long> mailBonus = new ArrayList<>();
        if (goldPenalty > 0)
            mailBonus.addAll(Bonus.viewGold(goldPenalty));
        if (drop != null) {
            int typeBonus = drop.equipment ? Bonus.BONUS_EQUIPMENT : Bonus.BONUS_MATERIAL;
            mailBonus.addAll(List.of(
                    (long) Bonus.BONUS_CHANGE_OWNER,
                    (long) typeBonus,
                    drop.rowId,
                    (long) drop.itemKey));
        }
        if (mailBonus.isEmpty())
            return;

        String lang = CfgServer.config != null ? CfgServer.config.mainLanguage : "vi";
        MailCreatorCache.sendMail(UserMailEntity.builder()
                .userId(killerUser.getUser().getId())
                .senderId(0)
                .senderName(Lang.getTitle(lang, Lang.mail_sender_system))
                .title(title)
                .message(title)
                .bonus(StringHelper.toDBString(mailBonus))
                .build()
                .initDefault());
    }

    static void notifyVictimItemRemoved(Player victim, DropSelection drop) {
        if (drop.equipment)
            victim.protoStatus(Pbmethod.SubStateType.REMOVE_EQUIPMENT, drop.rowId);
        else
            victim.protoStatus(Pbmethod.SubStateType.REMOVE_MATERIAL, drop.rowId);
    }

    static void syncVictimAfterUnequip(Player victim) {
        victim.getMUser().reCalculatePoint();
        List<Integer> equipList = victim.getMUser().getUser().normalizeItemEquipList();
        if (equipList == null || equipList.isEmpty())
            return;
        List<Long> wire = equipList.stream().map(Integer::longValue).collect(Collectors.toList());
        victim.protoStatus(Pbmethod.SubStateType.UPDATE_ITEM_EQUIP, wire);
    }

    static boolean clearItemFromEquipList(MyUser mUser, int itemId, UserEquipmentEntity item) {
        List<Integer> lst = mUser.getUser().normalizeItemEquipList();
        int slotIndex = UserEntity.findEquipSlotByItemId(lst, itemId);
        if (slotIndex < 0) {
            ResItemEquipmentEntity resEquip = item.getResEquipment();
            if (resEquip == null)
                return false;
            slotIndex = mUser.getUser().equipSlotIndex(resEquip.getType());
            if (slotIndex < 0 || lst.get(slotIndex) != itemId)
                return false;
        }
        lst.set(slotIndex, 0);
        lst.set(slotIndex + 1, 0);
        lst.set(slotIndex + 2, 0);
        return mUser.getUser().updateItemEquip(lst);
    }

    static final class DropSelection {
        boolean equipment;
        long rowId;
        int itemKey;
        int tier;
        UserEquipmentEntity equip;
        UserMaterialEntity material;
    }
}
