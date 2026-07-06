package game.treasure.service.item;

import com.google.protobuf.CodedOutputStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/** Gắn isTrading / inMarket vào proto item khi chưa regenerate Pbmethod từ .proto. */
public final class ProtoTradingWire {
    private ProtoTradingWire() {
    }

    public static byte[] appendPetMountTrading(byte[] base, int isTrading, int inMarket) {
        return appendInt32Pair(base, 12, isTrading, 13, inMarket);
    }

    public static byte[] appendItemTrading(byte[] base, int isTrading, int inMarket) {
        return appendInt32Pair(base, 10, isTrading, 11, inMarket);
    }

    public static byte[] appendEquipmentTrading(byte[] base, int isTrading, int inMarket) {
        return appendInt32Pair(base, 11, isTrading, 12, inMarket);
    }

    public static byte[] appendMaterialTrading(byte[] base, int isTrading, int inMarket) {
        return appendInt32Pair(base, 7, isTrading, 8, inMarket);
    }

    public static byte[] appendArtifactTrading(byte[] base, int isTrading, int inMarket) {
        return appendInt32Pair(base, 13, isTrading, 14, inMarket);
    }

    public static byte[] appendMobTrading(byte[] base, int isTrading, int inMarket) {
        return appendInt32Pair(base, 4, isTrading, 5, inMarket);
    }

    public static byte[] appendSkinTrading(byte[] base, int isCraft, int isTrading, int inMarket) {
        if (base == null)
            base = new byte[0];
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream(base.length + 32);
            out.write(base);
            CodedOutputStream coded = CodedOutputStream.newInstance(out);
            if (isCraft > 0)
                coded.writeInt32(6, isCraft);
            if (isTrading > 0)
                coded.writeInt32(7, isTrading);
            if (inMarket > 0)
                coded.writeInt32(8, inMarket);
            coded.flush();
            return out.toByteArray();
        } catch (IOException ex) {
            return base;
        }
    }

    static byte[] appendInt32Pair(byte[] base, int fieldTrading, int isTrading, int fieldMarket, int inMarket) {
        if (base == null)
            base = new byte[0];
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream(base.length + 16);
            out.write(base);
            CodedOutputStream coded = CodedOutputStream.newInstance(out);
            if (isTrading > 0)
                coded.writeInt32(fieldTrading, isTrading);
            if (inMarket > 0)
                coded.writeInt32(fieldMarket, inMarket);
            coded.flush();
            return out.toByteArray();
        } catch (IOException ex) {
            return base;
        }
    }
}
