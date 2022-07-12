package me.jar.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import me.jar.constants.ProxyConstants;
import me.jar.constants.TransferMsgType;
import me.jar.message.TransferMsg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * @Description
 * @Date 2021/4/27-21:50
 */
public class ClientHandler extends ChannelInboundHandlerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientHandler.class);
    private Channel proxyChannel;
    private String channelId;

    public ClientHandler(Channel proxyChannel, String channelId) {
        this.proxyChannel = proxyChannel;
        this.channelId = channelId;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof byte[]) {
            byte[] bytes = (byte[]) msg;
            Map<String, Object> metaData = new HashMap<>(1);
            metaData.put(ProxyConstants.CHANNEL_ID, channelId);
            TransferMsg transferMsg = new TransferMsg();
            transferMsg.setType(TransferMsgType.DATA);
            transferMsg.setMetaData(metaData);
            transferMsg.setDate(bytes);
            proxyChannel.writeAndFlush(transferMsg);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        LOGGER.info("===target channel inactive. channel: " + ctx.channel().toString());
        Map<String, Object> metaData = new HashMap<>(1);
        metaData.put(ProxyConstants.CHANNEL_ID, channelId);
        TransferMsg transferMsg = new TransferMsg();
        transferMsg.setType(TransferMsgType.DISCONNECT);
        transferMsg.setMetaData(metaData);
        proxyChannel.writeAndFlush(transferMsg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOGGER.error("===target channel has caught exception, cause: {}", cause.getMessage());
        ctx.close();
    }
}
