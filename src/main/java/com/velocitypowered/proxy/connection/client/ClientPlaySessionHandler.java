package com.velocitypowered.proxy.connection.client;

import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.backend.ServerConnection;
import com.velocitypowered.proxy.data.ServerInfo;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.packets.Chat;
import com.velocitypowered.proxy.protocol.packets.JoinGame;
import com.velocitypowered.proxy.protocol.packets.Ping;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.packets.Respawn;
import io.netty.buffer.ByteBuf;
import io.netty.channel.EventLoop;

import java.net.InetSocketAddress;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class ClientPlaySessionHandler implements MinecraftSessionHandler {
    private final ConnectedPlayer player;
    private ScheduledFuture<?> pingTask;
    private long lastPing = -1;
    private boolean spawned = false;
    private int currentDimension;

    public ClientPlaySessionHandler(ConnectedPlayer player) {
        this.player = player;
    }

    @Override
    public void activated() {
        EventLoop loop = player.getConnection().getChannel().eventLoop();
        pingTask = loop.scheduleAtFixedRate(this::ping, 5, 15, TimeUnit.SECONDS);
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

        if (packet instanceof Chat) {
            Chat chat = (Chat) packet;
            if (chat.getMessage().equals("/connect")) {
                ServerInfo info = new ServerInfo("test", new InetSocketAddress("localhost", 25566));
                ServerConnection connection = new ServerConnection(info, player, VelocityServer.getServer());
                connection.connect();
            }
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

    public void handleBackendJoinGame(JoinGame joinGame) {
        if (!spawned) {
            // nothing special to do here
            spawned = true;
            currentDimension = joinGame.getDimension();
            player.getConnection().write(joinGame);
        } else {
            // In order to handle switching to another server we will need send three packets:
            // - The join game packet
            // - A respawn packet, with a different dimension, if it differs
            // - Another respawn with the correct dimension
            player.getConnection().write(joinGame);
            if (joinGame.getDimension() == currentDimension) {
                int tempDim = joinGame.getDimension() == 0 ? -1 : 0;
                player.getConnection().write(new Respawn(tempDim, joinGame.getDifficulty(), joinGame.getGamemode(), joinGame.getLevelType()));
            }
            player.getConnection().write(new Respawn(joinGame.getDimension(), joinGame.getDifficulty(), joinGame.getGamemode(), joinGame.getLevelType()));
            currentDimension = joinGame.getDimension();
        }
    }

    public void setCurrentDimension(int currentDimension) {
        this.currentDimension = currentDimension;
    }
}
