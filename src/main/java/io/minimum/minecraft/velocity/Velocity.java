package io.minimum.minecraft.velocity;

import io.minimum.minecraft.velocity.proxy.VelocityServer;

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
