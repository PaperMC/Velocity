package com.velocitypowered.proxy.connection.backend;

import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.connection.VelocityConstants;
import com.velocitypowered.proxy.connection.client.ClientPlaySessionHandler;
import com.velocitypowered.proxy.connection.util.ConnectionMessages;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolConstants;
import com.velocitypowered.proxy.protocol.packet.*;
import com.velocitypowered.proxy.protocol.util.PluginMessageUtil;
import io.netty.buffer.ByteBuf;

public class BackendPlaySessionHandler implements MinecraftSessionHandler {
    private final VelocityServer server;
    private final VelocityServerConnection serverConn;

    public BackendPlaySessionHandler(VelocityServer server, VelocityServerConnection serverConn) {
        this.server = server;
        this.serverConn = serverConn;
    }

    @Override
    public void activated() {
        server.getEventManager().fireAndForget(new ServerConnectedEvent(serverConn.getPlayer(), serverConn.getServer()));
        serverConn.getServer().addPlayer(serverConn.getPlayer());
    }

    @Override
    public void handle(MinecraftPacket packet) {
        if (!serverConn.getPlayer().isActive()) {
            // Connection was left open accidentally. Close it so as to avoid "You logged in from another location"
            // errors.
            serverConn.disconnect();
            return;
        }

        ClientPlaySessionHandler playerHandler =
                (ClientPlaySessionHandler) serverConn.getPlayer().getConnection().getSessionHandler();
        if (packet instanceof KeepAlive) {
            // Forward onto the player
            serverConn.setLastPingId(((KeepAlive) packet).getRandomId());
            serverConn.getPlayer().getConnection().write(packet);
        } else if (packet instanceof Disconnect) {
            Disconnect original = (Disconnect) packet;
            serverConn.disconnect();
            serverConn.getPlayer().handleConnectionException(serverConn.getServer(), original);
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
            serverConn.getPlayer().getConnection().write(packet);
        } else if (packet instanceof PluginMessage) {
            PluginMessage pm = (PluginMessage) packet;
            if (!canForwardPluginMessage(pm)) {
                return;
            }

            if (PluginMessageUtil.isMCBrand(pm)) {
                serverConn.getPlayer().getConnection().write(PluginMessageUtil.rewriteMCBrand(pm));
                return;
            }

            if (!serverConn.hasCompletedJoin() && pm.getChannel().equals(VelocityConstants.FORGE_LEGACY_HANDSHAKE_CHANNEL)) {
                if (!serverConn.isLegacyForge()) {
                    serverConn.setLegacyForge(true);

                    // We must always reset the handshake before a modded connection is established if
                    // we haven't done so already.
                    serverConn.getPlayer().sendLegacyForgeHandshakeResetPacket();
                }

                // Always forward these messages during login
                serverConn.getPlayer().getConnection().write(pm);
                return;
            }

            ChannelIdentifier id = server.getChannelRegistrar().getFromId(pm.getChannel());
            if (id == null) {
                serverConn.getPlayer().getConnection().write(pm);
            } else {
                PluginMessageEvent event = new PluginMessageEvent(serverConn, serverConn.getPlayer(), id, pm.getData());
                server.getEventManager().fire(event)
                        .thenAcceptAsync(pme -> {
                            if (pme.getResult().isAllowed()) {
                                serverConn.getPlayer().getConnection().write(pm);
                            }
                        }, serverConn.getConnection().eventLoop());
            }
        } else if (packet instanceof TabCompleteResponse) {
            playerHandler.handleTabCompleteResponse((TabCompleteResponse) packet);
        } else if (serverConn.hasCompletedJoin()) {
            // Just forward the packet on. We don't have anything to handle at this time.
            serverConn.getPlayer().getConnection().write(packet);
        }
    }

    @Override
    public void handleUnknown(ByteBuf buf) {
        if (!serverConn.getPlayer().isActive()) {
            // Connection was left open accidentally. Close it so as to avoid "You logged in from another location"
            // errors.
            serverConn.disconnect();
            return;
        }

        if (serverConn.hasCompletedJoin()) {
            serverConn.getPlayer().getConnection().write(buf.retain());
        }
    }

    @Override
    public void exception(Throwable throwable) {
        serverConn.getPlayer().handleConnectionException(serverConn.getServer(), throwable);
    }

    public VelocityServer getServer() {
        return server;
    }

    @Override
    public void disconnected() {
        serverConn.getServer().removePlayer(serverConn.getPlayer());
        if (!serverConn.isGracefulDisconnect()) {
            serverConn.getPlayer().handleConnectionException(serverConn.getServer(), Disconnect.create(
                    ConnectionMessages.UNEXPECTED_DISCONNECT));
        }
    }

    private boolean canForwardPluginMessage(PluginMessage message) {
        ClientPlaySessionHandler playerHandler =
                (ClientPlaySessionHandler) serverConn.getPlayer().getConnection().getSessionHandler();
        boolean isMCOrFMLMessage;
        if (serverConn.getConnection().getProtocolVersion() <= ProtocolConstants.MINECRAFT_1_12_2) {
            String channel = message.getChannel();
            isMCOrFMLMessage = channel.startsWith("MC|") || channel.startsWith(VelocityConstants.FORGE_LEGACY_HANDSHAKE_CHANNEL);
        } else {
            isMCOrFMLMessage = message.getChannel().startsWith("minecraft:");
        }
        return isMCOrFMLMessage || playerHandler.getClientPluginMsgChannels().contains(message.getChannel()) ||
                server.getChannelRegistrar().registered(message.getChannel());
    }
}
