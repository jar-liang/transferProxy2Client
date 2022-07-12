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
 * @Date 2021/4/25-23:39
 */
public class ConnectClientHandler extends ChannelInboundHandlerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectClientHandler.class);
    private Channel proxyServer;

    public ConnectClientHandler(Channel proxyServer) {
        this.proxyServer = proxyServer;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof byte[]) {
            byte[] bytes = (byte[]) msg;
            TransferMsg transferMsg = new TransferMsg();
            transferMsg.setType(TransferMsgType.DATA);
            Map<String, Object> metaData = new HashMap<>(1);
            metaData.put(ProxyConstants.CHANNEL_ID, ctx.channel().id().asLongText());
            transferMsg.setMetaData(metaData);
            transferMsg.setDate(bytes);
            proxyServer.writeAndFlush(transferMsg);
        }
    }

//    @Override // 不需要，因为客户端那边会根据是否有连接进行自动连接，不需根据CONNECT消息进行
//    public void channelActive(ChannelHandlerContext ctx) throws Exception {
//        TransferMsg transferMsg = new TransferMsg();
//        transferMsg.setType(TransferMsgType.CONNECT);
//        Map<String, Object> metaData = new HashMap<>(1);
//        metaData.put(ProxyConstants.CHANNEL_ID, ctx.channel().id().asLongText());
//        transferMsg.setMetaData(metaData);
//        proxyServer.writeAndFlush(transferMsg);
//    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        TransferMsg transferMsg = new TransferMsg();
        transferMsg.setType(TransferMsgType.DISCONNECT);
        Map<String, Object> metaData = new HashMap<>(1);
        metaData.put(ProxyConstants.CHANNEL_ID, ctx.channel().id().asLongText());
        transferMsg.setMetaData(metaData);
        proxyServer.writeAndFlush(transferMsg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOGGER.error("===server2Client caught exception. channel:" + ctx.channel().toString() + ". cause: " + cause.getMessage());
        ctx.close();
    }
}
