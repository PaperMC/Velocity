package com.velocitypowered.proxy;

import com.velocitypowered.proxy.console.VelocityConsole;

public class Velocity {
    public static void main(String... args) {
        final VelocityServer server = VelocityServer.getServer();
        server.start();

        Runtime.getRuntime().addShutdownHook(new Thread(server::shutdown, "Shutdown thread"));
        new VelocityConsole(server).start();
    }
}
