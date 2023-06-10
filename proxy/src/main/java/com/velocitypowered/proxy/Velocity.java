/*
 * Copyright (C) 2018-2023 Velocity Contributors
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

package com.velocitypowered.proxy;

import io.netty.util.ResourceLeakDetector;
import io.netty.util.ResourceLeakDetector.Level;
import java.text.DecimalFormat;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The main class. Responsible for parsing command line arguments and then launching the
 * proxy.
 */
public class Velocity {

  private static final Logger logger;

  static {
    System.setProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager");
    logger = LogManager.getLogger(Velocity.class);

    // We use BufferedImage for favicons, and on macOS this puts the Java application in the dock.
    // How inconvenient. Force AWT to work with its head chopped off.
    System.setProperty("java.awt.headless", "true");

    // If Velocity's natives are being extracted to a different temporary directory, make sure the
    // Netty natives are extracted there as well
    if (System.getProperty("velocity.natives-tmpdir") != null) {
      System.setProperty("io.netty.native.workdir", System.getProperty("velocity.natives-tmpdir"));
    }

    // Disable the resource leak detector by default as it reduces performance. Allow the user to
    // override this if desired.
    if (System.getProperty("io.netty.leakDetection.level") == null) {
      ResourceLeakDetector.setLevel(Level.DISABLED);
    }
  }

  /**
   * Main method that the JVM will call when {@code java -jar velocity.jar} is executed.
   *
   * @param args the arguments to the proxy
   */
  public static void main(String... args) {
    final ProxyOptions options = new ProxyOptions(args);
    if (options.isHelp()) {
      return;
    }

    long startTime = System.currentTimeMillis();

    VelocityServer server = new VelocityServer(options);
    server.start();
    Runtime.getRuntime().addShutdownHook(new Thread(() -> server.shutdown(false),
        "Shutdown thread"));

    double bootTime = (System.currentTimeMillis() - startTime) / 1000d;
    logger.info("Done ({}s)!", new DecimalFormat("#.##").format(bootTime));
    server.getConsoleCommandSource().start();

    // If we don't have a console available (because SimpleTerminalConsole returned), then we still
    // need to wait, otherwise the JVM will reap us as no non-daemon threads will be active once the
    // main thread exits.
    server.awaitProxyShutdown();
  }
}
