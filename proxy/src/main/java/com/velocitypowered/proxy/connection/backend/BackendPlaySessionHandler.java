package com.velocitypowered.proxy.connection.backend;

import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.client.ClientPlaySessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolConstants;
import com.velocitypowered.proxy.protocol.packet.*;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.util.PluginMessageUtil;
import io.netty.buffer.ByteBuf;

public class BackendPlaySessionHandler implements MinecraftSessionHandler {
    private final ServerConnection connection;

    public BackendPlaySessionHandler(ServerConnection connection) {
        this.connection = connection;
    }

    @Override
    public void activated() {
        VelocityServer.getServer().getEventManager().fireAndForget(new ServerConnectedEvent(connection.getProxyPlayer(),
                connection.getServerInfo()));
    }

    @Override
    public void handle(MinecraftPacket packet) {
        //Not handleable packets: Chat, TabCompleteResponse, Respawn, Scoreboard*
        if (!connection.getProxyPlayer().isActive()) {
            // Connection was left open accidentally. Close it so as to avoid "You logged in from another location"
            // errors.
            connection.getMinecraftConnection().close();
            return;
        }

        ClientPlaySessionHandler playerHandler =
                (ClientPlaySessionHandler) connection.getProxyPlayer().getConnection().getSessionHandler();
        if (packet instanceof KeepAlive) {
            // Forward onto the player
            playerHandler.setLastPing(((KeepAlive) packet).getRandomId());
            connection.getProxyPlayer().getConnection().write(packet);
        } else if (packet instanceof Disconnect) {
            Disconnect original = (Disconnect) packet;
            connection.getProxyPlayer().handleConnectionException(connection.getServerInfo(), original);
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
            connection.getProxyPlayer().getConnection().write(packet);
        } else if (packet instanceof PluginMessage) {
            PluginMessage pm = (PluginMessage) packet;
            if (!canForwardPluginMessage(pm)) {
                return;
            }

            if (PluginMessageUtil.isMCBrand(pm)) {
                connection.getProxyPlayer().getConnection().write(PluginMessageUtil.rewriteMCBrand(pm));
                return;
            }

            connection.getProxyPlayer().getConnection().write(pm);
        } else {
            // Just forward the packet on. We don't have anything to handle at this time.
            connection.getProxyPlayer().getConnection().write(packet);
        }
    }

    @Override
    public void handleUnknown(ByteBuf buf) {
        if (!connection.getProxyPlayer().isActive()) {
            // Connection was left open accidentally. Close it so as to avoid "You logged in from another location"
            // errors.
            connection.getMinecraftConnection().close();
            return;
        }

        ClientPlaySessionHandler playerHandler =
                (ClientPlaySessionHandler) connection.getProxyPlayer().getConnection().getSessionHandler();
        ByteBuf remapped = playerHandler.getIdRemapper().remap(buf, ProtocolConstants.Direction.CLIENTBOUND);
        connection.getProxyPlayer().getConnection().write(remapped);
    }

    @Override
    public void exception(Throwable throwable) {
        connection.getProxyPlayer().handleConnectionException(connection.getServerInfo(), throwable);
    }

    private boolean canForwardPluginMessage(PluginMessage message) {
        ClientPlaySessionHandler playerHandler =
                (ClientPlaySessionHandler) connection.getProxyPlayer().getConnection().getSessionHandler();
        if (connection.getMinecraftConnection().getProtocolVersion() <= ProtocolConstants.MINECRAFT_1_12_2) {
            return message.getChannel().startsWith("MC|") ||
                    playerHandler.getClientPluginMsgChannels().contains(message.getChannel());
        } else {
            return message.getChannel().startsWith("minecraft:") ||
                    playerHandler.getClientPluginMsgChannels().contains(message.getChannel());
        }
    }
}
