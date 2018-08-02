package com.velocitypowered.proxy.connection.backend;

import com.velocitypowered.proxy.connection.client.ClientPlaySessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolConstants;
import com.velocitypowered.proxy.protocol.packet.*;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.util.PluginMessageUtil;
import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCountUtil;

public class BackendPlaySessionHandler implements MinecraftSessionHandler {
    private final ServerConnection connection;

    public BackendPlaySessionHandler(ServerConnection connection) {
        this.connection = connection;
    }

    @Override
    public void handle(MinecraftPacket packet) {
        ClientPlaySessionHandler playerHandler =
                (ClientPlaySessionHandler) connection.getProxyPlayer().getConnection().getSessionHandler();
        if (packet instanceof KeepAlive) {
            // Forward onto the server
            connection.getMinecraftConnection().write(packet);
        } else if (packet instanceof Disconnect) {
            Disconnect original = (Disconnect) packet;
            connection.getProxyPlayer().handleConnectionException(connection.getServerInfo(), original);
        } else if (packet instanceof JoinGame) {
            playerHandler.handleBackendJoinGame((JoinGame) packet);
        } else if (packet instanceof Respawn) {
            // Record the dimension switch, and then forward the packet on.
            playerHandler.setCurrentDimension(((Respawn) packet).getDimension());
            connection.getProxyPlayer().getConnection().write(packet);
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
            try {
                PluginMessage newPacket = pm;
                if (!canForwardPluginMessage(newPacket)) {
                    return;
                }

                if (newPacket.getChannel().equals("MC|Brand")) {
                    newPacket = PluginMessageUtil.rewriteMCBrand(pm);
                }

                if (newPacket == pm) {
                    // we'll decrement this thrice: once when writing to the server, once just below this block,
                    // and once in the MinecraftConnection (since this is a slice)
                    pm.getData().retain();
                }
                connection.getProxyPlayer().getConnection().write(newPacket);
            } finally {
                ReferenceCountUtil.release(pm.getData());
            }
        } else {
            // Just forward the packet on. We don't have anything to handle at this time.
            if (packet instanceof ScoreboardTeam ||
                    packet instanceof ScoreboardObjective ||
                    packet instanceof ScoreboardSetScore ||
                    packet instanceof ScoreboardDisplay) {
                playerHandler.handleServerScoreboardPacket(packet);
            }
            connection.getProxyPlayer().getConnection().write(packet);
        }
    }

    @Override
    public void handleUnknown(ByteBuf buf) {
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
