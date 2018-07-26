package com.velocitypowered.proxy;

public class Velocity {
    public static void main(String... args) throws InterruptedException {
        VelocityServer server = new VelocityServer();
        server.initialize();

        Thread.currentThread().join();
    }
}
