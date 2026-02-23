package game.config.aEnum;

import game.protocol.CommonProto;
import protocol.Pbmethod;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public enum ToastType {
    NORMAL(0), // màu đen bình thường
    SUCCESS(1), // thành công
    FAIL(2), // thất bại
    TOAST_GREEN(4),
    TOAST_RED(5),
    TOAST_YELLOW(6),

    ;

    public final int value;

    ToastType(int value) {
        this.value = value;
    }

    static Map<Integer, ToastType> lookup = new HashMap<>();

    static {
        for (ToastType type : values()) {
            lookup.put(type.value, type);
        }
    }

    public static ToastType get(int type) {
        return lookup.get(type);
    }

    public Pbmethod.CommonVector retToast(String msg) {
        return CommonProto.getCommonVectorProto(Arrays.asList((long) value), Arrays.asList(msg));
    }
}
