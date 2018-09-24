package com.velocitypowered.proxy;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.text.DecimalFormat;

public class Velocity {
    private static final Logger logger = LogManager.getLogger(Velocity.class);

    private static long startTime;

    static {
        // We use BufferedImage for favicons, and on macOS this puts the Java application in the dock. How inconvenient.
        // Force AWT to work with its head chopped off.
        System.setProperty("java.awt.headless", "true");
    }

    public static void main(String... args) {
        startTime = System.currentTimeMillis();
        logger.info("Booting up Velocity {}...", Velocity.class.getPackage().getImplementationVersion());

        VelocityServer server = new VelocityServer();
        server.start();

        Runtime.getRuntime().addShutdownHook(new Thread(server::shutdown, "Shutdown thread"));

        double bootTime = (System.currentTimeMillis() - startTime) / 1000d;
        logger.info("Done ({}s)!", new DecimalFormat("#.##").format(bootTime));
        server.getConsoleCommandSource().start();
    }
}
