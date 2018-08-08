package com.velocitypowered.proxy;

import com.velocitypowered.proxy.console.VelocityConsole;

public class Velocity {
    static {
        // We use BufferedImage for favicons, and on macOS this puts the Java application in the dock. How inconvenient.
        // Force AWT to work with its head chopped off.
        System.setProperty("java.awt.headless", "true");
    }

    public static void main(String... args) {
        final VelocityServer server = VelocityServer.getServer();
        server.start();

        Runtime.getRuntime().addShutdownHook(new Thread(server::shutdown, "Shutdown thread"));
        new VelocityConsole(server).start();
    }
}
