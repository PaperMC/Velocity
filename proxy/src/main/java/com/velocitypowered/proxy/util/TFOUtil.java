package com.velocitypowered.proxy.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public class TFOUtil {

    private static final Logger logger = LogManager.getLogger(TFOUtil.class);

    private static final int TFO_ENABLED_CLIENT_MASK = 0x1;
    private static final int TFO_ENABLED_SERVER_MASK = 0x2;

    private static final int TCP_FASTOPEN_MODE = tcpFastopenMode();
    public static final boolean IS_SUPPORTING_TCP_FASTOPEN_CLIENT =
            (TCP_FASTOPEN_MODE & TFO_ENABLED_CLIENT_MASK) == TFO_ENABLED_CLIENT_MASK;
    public static final boolean IS_SUPPORTING_TCP_FASTOPEN_SERVER =
            (TCP_FASTOPEN_MODE & TFO_ENABLED_SERVER_MASK) == TFO_ENABLED_SERVER_MASK;

    private static int tcpFastopenMode() {
        int fastopen = 0;
        File file = new File("/proc/sys/net/ipv4/tcp_fastopen");
        if (file.exists()) {
            try (BufferedReader in = new BufferedReader(new FileReader(file))) {
                fastopen = Integer.parseInt(in.readLine());
                if (logger.isDebugEnabled()) {
                    logger.debug("{}: {}", file, fastopen);
                }
            } catch (Exception e) {
                logger.debug("Failed to get TCP_FASTOPEN from: {}", file, e);
            }
            // Ignored.
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("{}: {} (non-existent)", file, fastopen);
            }
        }
        return fastopen;
    }
}
