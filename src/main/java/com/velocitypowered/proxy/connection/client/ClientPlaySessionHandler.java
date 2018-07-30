package com.velocitypowered.proxy.connection.client;

import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.backend.ServerConnection;
import com.velocitypowered.proxy.data.ServerInfo;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.packets.*;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.util.ThrowableUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.EventLoop;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class ClientPlaySessionHandler implements MinecraftSessionHandler {
    private static final Logger logger = LogManager.getLogger(ClientPlaySessionHandler.class);
    private static final int MAX_PLUGIN_CHANNELS = 128;

    private final ConnectedPlayer player;
    private ScheduledFuture<?> pingTask;
    private long lastPing = -1;
    private boolean spawned = false;
    private final List<UUID> serverBossBars = new ArrayList<>();
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
        long randomId = ThreadLocalRandom.current().nextInt();
        lastPing = randomId;
        KeepAlive keepAlive = new KeepAlive();
        keepAlive.setRandomId(randomId);
        player.getConnection().write(keepAlive);
    }

    @Override
    public void handle(MinecraftPacket packet) {
        if (packet instanceof KeepAlive) {
            KeepAlive keepAlive = (KeepAlive) packet;
            if (keepAlive.getRandomId() != lastPing) {
                throw new IllegalStateException("Client sent invalid keepAlive; expected " + lastPing + ", got " + keepAlive.getRandomId());
            }

            // Do not forward the packet to the player's server, because we handle pings for all servers already.
            return;
        }

        if (packet instanceof ClientSettings) {
            player.setClientSettings((ClientSettings) packet);
            // forward it on
        }

        if (packet instanceof Chat) {
            Chat chat = (Chat) packet;
            if (chat.getMessage().equals("/connect")) {
                ServerInfo info = new ServerInfo("test", new InetSocketAddress("localhost", 25566));
                ServerConnection connection = new ServerConnection(info, player, VelocityServer.getServer());
                connection.connect();
            }
        }

        if (packet instanceof PluginMessage) {
            handlePluginMessage((PluginMessage) packet, false);
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

    @Override
    public void exception(Throwable throwable) {
        player.close(TextComponent.builder()
                .content("An exception occurred in your connection: ")
                .color(TextColor.RED)
                .append(TextComponent.of(ThrowableUtils.briefDescription(throwable), TextColor.WHITE))
                .build());
    }

    public void handleBackendJoinGame(JoinGame joinGame) {
        if (!spawned) {
            // nothing special to do here
            spawned = true;
            currentDimension = joinGame.getDimension();
            player.getConnection().delayedWrite(joinGame);
        } else {
            // In order to handle switching to another server we will need send three packets:
            // - The join game packet from the backend server
            // - A respawn packet with a different dimension
            // - Another respawn with the correct dimension
            // We can't simply ignore the packet with the different dimension. If you try to be smart about it it doesn't
            // work.
            player.getConnection().delayedWrite(joinGame);
            int tempDim = joinGame.getDimension() == 0 ? -1 : 0;
            player.getConnection().delayedWrite(new Respawn(tempDim, joinGame.getDifficulty(), joinGame.getGamemode(), joinGame.getLevelType()));
            player.getConnection().delayedWrite(new Respawn(joinGame.getDimension(), joinGame.getDifficulty(), joinGame.getGamemode(), joinGame.getLevelType()));
            currentDimension = joinGame.getDimension();
        }

        // Resend client settings packet to remote server if we have it, this preserves client settings across
        // transitions.
        if (player.getClientSettings() != null) {
            player.getConnectedServer().getChannel().write(player.getClientSettings());
        }

        // Remove old boss bars.
        for (UUID serverBossBar : serverBossBars) {
            BossBar deletePacket = new BossBar();
            deletePacket.setUuid(serverBossBar);
            deletePacket.setAction(1); // remove
            player.getConnection().delayedWrite(deletePacket);
        }

        serverBossBars.clear();
        player.getConnection().flush();
    }

    public void setCurrentDimension(int currentDimension) {
        this.currentDimension = currentDimension;
    }

    public List<UUID> getServerBossBars() {
        return serverBossBars;
    }

    public void handlePluginMessage(PluginMessage packet, boolean fromBackend) {
        logger.info("Got plugin message packet {}", packet);

        // TODO: this certainly isn't the right approach, need a better way!
        /*if (packet.getChannel().equals("REGISTER")) {
            List<String> actuallyRegistered = new ArrayList<>();
            List<String> channels = PluginMessageUtil.getChannels(packet);
            for (String channel : channels) {
                if (pluginMessageChannels.size() >= MAX_PLUGIN_CHANNELS && !pluginMessageChannels.contains(channel)) {
                    throw new IllegalStateException("Too many plugin message channels registered");
                }
                if (pluginMessageChannels.add(channel)) {
                    actuallyRegistered.add(channel);
                }
            }

            if (actuallyRegistered.size() > 0) {
                logger.info("Rewritten register packet: {}", actuallyRegistered);
                PluginMessage newRegisterPacket = PluginMessageUtil.constructChannelsPacket("REGISTER", actuallyRegistered);
                if (fromBackend) {
                    player.getConnection().write(newRegisterPacket);
                } else {
                    player.getConnectedServer().getChannel().write(newRegisterPacket);
                }
            }

            return;
        }

        if (packet.getChannel().equals("UNREGISTER")) {
            List<String> channels = PluginMessageUtil.getChannels(packet);
            pluginMessageChannels.removeAll(channels);
        }*/

        if (packet.getChannel().equals("MC|Brand")) {
            // Rewrite this packet to indicate that Velocity is running. Hurrah!
            ByteBuf currentBrandBuf = Unpooled.wrappedBuffer(packet.getData());

            ByteBuf buf = Unpooled.buffer();
            byte[] rewrittenBrand;
            try {
                String currentBrand = ProtocolUtils.readString(currentBrandBuf);
                logger.info("Remote server brand: {}", currentBrand);
                ProtocolUtils.writeString(buf, currentBrand + " (Velocity)");
                rewrittenBrand = new byte[buf.readableBytes()];
                buf.readBytes(rewrittenBrand);
            } finally {
                buf.release();
            }
            packet.setData(rewrittenBrand);
        }

        // No other special handling?
        if (fromBackend) {
            player.getConnection().write(packet);
        } else {
            player.getConnectedServer().getChannel().write(packet);
        }
    }
}
