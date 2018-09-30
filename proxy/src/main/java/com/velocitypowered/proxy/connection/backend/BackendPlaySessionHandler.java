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
    private final ClientPlaySessionHandler playerSessionHandler;

    BackendPlaySessionHandler(VelocityServer server, VelocityServerConnection serverConn) {
        this.server = server;
        this.serverConn = serverConn;
        this.playerSessionHandler = (ClientPlaySessionHandler) serverConn.getPlayer().getConnection().getSessionHandler();
    }

    @Override
    public void activated() {
        server.getEventManager().fireAndForget(new ServerConnectedEvent(serverConn.getPlayer(), serverConn.getServer()));
        serverConn.getServer().addPlayer(serverConn.getPlayer());
    }

    @Override
    public boolean beforeHandle() {
        if (!serverConn.getPlayer().isActive()) {
            // Obsolete connection
            serverConn.disconnect();
            return true;
        }
        return false;
    }

    @Override
    public boolean handle(KeepAlive packet) {
        serverConn.setLastPingId(packet.getRandomId());
        return false; // forwards on
    }

    @Override
    public boolean handle(Disconnect packet) {
        serverConn.disconnect();
        serverConn.getPlayer().handleConnectionException(serverConn.getServer(), packet);
        return true;
    }

    @Override
    public boolean handle(JoinGame packet) {
        playerSessionHandler.handleBackendJoinGame(packet);
        return true;
    }

    @Override
    public boolean handle(BossBar packet) {
        switch (packet.getAction()) {
            case 0: // add
                playerSessionHandler.getServerBossBars().add(packet.getUuid());
                break;
            case 1: // remove
                playerSessionHandler.getServerBossBars().remove(packet.getUuid());
                break;
        }
        return false; // forward
    }

    @Override
    public boolean handle(PluginMessage packet) {
        if (!canForwardPluginMessage(packet)) {
            return true;
        }

        if (PluginMessageUtil.isMCBrand(packet)) {
            serverConn.getPlayer().getConnection().write(PluginMessageUtil.rewriteMCBrand(packet));
            return true;
        }

        if (!serverConn.hasCompletedJoin() && packet.getChannel().equals(VelocityConstants.FORGE_LEGACY_HANDSHAKE_CHANNEL)) {
            if (!serverConn.isLegacyForge()) {
                serverConn.setLegacyForge(true);

                // We must always reset the handshake before a modded connection is established if
                // we haven't done so already.
                serverConn.getPlayer().sendLegacyForgeHandshakeResetPacket();
            }

            // Always forward these messages during login
            return false;
        }

        ChannelIdentifier id = server.getChannelRegistrar().getFromId(packet.getChannel());
        if (id == null) {
            serverConn.getPlayer().getConnection().write(packet);
        } else {
            PluginMessageEvent event = new PluginMessageEvent(serverConn, serverConn.getPlayer(), id, packet.getData());
            server.getEventManager().fire(event)
                    .thenAcceptAsync(pme -> {
                        if (pme.getResult().isAllowed()) {
                            serverConn.getPlayer().getConnection().write(packet);
                        }
                    }, serverConn.getConnection().eventLoop());
        }
        return true;
    }

    @Override
    public boolean handle(TabCompleteResponse packet) {
        playerSessionHandler.handleTabCompleteResponse(packet);
        return true;
    }

    @Override
    public void handleGeneric(MinecraftPacket packet) {
        if (!serverConn.getPlayer().isActive()) {
            // Connection was left open accidentally. Close it so as to avoid "You logged in from another location"
            // errors.
            serverConn.disconnect();
            return;
        }

        if (serverConn.hasCompletedJoin()) {
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
