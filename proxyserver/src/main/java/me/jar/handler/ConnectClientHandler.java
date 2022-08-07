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
    private final Channel proxyServer;
    private final Map<String, Channel> channelMap;

    public ConnectClientHandler(Channel proxyServer, Map<String, Channel> channelMap) {
        this.proxyServer = proxyServer;
        this.channelMap = channelMap;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof byte[]) {
            byte[] bytes = (byte[]) msg;
            int markByteIndex = bytes.length - ProxyConstants.MARK_BYTE.length;
            for (int i = 0; i < ProxyConstants.MARK_BYTE.length; i++) {
                if (bytes[markByteIndex + i] != ProxyConstants.MARK_BYTE[i]) {
                    LOGGER.info("===Illegal data from ip: {}", ctx.channel().remoteAddress());
                    ctx.close();
                    return;
                }
            }
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
        String id = ctx.channel().id().asLongText();
        channelMap.remove(id);
        TransferMsg transferMsg = new TransferMsg();
        transferMsg.setType(TransferMsgType.DISCONNECT);
        Map<String, Object> metaData = new HashMap<>(1);
        metaData.put(ProxyConstants.CHANNEL_ID, id);
        transferMsg.setMetaData(metaData);
        proxyServer.writeAndFlush(transferMsg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOGGER.error("===server2Client caught exception. channel:" + ctx.channel().toString() + ". cause: " + cause.getMessage());
        ctx.close();
    }

//    @Override
//    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
//        if (evt instanceof IdleStateEvent) {
//            IdleStateEvent event = (IdleStateEvent) evt;
//            if (event.state() == IdleState.ALL_IDLE) {
//                LOGGER.warn("no data read and write more than 10s, close connection");
//                channelMap.remove(ctx.channel().id().asLongText());
//                NettyUtil.closeOnFlush(ctx.channel());
//            }
//        }
//    }
}
