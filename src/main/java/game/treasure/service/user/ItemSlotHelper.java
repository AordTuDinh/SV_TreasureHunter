package game.treasure.service.user;

import ozudo.base.helper.GsonUtil;
import ozudo.base.helper.StringHelper;

import java.util.ArrayList;
import java.util.List;

/** Quản lý user_data.item_slot — flat [bonusType, rowId, ...]; ô trống = [0, 0].
 * bonusType = wire Bonus.java (4=BONUS_ITEM, 9=BONUS_PET, 10=BONUS_MOUNT, 14=BONUS_MOB, 12=BONUS_EQUIPMENT, ...), không phải res_item.type. */
public final class ItemSlotHelper {
    public static final int EMPTY_TYPE = 0;
    public static final long EMPTY_ID = 0;

    private ItemSlotHelper() {
    }

    public static List<Long> parse(String json) {
        if (json == null || json.isEmpty() || "[]".equals(json))
            return new ArrayList<>();
        return new ArrayList<>(GsonUtil.strToListLong(json));
    }

    public static String serialize(List<Long> slots) {
        return StringHelper.toDBString(slots);
    }

    public static void ensureCapacity(List<Long> slots, int bagCount, int eventCount) {
        int need = (bagCount + eventCount) * 2;
        while (slots.size() < need)
            slots.add(0L);
    }

    /** item_slot chỉ có {@code bagCount} ô (consum / equip / pet / mount). */
    public static void ensureBagCapacity(List<Long> slots, int bagCount) {
        int need = bagCount * 2;
        while (slots.size() < need)
            slots.add(0L);
    }

    public static int getBonusType(List<Long> slots, int slotIndex) {
        int i = slotIndex * 2;
        return i < slots.size() ? slots.get(i).intValue() : 0;
    }

    public static long getRowId(List<Long> slots, int slotIndex) {
        int i = slotIndex * 2 + 1;
        return i < slots.size() ? slots.get(i) : 0;
    }

    public static void setPair(List<Long> slots, int slotIndex, int bonusType, long rowId) {
        int i = slotIndex * 2;
        while (slots.size() <= i + 1)
            slots.add(0L);
        slots.set(i, (long) bonusType);
        slots.set(i + 1, rowId);
    }

    public static void clearPair(List<Long> slots, int slotIndex) {
        setPair(slots, slotIndex, EMPTY_TYPE, EMPTY_ID);
    }

    public static boolean isEmpty(List<Long> slots, int slotIndex) {
        return getBonusType(slots, slotIndex) == EMPTY_TYPE && getRowId(slots, slotIndex) == EMPTY_ID;
    }

    public static Integer findFirstEmpty(List<Long> slots, int startIndex, int count) {
        for (int s = 0; s < count; s++) {
            int idx = startIndex + s;
            if (isEmpty(slots, idx))
                return idx;
        }
        return null;
    }

    public static int countOccupied(List<Long> slots, int startIndex, int count) {
        int n = 0;
        for (int s = 0; s < count; s++) {
            if (!isEmpty(slots, startIndex + s))
                n++;
        }
        return n;
    }

    public static Integer findSlotOf(List<Long> slots, int startIndex, int count, int bonusType, long rowId) {
        for (int s = 0; s < count; s++) {
            int idx = startIndex + s;
            if (getBonusType(slots, idx) == bonusType && getRowId(slots, idx) == rowId)
                return idx;
        }
        return null;
    }
}
