package ozudo.net.tcp;

import io.netty.channel.ChannelHandlerContext;
import ozudo.net.AbstractHandler;
import game.dragonhero.server.Constans;
import ozudo.base.helper.ChUtil;

public class TCPHandler extends AbstractHandler {
    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        super.channelRegistered(ctx);
        ChUtil.set(ctx.channel(), Constans.KEY_PROTOCOL, Constans.PROTOCOL_TCP);
    }
}
