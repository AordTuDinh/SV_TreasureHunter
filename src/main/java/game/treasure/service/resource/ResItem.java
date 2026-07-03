package game.treasure.service.resource;

import game.battle.calculate.IMath;
import game.battle.model.Player;
import game.battle.object.Point;
import game.config.CfgItem;
import game.config.CfgServer;
import game.config.aEnum.DetailActionType;
import protocol.Pbmethod;
import game.object.MyUser;
import game.protocol.CommonProto;
import game.treasure.mapping.UserEquipmentEntity;
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
import protocol.Pbmethod;

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

    /** Icon hiển thị — từ res_item.icon, fallback itemId. */
    public static int resolveIcon(int itemId) {
        ResItemEntity res = getItem(itemId);
        if (res != null && res.getIcon() > 0)
            return res.getIcon();
        return itemId;
    }

    public static ResMaterialEntity getMaterial(int materialId) {
        return mMaterial.get(materialId);
    }

    public static int getMaterialCount() {
        return mMaterial.size();
    }

    public static List<Integer> getSortedMaterialIds() {
        List<Integer> ids = new ArrayList<>(mMaterial.keySet());
        Collections.sort(ids);
        return ids;
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

    public static int resolveTier( int configTier) {
        if (configTier < 1) return 1;
        return Math.min(configTier, 4);
    }

    /**
     * Khởi tạo user_item.data cho medicine khi nhận vào túi.
     * Roll base từ res_item.data [min,max] → lưu [pointId, value], pointId = {@link Point#HP}.
     */
    public static void initConsumableInstanceData(UserItemEntity uItem) {
        if (uItem == null || uItem.getType() != Pbmethod.ItemType.POSITION.getNumber())
            return;
        if (!CfgItem.isItemMedicine(uItem.getItemId()))
            return;
        ResItemEntity res = uItem.getRes();
        if (res == null)
            return;
        float value = rollFloatRange(res.getData());
        if (value <= 0f)
            return;
        uItem.setData(toPointDataString(Point.HP, value));
    }

    /** Random float trong khoảng res_item.data, vd "[50,60]". */
    public static float rollFloatRange(String resData) {
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

    public static String toPointDataString(int pointId, float value) {
        return StringHelper.toDBString(Arrays.asList((float) pointId, IMath.round1(value)));
    }

    /** Kiểm tra item có data point hợp lệ để nâng cấp (medicine / equipment). */
    public static boolean hasUpgradePointData(UserItemEntity uItem) {
        if (uItem == null)
            return false;
        return uItem.hasUpgradePointData();
    }

    public static boolean hasUpgradePointData(UserEquipmentEntity uItem) {
        if (uItem == null)
            return false;
        return uItem.hasUpgradePointData();
    }

    /** user_item.data → wire [pointId, value, ...] đã nhân theo level. */
    public static List<Float> dataToPointWire(String data, int level) {
        if (data == null || data.length() < 2 || "[]".equals(data))
            return Collections.emptyList();
        List<Float> list = GsonUtil.strToListFloat(data);
        if (list.isEmpty())
            return Collections.emptyList();
        int itemLevel = level > 0 ? level : 1;
        if (list.size() >= 2) {
            List<Float> wire = new ArrayList<>();
            for (int i = 0; i + 1 < list.size(); i += 2) {
                int pointId = Math.round(list.get(i));
                float scaled = CfgItem.formatPointStat(pointId, CfgItem.getStatAtLevel(list.get(i + 1), itemLevel));
                wire.add(list.get(i));
                wire.add(scaled);
            }
            return wire;
        }
        float scaled = CfgItem.formatPointStat(Point.HP, CfgItem.getStatAtLevel(list.get(0), itemLevel));
        return new ArrayList<>(Arrays.asList((float) Point.HP, scaled));
    }

    public static int resolveTypeBonus(UserItemEntity uItem) {
        return Bonus.BONUS_ITEM;
    }

    public static int resolveTypeBonus(UserEquipmentEntity uItem) {
        return Bonus.BONUS_EQUIPMENT;
    }

    /** user_item.data → wire [pointId, value, ...]. */
    public static List<Float> dataToPointWire(String data) {
        return dataToPointWire(data, 1);
    }

    public static Pbmethod.PbPointItemUpdate buildPointItemUpdate(UserItemEntity uItem) {
        if (uItem == null)
            return null;
        List<Float> points = dataToPointWire(uItem.getData(), uItem.getLevel());
        if (points.isEmpty())
            return null;
        return Pbmethod.PbPointItemUpdate.newBuilder()
                .setItemId(uItem.getId())
                .setTypeBonus(resolveTypeBonus(uItem))
                .addAllPoints(points)
                .build();
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
        if (item.getType() != Pbmethod.ItemType.POSITION.getNumber())
            return false;
        if (item.getUserId() != mUser.getUser().getId())
            return false;

        float hpAdd = item.readHpBonus();
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

    /** Xóa 1 row user_item và trả chunk bonus [4,rowId,-itemKey] cho client. */
    public static List<Long> removeUserItemRow(MyUser mUser, UserItemEntity uItem, String detailAction) {
        if (uItem == null || mUser == null)
            return Collections.emptyList();
        long rowId = uItem.getId();
        int itemKey = uItem.getItemId();

        Bonus.clearItemFromSlot(mUser, Bonus.BONUS_ITEM, rowId);
        if (!uItem.deleteFromDb())
            return Collections.emptyList();
        mUser.getResources().removeItem(rowId);
        return List.of((long) Bonus.BONUS_ITEM, rowId, (long) -itemKey);
    }
}
