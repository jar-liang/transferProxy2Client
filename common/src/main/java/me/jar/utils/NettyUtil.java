package me.jar.utils;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import me.jar.beans.HostAndPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @Description
 * @Date 2021/4/21-21:33
 */
public final class NettyUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(NettyUtil.class);

    private NettyUtil() {
    }

    public static void closeOnFlush(Channel channel) {
        if (channel != null && channel.isActive()) {
            channel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }
    }

    public static HostAndPort parseHostAndPort(String message, int defaultPort) {
        int port = defaultPort;
        if (message == null || message.length() == 0) {
            return new HostAndPort("", port);
        }
        String host = message;
        if (message.contains(":")) {
            String[] split = message.split(":");
            if (split.length == 2) {
                host = split[0];
                try {
                    port = Integer.parseInt(split[1]);
                } catch (NumberFormatException e) {
                    LOGGER.error("===Fail to parse number.", e);
                }
            }
        }
        return new HostAndPort(host, port);
    }

    public static void starServer(int port, ChannelHandler chanelInitializer) {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
                    .childOption(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childHandler(chanelInitializer);
            ChannelFuture cf = serverBootstrap.bind(port).sync();
            LOGGER.info(">>>Proxy server started, the listening port is {}.", port);
            cf.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

}
