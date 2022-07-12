package me.jar.handler;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.bytes.ByteArrayDecoder;
import io.netty.handler.codec.bytes.ByteArrayEncoder;
import me.jar.constants.ProxyConstants;
import me.jar.constants.TransferMsgType;
import me.jar.exception.TransferProxyException;
import me.jar.message.TransferMsg;
import me.jar.utils.CommonHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Description
 * @Date 2021/4/27-21:50
 */
public class ProxyHandler extends CommonHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyHandler.class);
    private static final Map<String, Channel> CHANNEL_MAP = new ConcurrentHashMap<>();

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof TransferMsg) {
            TransferMsg transferMsg = (TransferMsg) msg;
            TransferMsgType type = transferMsg.getType();
            Map<String, Object> metaData = transferMsg.getMetaData();
            String channelId = String.valueOf(metaData.get(ProxyConstants.CHANNEL_ID));
            switch (type) {
                case REGISTER_RESULT:
                    if ("1".equals(metaData.get("result"))) {
                        LOGGER.info("register to transfer proxy server successfully");
                    } else {
                        LOGGER.error("register failed, reason: " + metaData.get("reason"));
                        ctx.close();
                    }
                    break;
//                case CONNECT: // 不需要了，直接根据是否有连接判断进行处理，在下面的DATA的case中处理连接
//                    connectTarget(ctx, msg, metaData);
//                    break;
                case DISCONNECT:
                    Channel channel = CHANNEL_MAP.get(channelId);
                    if (channel != null) {
                        channel.close();
                        CHANNEL_MAP.remove(channelId);
                    }
                    break;
                case DATA:
                    Channel channelData = CHANNEL_MAP.get(channelId);
                    if (channelData == null || !channelData.isActive()) {
                        connectTarget(ctx, transferMsg.getDate(), metaData);
                    } else {
                        channelData.writeAndFlush(transferMsg.getDate());
                    }
                    break;
                case KEEPALIVE:
                    break;
                default:
                    throw new TransferProxyException("unknown type: " + type.getType());
            }
        }
    }

    private void connectTarget(ChannelHandlerContext ctx, byte[] data, Map<String, Object> metaData) {
        String channelId = String.valueOf(metaData.get(ProxyConstants.CHANNEL_ID));
        EventLoopGroup workGroup = new NioEventLoopGroup(1);
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(workGroup).channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000).handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) {
                ChannelPipeline pipeline = ch.pipeline();
                pipeline.addLast("byteArrayDecoder", new ByteArrayDecoder());
                pipeline.addLast("byteArrayEncoder", new ByteArrayEncoder());
                pipeline.addLast("clientHandler", new ClientHandler(ctx.channel(), channelId));
                CHANNEL_MAP.put(channelId, ch);
            }
        });
        String targetIp = ProxyConstants.PROPERTY.get(ProxyConstants.TARGET_IP);
        String targetPort = ProxyConstants.PROPERTY.get(ProxyConstants.TARGET_PORT);
        try {
            int targetPortNum = Integer.parseInt(targetPort);
            bootstrap.connect(targetIp, targetPortNum).addListener((ChannelFutureListener) connectFuture -> {
                if (connectFuture.isSuccess()) {
                    connectFuture.channel().writeAndFlush(data);
                    LOGGER.info(">>>Connect target server and send data successfully. target channel: " + connectFuture.channel().toString());
                } else {
                    LOGGER.error("===Failed to connect to target server! host: " + targetIp + " , port: " + targetPortNum);
                    sendDisconnectMsgAndRemoveChannel(ctx, channelId);
                }
            });
        } catch (Exception e) {
            LOGGER.error("===Failed to connect to target server! cause: " + e.getMessage());
            sendDisconnectMsgAndRemoveChannel(ctx, channelId);
        }
    }

    private void sendDisconnectMsgAndRemoveChannel(ChannelHandlerContext ctx, String channelId) {
        TransferMsg disconnectMsg = new TransferMsg();
        disconnectMsg.setType(TransferMsgType.DISCONNECT);
        Map<String, Object> failMetaData = new HashMap<>(1);
        failMetaData.put(ProxyConstants.CHANNEL_ID, channelId);
        disconnectMsg.setMetaData(failMetaData);
        ctx.writeAndFlush(disconnectMsg);
        CHANNEL_MAP.remove(channelId);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        LOGGER.info("start to register to server agent...");
        TransferMsg transferMsg = new TransferMsg();
        transferMsg.setType(TransferMsgType.REGISTER);
        Map<String, Object> metaData = new HashMap<>(3);
        String userName = ProxyConstants.PROPERTY.get(ProxyConstants.USER_NAME);
        metaData.put("userName", userName);
        String password = ProxyConstants.PROPERTY.get(ProxyConstants.USER_PASSWORD);
        metaData.put("password", password);
        String server2ClientPort = ProxyConstants.PROPERTY.get(ProxyConstants.SERVER_CLIENT_PORT);
        metaData.put("port", server2ClientPort);
        transferMsg.setMetaData(metaData);
        ctx.writeAndFlush(transferMsg);
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        LOGGER.info("===client agent to server agent connection inactive, channel: " + ctx.channel().toString());
        CHANNEL_MAP.values().forEach(e -> {
            if (e != null && e.isActive()) {
                e.close();
            }
        });
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOGGER.error("===client agent has caught exception, cause: {}", cause.getMessage());
        ctx.close();
    }
}
