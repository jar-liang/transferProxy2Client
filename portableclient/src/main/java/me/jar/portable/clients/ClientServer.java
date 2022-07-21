package me.jar.portable.clients;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import me.jar.constants.ProxyConstants;
import me.jar.portable.handler.ConnectFarHandler;
import me.jar.utils.NettyUtil;
import me.jar.utils.PlatformUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @Description
 * @Date 2021/4/27-21:31
 */
public class ClientServer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientServer.class);

    private final int port;

    public ClientServer(int port) {
        this.port = port;
    }

    public void run() {
        ChannelInitializer<SocketChannel> channelInitializer = new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) {
                ch.pipeline().addLast("connectFar", new ConnectFarHandler());
            }
        };
        NettyUtil.starServer(port, channelInitializer);
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            LOGGER.error("Usage: java -jar jarName.jar [index]. index is used to get property file");
            System.exit(0);
        }

        ProxyConstants.PROPERTY.clear();
        ProxyConstants.PROPERTY.putAll(PlatformUtil.getProperty(args[0]));

        if (ProxyConstants.PROPERTY.containsKey(ProxyConstants.KEY_NAME_PORT)) {
            String port = ProxyConstants.PROPERTY.get(ProxyConstants.KEY_NAME_PORT);
            try {
                int portNum = Integer.parseInt(port.trim());
                new ClientServer(portNum).run();
            } catch (NumberFormatException e) {
                LOGGER.error("===Failed to parse number, property setting may be wrong.", e);
            }
        } else {
            LOGGER.error("===Failed to get port from property, starting server failed.");
        }
    }
}
