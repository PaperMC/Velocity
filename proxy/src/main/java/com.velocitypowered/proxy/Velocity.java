package com.velocitypowered.proxy;

public class Velocity {
    public static void main(String... args) throws InterruptedException {
        final VelocityServer server = VelocityServer.getServer();
        server.start();

        Runtime.getRuntime().addShutdownHook(new Thread(server::shutdown, "Shutdown thread"));

        Thread.currentThread().join();
    }
}
