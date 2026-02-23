package ozudo.net;

import game.config.CfgServer;
import game.config.lang.Lang;
import game.dragonhero.server.IAction;
import game.monitor.Online;
import game.protocol.CommonProto;
import io.netty.channel.*;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import ozudo.base.helper.ChUtil;
import ozudo.base.helper.Util;
import ozudo.base.log.Logs;

import java.net.SocketException;

public class AbstractHandler extends ChannelDuplexHandler {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof RequestMessage) {
            ResponseMessage responseMessage = new ResponseMessage((RequestMessage) msg, ctx.channel());
            if (responseMessage.getService() == IAction.PING_IDLE) {
                return;
            }
            Object returnObject = responseMessage.getResponse();
            if (returnObject != null) {
                ctx.channel().writeAndFlush(returnObject).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
            }
        } else {
            ctx.channel().close();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (cause instanceof SocketException) {
        } else {
            ctx.channel().close();
            super.exceptionCaught(ctx, cause);
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent e = (IdleStateEvent) evt;
            Channel channel = ctx.channel();
            if (e.state() == IdleState.READER_IDLE) {
                Integer idle = ChUtil.getInteger(channel, ChUtil.KEY_IDLE);
                idle = idle == null ? 0 : idle;
                if (idle == 0) {
                    Util.sendProtoData(channel, null, IAction.PING_IDLE);
                    ChUtil.set(channel, ChUtil.KEY_IDLE, ++idle);
                } else {
                    Util.sendProtoData(channel, CommonProto.getCommonVector(Lang.getTitle(CfgServer.config.mainLanguage, Lang.msg_disconnect_idle)), IAction.DISCONNECT_MSG);
                    channel.close();
                }

            } else if (e.state() == IdleState.WRITER_IDLE) {

            }
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        //System.out.println("channel Active");
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        super.channelRegistered(ctx);
        //Online.addChannel(ctx.channel());
        //getLogger().info("channelRegister " + Online.getAllChanel().size());
    }

    @Override
    public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        super.close(ctx, promise);
        Online.logoutChannel(ctx.channel());
        //getLogger().info("channelClose  ");
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        super.channelUnregistered(ctx);
//        Online.mChannel.remove(ctx.channel());
        Online.logoutChannel(ctx.channel());
        //getLogger().info("channelUnregister: " + Online.channels.size());
//        Logs.info("channelUnregister: " + Online.mChannel.size());
    }


}
