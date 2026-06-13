package game.treasure.service.resource;

import game.battle.model.Player;
import game.config.CfgServer;
import game.config.aEnum.DetailActionType;
import game.config.aEnum.ItemType;
import game.object.MyUser;
import game.protocol.CommonProto;
import game.treasure.mapping.UserItemEntity;
import game.treasure.mapping.main.ResItemEntity;
import game.treasure.mapping.main.ResItemEquipmentEntity;
import game.treasure.mapping.main.ResMaterialEntity;
import game.treasure.server.IAction;
import game.treasure.service.user.Bonus;
import io.netty.channel.Channel;
import ozudo.base.database.DBResource;
import ozudo.base.helper.GsonUtil;
import ozudo.base.helper.NumberUtil;
import ozudo.base.helper.StringHelper;
import ozudo.base.helper.Util;

import java.util.*;

public class ResItem {
    // item
    static Map<Integer, ResItemEntity> mItem = new HashMap<>();
    // item equipment
    static Map<Integer, ResItemEquipmentEntity> mItemEquipment = new HashMap<>();
    // cover / stat materials (res_material)
    static Map<Integer, ResMaterialEntity> mMaterial = new HashMap<>();

    public static ResItemEquipmentEntity getItemEquipment(int itemId) {
        return mItemEquipment.get(itemId);
    }

    public static ResItemEntity getItem(int itemId) {
        return mItem.get(itemId);
    }

    public static ResMaterialEntity getMaterial(int materialId) {
        return mMaterial.get(materialId);
    }

    public static final int sizeItemEquipment = 24;

    public static void init() {
        // for item
        List<ResItemEntity> aItem = DBResource.getInstance().getList(CfgServer.DB_MAIN + "res_item",  ResItemEntity.class);
        mItem.clear();
        aItem.forEach(item -> {
            item.init();
            mItem.put(item.getId(), item);
        });

        // for item equipment
        List<ResItemEquipmentEntity> aItemEquipment = DBResource.getInstance().getList(CfgServer.DB_MAIN + "res_item_equipment", ResItemEquipmentEntity.class);
        mItemEquipment.clear();
        aItemEquipment.forEach(item -> mItemEquipment.put(item.getId(), item));

        List<ResMaterialEntity> aMaterial = DBResource.getInstance().getList(CfgServer.DB_MAIN + "res_material", ResMaterialEntity.class);
        mMaterial.clear();
        aMaterial.forEach(row -> mMaterial.put(row.getId(), row));
    }

    public static int resolveTier(int itemType, int itemKey, int configTier) {
        if (itemType == ItemType.EQUIPMENT.value) {
            if (configTier < 1) return 1;
            return Math.min(configTier, 4);
        }
        ResItemEntity res = getItem(itemKey);
        if (res != null)
            return res.getTier();
        return configTier > 0 ? configTier : 1;
    }

    /** Roll HP bonus từ res_item.data [min,max] → user_item.data "[hp]". Chỉ user_item.type=1. */
    public static void initConsumableInstanceData(UserItemEntity uItem) {
        if (uItem == null || uItem.getType() != ItemType.CONSUMABLE.value)
            return;
        ResItemEntity res = uItem.getRes();
        if (res == null)
            return;
        uItem.setTier(res.getTier());
        float hp = rollHpFromResData(res.getData());
        if (hp <= 0f)
            return;
        uItem.setData(StringHelper.toDBString(Collections.singletonList(hp)));
    }

    /** Random float trong khoảng res_item.data, vd "[10,15]". */
    public static float rollHpFromResData(String resData) {
        if (resData == null || resData.length() < 3)
            return 0f;
        List<Float> range = GsonUtil.strToListFloat(resData);
        if (range.size() < 2)
            return 0f;
        float min = range.get(0);
        float max = range.get(1);
        if (max < min) {
            float t = min;
            min = max;
            max = t;
        }
        return NumberUtil.getRandom(min, max);
    }

    public static float readHpBonus(UserItemEntity uItem) {
        if (uItem == null || uItem.getData() == null || uItem.getData().length() < 2)
            return 0f;
        List<Float> list = GsonUtil.strToListFloat(uItem.getData());
        if (list.isEmpty())
            return 0f;
        return list.get(0);
    }

    /** Dùng buff consumable trong map — trừ item, hồi HP (cap max), trả ITEM_USE sync túi. */
    public static boolean useBuffItemInRoom(Player player, long userItemId) {
        if (player == null || !player.isAlive())
            return false;
        MyUser mUser = player.getMUser();
        if (mUser == null)
            return false;

        UserItemEntity item = mUser.getResources().getItem(userItemId);
        if (item == null)
            return false;
        if (item.getType() != ItemType.CONSUMABLE.value)
            return false;
        if (item.getUserId() != mUser.getUser().getId())
            return false;

        float hpAdd = readHpBonus(item);
        List<Long> removed = removeUserItemRow(mUser, item, DetailActionType.SU_DUNG_ITEM.getKey(userItemId));
        if (removed.isEmpty())
            return false;

        int heal = Math.max(0, Math.round(hpAdd));
        if (heal > 0)
            player.reHp(heal);

        Channel channel = mUser.getChannel();
        if (channel != null)
            Util.sendProtoData(channel, CommonProto.getCommonVectorProto(removed), IAction.ITEM_USED);
        return true;
    }

    /** Xóa 1 row user_item và trả chunk bonus [4,type,rowId,itemKey,tier] cho client. */
    public static List<Long> removeUserItemRow(MyUser mUser, UserItemEntity uItem, String detailAction) {
        if (uItem == null || mUser == null)
            return Collections.emptyList();
        long rowId = uItem.getId();
        int storageType = uItem.getType();
        int itemKey = uItem.getItemId();

        if (uItem.isAggregatedItem()) {
            return Bonus.receiveListItem(mUser, detailAction,
                    Bonus.viewItemRemove(storageType, rowId, itemKey, uItem.getTier(), 1));
        }
        if (!uItem.deleteFromDb())
            return Collections.emptyList();
        mUser.getResources().removeItem(rowId);
        return List.of((long) Bonus.BONUS_ITEM, (long) storageType, rowId, (long) itemKey, (long) uItem.getTier());
    }
}
