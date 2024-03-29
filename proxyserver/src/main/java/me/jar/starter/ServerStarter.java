package me.jar.starter;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.GlobalEventExecutor;
import me.jar.constants.ProxyConstants;
import me.jar.handler.ConnectProxyHandler;
import me.jar.utils.Byte2TransferMsgDecoder;
import me.jar.utils.LengthContentDecoder;
import me.jar.utils.NettyUtil;
import me.jar.utils.PlatformUtil;
import me.jar.utils.TransferMsg2ByteEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * @Description
 * @Date 2021/4/23-23:45
 */
public class ServerStarter {
    static {
        String path = ServerStarter.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        if (path.contains(".jar")) {
            String osName = System.getProperty("os.name");
            String tempPath;
            if (osName.contains("Windows")) {
                tempPath = path.substring(path.indexOf("/") + 1, path.indexOf(".jar"));
            } else {
                tempPath = path.substring(path.indexOf("/"), path.indexOf(".jar"));
            }
            String targetDirPath = tempPath.substring(0, tempPath.lastIndexOf("/"));
            System.out.println("target path: " + targetDirPath);
            System.setProperty("WORKDIR", targetDirPath);
        } else {
            System.out.println("current path not contain .jar file");
            System.exit(1);
        }
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerStarter.class);
    private static final ChannelGroup CHANNELS = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    private static final URL URL = ServerStarter.class.getProtectionDomain().getCodeSource().getLocation();

    public static void main(String[] args) {
        recordServer2ClientPortAtFixedRate(5000L, 60000L);

        Map<String, String> propertyMap = PlatformUtil.parseProperty2Map(URL);
        if (!propertyMap.isEmpty()) {
            ProxyConstants.PROPERTY.clear();
            ProxyConstants.PROPERTY.putAll(propertyMap);
        }
        if (ProxyConstants.PROPERTY.containsKey(ProxyConstants.SERVER_LISTEN_PORT)) {
            String port = ProxyConstants.PROPERTY.get(ProxyConstants.SERVER_LISTEN_PORT);
            try {
                int portNum = Integer.parseInt(port.trim());
                new ServerStarter().runForProxy(portNum);
            } catch (NumberFormatException e) {
                LOGGER.error("===Failed to parse number, property setting may be wrong.", e);
            }
        } else {
            LOGGER.error("===Failed to get port from property, starting server failed.");
        }
    }

    public void runForProxy(int port) {
        ChannelInitializer<SocketChannel> channelInitializer = new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) {
                ChannelPipeline pipeline = ch.pipeline();
                // 添加与客户端交互的handler
                pipeline.addLast("lengthContent", new LengthContentDecoder());
                pipeline.addLast("decoder", new Byte2TransferMsgDecoder());
                pipeline.addLast("encoder", new TransferMsg2ByteEncoder());
                pipeline.addLast("idleEvt", new IdleStateHandler(60, 30, 0));
                pipeline.addLast("connectProxy", new ConnectProxyHandler(CHANNELS));
            }
        };
        NettyUtil.starServer(port, channelInitializer);
    }

    private static void recordServer2ClientPortAtFixedRate(long delay, long period) {
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                try {
                    int size = CHANNELS.size();
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(">>>Current server2Client amount: [")
                            .append(size).append("]. ");
                    if (size > 0) {
                        stringBuilder.append("listening port: |");
                        for (Channel channel : CHANNELS) {
                            if (channel != null) {
                                String address = channel.localAddress().toString();
                                String port = address.substring(address.lastIndexOf(":") + 1);
                                stringBuilder.append(port).append("|");
                            }
                        }
                    }
                    LOGGER.info(stringBuilder.toString());
                } catch (Exception e) {
                    LOGGER.error("counting client agent failed!" + e.getMessage());
                }
            }
        };

        new Timer().schedule(timerTask, delay, period);
    }

    public static URL getUrl() {
        return URL;
    }
}
