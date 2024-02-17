/*
 * Copyright (C) 2024 Velocity Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.velocitypowered.proxy.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Utility class for TCP Fast Open.
 */
public class TcpFastOpenUtil {

  private static final Logger logger = LogManager.getLogger(TcpFastOpenUtil.class);

  private static final int TFO_ENABLED_CLIENT_MASK = 0x1;
  private static final int TFO_ENABLED_SERVER_MASK = 0x2;

  private static final int TCP_FASTOPEN_MODE = tcpFastopenMode();
  public static final boolean IS_SUPPORTING_TCP_FASTOPEN_CLIENT =
      (TCP_FASTOPEN_MODE & TFO_ENABLED_CLIENT_MASK) == TFO_ENABLED_CLIENT_MASK;
  public static final boolean IS_SUPPORTING_TCP_FASTOPEN_SERVER =
      (TCP_FASTOPEN_MODE & TFO_ENABLED_SERVER_MASK) == TFO_ENABLED_SERVER_MASK;

  private static int tcpFastopenMode() {
    return AccessController.doPrivileged((PrivilegedAction<Integer>) () -> {
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
    });
  }
}
