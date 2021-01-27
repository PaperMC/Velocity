package com.velocitypowered.proxy;

import io.netty.util.ResourceLeakDetector;
import io.netty.util.ResourceLeakDetector.Level;
import java.text.DecimalFormat;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Velocity {

  private static final Logger logger;

  static {
    System.setProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager");
    logger = LogManager.getLogger(Velocity.class);

    // We use BufferedImage for favicons, and on macOS this puts the Java application in the dock.
    // How inconvenient. Force AWT to work with its head chopped off.
    System.setProperty("java.awt.headless", "true");

    // By default, Netty allocates 16MiB arenas for the PooledByteBufAllocator. This is too much
    // memory for Minecraft, which imposes a maximum packet size of 2MiB! We'll use 4MiB as a more
    // sane default.
    //
    // Note: io.netty.allocator.pageSize << io.netty.allocator.maxOrder is the formula used to
    // compute the chunk size. We lower maxOrder from its default of 11 to 9. (We also use a null
    // check, so that the user is free to choose another setting if need be.)
    if (System.getProperty("io.netty.allocator.maxOrder") == null) {
      System.setProperty("io.netty.allocator.maxOrder", "9");
    }

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
