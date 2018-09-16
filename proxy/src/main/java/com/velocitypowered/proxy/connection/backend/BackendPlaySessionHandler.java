package com.velocitypowered.proxy.connection.backend;

import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.VelocityConstants;
import com.velocitypowered.proxy.connection.client.ClientPlaySessionHandler;
import com.velocitypowered.proxy.connection.util.ConnectionMessages;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolConstants;
import com.velocitypowered.proxy.protocol.packet.*;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.util.PluginMessageUtil;
import io.netty.buffer.ByteBuf;

public class BackendPlaySessionHandler implements MinecraftSessionHandler {
    private final VelocityServer server;
    private final VelocityServerConnection connection;

    public BackendPlaySessionHandler(VelocityServer server, VelocityServerConnection connection) {
        this.server = server;
        this.connection = connection;
    }

    @Override
    public void activated() {
        server.getEventManager().fireAndForget(new ServerConnectedEvent(connection.getPlayer(), connection.getServerInfo()));
    }

    @Override
    public void handle(MinecraftPacket packet) {
        if (!connection.getPlayer().isActive()) {
            // Connection was left open accidentally. Close it so as to avoid "You logged in from another location"
            // errors.
            connection.disconnect();
            return;
        }

        ClientPlaySessionHandler playerHandler =
                (ClientPlaySessionHandler) connection.getPlayer().getConnection().getSessionHandler();
        if (packet instanceof KeepAlive) {
            // Forward onto the player
            playerHandler.setLastPing(((KeepAlive) packet).getRandomId());
            connection.getPlayer().getConnection().write(packet);
        } else if (packet instanceof Disconnect) {
            Disconnect original = (Disconnect) packet;
            connection.disconnect();
            connection.getPlayer().handleConnectionException(connection.getServerInfo(), original);
        } else if (packet instanceof JoinGame) {
            playerHandler.handleBackendJoinGame((JoinGame) packet);
        } else if (packet instanceof BossBar) {
            BossBar bossBar = (BossBar) packet;
            switch (bossBar.getAction()) {
                case 0: // add
                    playerHandler.getServerBossBars().add(bossBar.getUuid());
                    break;
                case 1: // remove
                    playerHandler.getServerBossBars().remove(bossBar.getUuid());
                    break;
            }
            connection.getPlayer().getConnection().write(packet);
        } else if (packet instanceof PluginMessage) {
            PluginMessage pm = (PluginMessage) packet;
            if (!canForwardPluginMessage(pm)) {
                return;
            }

            if (PluginMessageUtil.isMCBrand(pm)) {
                connection.getPlayer().getConnection().write(PluginMessageUtil.rewriteMCBrand(pm));
                return;
            }

            if (!connection.hasCompletedJoin() && pm.getChannel().equals(VelocityConstants.FORGE_LEGACY_HANDSHAKE_CHANNEL)) {
                if (!connection.isLegacyForge()) {
                    connection.setLegacyForge(true);

                    // We must always reset the handshake before a modded connection is established if
                    // we haven't done so already.
                    connection.getPlayer().sendLegacyForgeHandshakeResetPacket();
                }

                // Always forward these messages during login
                connection.getPlayer().getConnection().write(pm);
                return;
            }

            PluginMessageEvent event = new PluginMessageEvent(connection, connection.getPlayer(), server.getChannelRegistrar().getFromId(pm.getChannel()),
                    pm.getData());
            server.getEventManager().fire(event)
                    .thenAcceptAsync(pme -> {
                        if (pme.getResult().isAllowed()) {
                            connection.getPlayer().getConnection().write(pm);
                        }
                    }, connection.getMinecraftConnection().getChannel().eventLoop());
        } else if (connection.hasCompletedJoin()) {
            // Just forward the packet on. We don't have anything to handle at this time.
            connection.getPlayer().getConnection().write(packet);
        }
    }

    @Override
    public void handleUnknown(ByteBuf buf) {
        if (!connection.getPlayer().isActive()) {
            // Connection was left open accidentally. Close it so as to avoid "You logged in from another location"
            // errors.
            connection.disconnect();
            return;
        }

        if (connection.hasCompletedJoin()) {
            connection.getPlayer().getConnection().write(buf.retain());
        }
    }

    @Override
    public void exception(Throwable throwable) {
        connection.getPlayer().handleConnectionException(connection.getServerInfo(), throwable);
    }

    public VelocityServer getServer() {
        return server;
    }

    @Override
    public void disconnected() {
        if (connection.isGracefulDisconnect()) {
            return;
        }
        connection.getPlayer().handleConnectionException(connection.getServerInfo(), Disconnect.create(ConnectionMessages.UNEXPECTED_DISCONNECT));
    }

    private boolean canForwardPluginMessage(PluginMessage message) {
        ClientPlaySessionHandler playerHandler =
                (ClientPlaySessionHandler) connection.getPlayer().getConnection().getSessionHandler();
        boolean isMCOrFMLMessage;
        if (connection.getMinecraftConnection().getProtocolVersion() <= ProtocolConstants.MINECRAFT_1_12_2) {
            String channel = message.getChannel();
            isMCOrFMLMessage = channel.startsWith("MC|") || channel.startsWith(VelocityConstants.FORGE_LEGACY_HANDSHAKE_CHANNEL);
        } else {
            isMCOrFMLMessage = message.getChannel().startsWith("minecraft:");
        }
        return isMCOrFMLMessage || playerHandler.getClientPluginMsgChannels().contains(message.getChannel()) ||
                server.getChannelRegistrar().registered(message.getChannel());
    }
}
