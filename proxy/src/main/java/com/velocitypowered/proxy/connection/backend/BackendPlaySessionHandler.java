package com.velocitypowered.proxy.connection.backend;

import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.connection.client.ClientPlaySessionHandler;
import com.velocitypowered.proxy.connection.forge.ForgeConstants;
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

        MinecraftSessionHandler psh = serverConn.getPlayer().getConnection().getSessionHandler();
        if (!(psh instanceof ClientPlaySessionHandler)) {
            throw new IllegalStateException("Initializing BackendPlaySessionHandler with no backing client play session handler!");
        }
        this.playerSessionHandler = (ClientPlaySessionHandler) psh;
    }

    @Override
    public void activated() {
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
            case BossBar.ADD:
                playerSessionHandler.getServerBossBars().add(packet.getUuid());
                break;
            case BossBar.REMOVE:
                playerSessionHandler.getServerBossBars().remove(packet.getUuid());
                break;
        }
        return false; // forward
    }

    @Override
    public boolean handle(PluginMessage packet) {
        MinecraftConnection smc = serverConn.getConnection();
        if (smc == null) {
            return true;
        }

        if (!canForwardPluginMessage(packet)) {
            return true;
        }

        if (PluginMessageUtil.isMCBrand(packet)) {
            serverConn.getPlayer().getConnection().write(PluginMessageUtil.rewriteMCBrand(packet));
            return true;
        }

        if (!serverConn.hasCompletedJoin() && packet.getChannel().equals(ForgeConstants.FORGE_LEGACY_HANDSHAKE_CHANNEL)) {
            if (!serverConn.isLegacyForge()) {
                serverConn.setLegacyForge(true);

                // We must always reset the handshake before a modded connection is established if
                // we haven't done so already.
                serverConn.getPlayer().sendLegacyForgeHandshakeResetPacket();
            }

            // Always forward these messages during login.
            return false;
        }

        ChannelIdentifier id = server.getChannelRegistrar().getFromId(packet.getChannel());
        if (id == null) {
            return false;
        }

        PluginMessageEvent event = new PluginMessageEvent(serverConn, serverConn.getPlayer(), id, packet.getData());
        server.getEventManager().fire(event)
                .thenAcceptAsync(pme -> {
                    if (pme.getResult().isAllowed()) {
                        smc.write(packet);
                    }
                }, smc.eventLoop());
        return true;
    }

    @Override
    public boolean handle(TabCompleteResponse packet) {
        playerSessionHandler.handleTabCompleteResponse(packet);
        return true;
    }

    @Override
    public boolean handle(PlayerListItem packet) {
        serverConn.getPlayer().getTabList().processBackendPacket(packet);
        return false; //Forward packet to player
    }

    @Override
    public void handleGeneric(MinecraftPacket packet) {
        serverConn.getPlayer().getConnection().write(packet);
    }

    @Override
    public void handleUnknown(ByteBuf buf) {
        serverConn.getPlayer().getConnection().write(buf.retain());
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
        MinecraftConnection mc = serverConn.getConnection();
        if (mc == null) {
            return false;
        }
        boolean isMCOrFMLMessage;
        if (mc.getProtocolVersion() <= ProtocolConstants.MINECRAFT_1_12_2) {
            String channel = message.getChannel();
            isMCOrFMLMessage = channel.startsWith("MC|") || channel.startsWith(ForgeConstants.FORGE_LEGACY_HANDSHAKE_CHANNEL);
        } else {
            isMCOrFMLMessage = message.getChannel().startsWith("minecraft:");
        }
        return isMCOrFMLMessage || playerSessionHandler.getClientPluginMsgChannels().contains(message.getChannel()) ||
                server.getChannelRegistrar().registered(message.getChannel());
    }
}
