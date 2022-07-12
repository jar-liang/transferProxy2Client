package me.jar.clients;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import me.jar.constants.ProxyConstants;
import me.jar.handler.ProxyHandler;
import me.jar.utils.Byte2TransferMsgDecoder;
import me.jar.utils.LengthContentDecoder;
import me.jar.utils.TransferMsg2ByteEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @Description
 * @Date 2021/4/27-21:31
 */
public class ClientServer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientServer.class);

    public static void connectProxyServer() throws InterruptedException {
        EventLoopGroup workGroup = new NioEventLoopGroup(1);
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(workGroup).channel(NioSocketChannel.class)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000).handler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) {
                    ChannelPipeline pipeline = ch.pipeline();
                    pipeline.addLast("lengthContent", new LengthContentDecoder());
                    pipeline.addLast("decoder", new Byte2TransferMsgDecoder());
                    pipeline.addLast("encoder", new TransferMsg2ByteEncoder());
                    pipeline.addLast("idleEvt", new IdleStateHandler(60, 30, 0));
                    pipeline.addLast("proxyHandler", new ProxyHandler());
                }
            });
            String serverAgentIp = ProxyConstants.PROPERTY.get(ProxyConstants.FAR_SERVER_IP);
            String serverAgentPort = ProxyConstants.PROPERTY.get(ProxyConstants.FAR_SERVER_PORT);
            int serverAgentPortNum = Integer.parseInt(serverAgentPort);
            Channel channel = bootstrap.connect(serverAgentIp, serverAgentPortNum).channel();
            channel.closeFuture().addListener(future -> {
                LOGGER.info("last client agent close, shutdown workGroup and retry in 10 seconds...");
                workGroup.shutdownGracefully();
                new Thread(() -> {
                    while (true) {
                        try {
                            Thread.sleep(10000L);
                        } catch (InterruptedException interruptedException) {
                            LOGGER.error("sleep 10s was interrupted!");
                        }
                        try {
                            connectProxyServer();
                            break;
                        } catch (InterruptedException e) {
                            LOGGER.error("channel close retry connection failed. detail: " + e.getMessage());
                        }
                    }
                }).start();
            });
        } catch (Exception e) {
            LOGGER.error("===client agent start failed, cause: " + e.getMessage());
            workGroup.shutdownGracefully();
        }
    }

    public static void main(String[] args) throws InterruptedException {
       connectProxyServer();
    }
}
