package com.velocitypowered.proxy;

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
