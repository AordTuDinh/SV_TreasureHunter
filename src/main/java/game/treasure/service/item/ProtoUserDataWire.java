package game.treasure.service.item;

import com.google.protobuf.CodedOutputStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public final class ProtoUserDataWire {
    private ProtoUserDataWire() {
    }

    public static byte[] appendSlotTrading(byte[] base, int slotTrading1, int slotTrading2) {
        if (base == null)
            base = new byte[0];
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream(base.length + 16);
            out.write(base);
            CodedOutputStream coded = CodedOutputStream.newInstance(out);
            if (slotTrading1 > 0)
                coded.writeInt32(34, slotTrading1);
            if (slotTrading2 >= 0)
                coded.writeInt32(35, slotTrading2);
            coded.flush();
            return out.toByteArray();
        } catch (IOException ex) {
            return base;
        }
    }
}
