package com.velocitypowered.proxy.connection.client;

import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.packets.Ping;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import io.netty.buffer.ByteBuf;
import io.netty.channel.EventLoop;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class PlaySessionHandler implements MinecraftSessionHandler {
    private final ConnectedPlayer player;
    private ScheduledFuture<?> pingTask;
    private long lastPing = -1;

    public PlaySessionHandler(ConnectedPlayer player) {
        this.player = player;
    }

    @Override
    public void activated() {
        EventLoop loop = player.getConnection().getChannel().eventLoop();
        loop.scheduleAtFixedRate(this::ping, 5, 15, TimeUnit.SECONDS);
    }

    private void ping() {
        long randomId = ThreadLocalRandom.current().nextLong();
        lastPing = randomId;
        Ping ping = new Ping();
        ping.setRandomId(randomId);
        player.getConnection().write(ping);
    }

    @Override
    public void handle(MinecraftPacket packet) {
        if (packet instanceof Ping) {
            Ping ping = (Ping) packet;
            if (ping.getRandomId() != lastPing) {
                throw new IllegalStateException("Client sent invalid ping; expected " + lastPing + ", got " + ping.getRandomId());
            }

            // Do not forward the packet to the player's server, because we handle pings for all servers already.
            return;
        }
        // If we don't want to handle this packet, just forward it on.
        player.getConnectedServer().getChannel().write(packet);
    }

    @Override
    public void handleUnknown(ByteBuf buf) {
        player.getConnectedServer().getChannel().write(buf.retain());
    }

    @Override
    public void disconnected() {
        player.getConnectedServer().disconnect();

        if (pingTask != null && !pingTask.isCancelled()) {
            pingTask.cancel(false);
            pingTask = null;
        }
    }
}
