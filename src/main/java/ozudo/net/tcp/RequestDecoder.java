package ozudo.net.tcp;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import game.dragonhero.server.Constans;
import ozudo.net.RequestMessage;

import java.net.InetSocketAddress;
import java.util.List;

public class RequestDecoder extends ByteToMessageDecoder {
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf buffer, List<Object> out) throws Exception {
        if (buffer.readableBytes() < 6) {
            return;
        }

        String address = ((InetSocketAddress) ctx.channel().remoteAddress()).getAddress().getHostAddress();
        while (buffer.readableBytes() >= 6) {
            if (!parsePackage(buffer, out, address)) {
                break;
            }
        }
    }

    boolean parsePackage(ByteBuf buffer, List<Object> out, String address) {
        if (buffer.readableBytes() < 6) {
            System.out.println("decode -------------------------------------");
            return false;
        }
        buffer.markReaderIndex();
        byte[] header = new byte[2];
        buffer.readBytes(header);
        String amazing = new String(header);
        int length = buffer.readInt();
        if (Constans.MAGICS.contains(amazing)) {
            if (length > 0) {
                if (buffer.readableBytes() < length) {
                    // dat control read ve vi tri ban dau de doc lai tu dau
                    buffer.resetReaderIndex();
                    return false;
                }
                byte[] data = new byte[length];
                buffer.readBytes(data);
                out.add(new RequestMessage(amazing, 0, data, address));
            } else {
                out.add(new RequestMessage(amazing, 0, null, address));
            }
            return true;
        } else {
//            System.out.println("sai cau truc");
            return false;
        }
    }
}
