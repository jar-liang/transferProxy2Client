package me.jar.utils;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import me.jar.constants.TransferMsgType;
import me.jar.message.TransferMsg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

public class CommonHandler extends ChannelInboundHandlerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommonHandler.class);
    protected Channel channel;

    public Channel getChannel() {
        return channel;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        this.channel = ctx.channel();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOGGER.error("===caught exception： " + cause.getMessage());
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state() == IdleState.READER_IDLE) {
                LOGGER.info("===Read idle loss connection. close channel: " + ctx.channel().toString());
                ctx.close();
            }
            if (event.state() == IdleState.WRITER_IDLE) {
                TransferMsg transferMsg = new TransferMsg();
                transferMsg.setType(TransferMsgType.KEEPALIVE);
                transferMsg.setMetaData(new HashMap<>());
                ctx.writeAndFlush(transferMsg);
            }
        }
    }
}
