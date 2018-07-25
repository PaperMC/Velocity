package com.velocitypowered.proxy;

import com.velocitypowered.proxy.connection.VelocityServer;

public class Velocity {
    public static void main(String... args) throws InterruptedException {
        VelocityServer server = new VelocityServer();
        server.initialize();

        while (true) {
            // temporary until jline is added.
            Thread.sleep(999999);
        }
    }
}
