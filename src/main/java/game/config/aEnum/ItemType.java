package game.config.aEnum;

import java.util.HashMap;
import java.util.Map;

public enum ItemType {
    ITEM_MATERIAL(1), // dạng vật liệu sự kiện, nguyên liệu.
    ITEM_OPEN(2), // dạng box mở ra vật phẩm
    ITEM_USE(3), // dạng item sử dụng được như hp,mp ..
    ITEM_USE_X1(4),// type item sử dụng x1
    QUEST_B(5), // mở ra nhiệm vụ cấp B
    LOTTE_MINI(6), // vé số nhỏ,
    LOTTE_NORMAL(7), //  vé số thường
    STONE_UPGRADE(8), // Đá cường hóa vũ khí
    LOTTE_SPECIAL(9), // vé số đặc biệt,
    ITEM_OPEN_STATIC(10), // mở ra nhận quà mặc định
    ITEM_USE_FOR_ITEM_1(11),// mở ra chọn item mũ   để sử dụng.
    ITEM_USE_FOR_ITEM_2(12),// mở ra chọn item găng  để sử dụng.
    ITEM_USE_FOR_ITEM_3(13),// mở ra chọn item áo  để sử dụng.
    ITEM_USE_FOR_ITEM_4(14),// mở ra chọn item giày  để sử dụng.
    ITEM_USE_FOR_ITEM_5(15),// mở ra chọn item vũ khí  để sử dụng.
    ITEM_USE_FOR_ITEM_6(16),// mở ra chọn item bảo vật  để sử dụng.
    ITEM_USE_FOR_ITEM_7(17),// mở ra chọn item nhẫn  để sử dụng.
    ITEM_USE_FOR_ITEM_8(18),// mở ra chọn item vòng cổ  để sử dụng.
    ;


    public final int value;


    ItemType(int value) {
        this.value = value;
    }

    // lookup
    static Map<Integer, ItemType> lookup = new HashMap<>();

    static {
        for (ItemType type : values()) {
            lookup.put(type.value, type);
        }
    }

    public static ItemType get(int type) {
        return lookup.get(type);
    }


}
