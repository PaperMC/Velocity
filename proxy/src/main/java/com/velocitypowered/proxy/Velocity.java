package com.velocitypowered.proxy;

import com.velocitypowered.proxy.console.VelocityConsole;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Velocity {
    private static final Logger logger = LogManager.getLogger(Velocity.class);

    static {
        // We use BufferedImage for favicons, and on macOS this puts the Java application in the dock. How inconvenient.
        // Force AWT to work with its head chopped off.
        System.setProperty("java.awt.headless", "true");
    }

    public static void main(String... args) {
        logger.info("Booting up Velocity...");

        final VelocityServer server = VelocityServer.getServer();
        server.start();

        Runtime.getRuntime().addShutdownHook(new Thread(server::shutdown, "Shutdown thread"));
        new VelocityConsole(server).start();
    }
}
