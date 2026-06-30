package game.treasure.service.item;

import com.google.protobuf.ByteString;
import com.google.protobuf.CodedOutputStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/** Gắn field proto 9 (data) và 10 (isEquip) khi PbPet/PbMount chưa regenerate từ .proto. */
public final class ProtoPetMountWire {
    private ProtoPetMountWire() {
    }

    public static byte[] appendDataAndIsEquip(byte[] baseMessage, String data, boolean isEquip) {
        if (baseMessage == null)
            baseMessage = new byte[0];
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream(baseMessage.length + 64);
            out.write(baseMessage);
            CodedOutputStream coded = CodedOutputStream.newInstance(out);
            if (data != null && !data.isEmpty() && !"[]".equals(data)) {
                coded.writeString(9, data);
            }
            if (isEquip) {
                coded.writeInt32(10, 1);
            }
            coded.flush();
            return out.toByteArray();
        } catch (IOException ex) {
            return baseMessage;
        }
    }

    public static ByteString appendDataAndIsEquip(ByteString baseMessage, String data, boolean isEquip) {
        byte[] base = baseMessage == null ? new byte[0] : baseMessage.toByteArray();
        return ByteString.copyFrom(appendDataAndIsEquip(base, data, isEquip));
    }
}
