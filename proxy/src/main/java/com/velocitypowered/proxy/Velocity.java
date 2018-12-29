package com.velocitypowered.proxy;

import java.text.DecimalFormat;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Velocity {

  private static final Logger logger = LogManager.getLogger(Velocity.class);

  static {
    // We use BufferedImage for favicons, and on macOS this puts the Java application in the dock.
    // How inconvenient. Force AWT to work with its head chopped off.
    System.setProperty("java.awt.headless", "true");
  }

  /**
   * Main method that the JVM will call when {@code java -jar velocity.jar} is executed.
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
  }
}
