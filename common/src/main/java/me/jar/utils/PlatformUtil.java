package me.jar.utils;

import io.netty.util.CharsetUtil;
import me.jar.constants.ProxyConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 * @Description
 * @Date 2021/4/24-17:31
 */
public final class PlatformUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlatformUtil.class);
    public static final int PLATFORM_CODE;

    static {
        PLATFORM_CODE = getPlatform();
    }

    private PlatformUtil() {
    }

    public static Map<String, String> getProperty() {
        Map<String, String> propertyMap = new HashMap<>(20);
        String propertyFileName;
        if (PLATFORM_CODE == ProxyConstants.WIN_OS) {
            propertyFileName = ProxyConstants.PROPERTY_NAME_WIN;
        } else if (PLATFORM_CODE == ProxyConstants.LINUX_OS) {
            propertyFileName = ProxyConstants.PROPERTY_NAME_LINUX;
        } else {
            // 打印日志提示，不支持的系统
            LOGGER.warn("===Unsupported System!");
            return propertyMap;
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(propertyFileName), CharsetUtil.UTF_8))) {
            while (true) {
                String line = reader.readLine();
                if (line == null) {
                    break;
                }
                if (line.length() == 0) {
                    continue;
                }
                String[] split = line.split("=");
                if (split.length == 2) {
                    propertyMap.put(split[0], split[1]);
                }
            }
        } catch (IOException e) {
            // 打印日志提示，读取配置文件失败
            LOGGER.error("===Reading property file failed，please check!", e);
        }
        return propertyMap;
    }

    /**
     * 获取操作系统类型
     *
     * @return 整数，1代表Windows，2代表Linux，3代表其它系统
     */
    public static int getPlatform() {
        String osName = System.getProperty("os.name");
        if (osName == null || osName.length() == 0) {
            return ProxyConstants.OTHER_OS;
        }
        if (osName.contains("Windows")) {
            LOGGER.info("===Running on Windows.");
            return ProxyConstants.WIN_OS;
        } else if (osName.contains("Linux")){
            LOGGER.info("===Running on Linux");
            return ProxyConstants.LINUX_OS;
        } else {
            LOGGER.info("===Running on other os, which is {}", osName);
            return ProxyConstants.OTHER_OS;
        }
    }
}
