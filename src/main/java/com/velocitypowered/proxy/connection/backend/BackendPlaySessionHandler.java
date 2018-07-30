package com.velocitypowered.proxy.connection.backend;

import com.velocitypowered.proxy.connection.client.ClientPlaySessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.packets.*;
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
            connection.getChannel().write(packet);
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
                if (!canForwardMessage(newPacket)) {
                    return;
                }

                if (newPacket.getChannel().equals("MC|Brand")) {
                    newPacket = PluginMessageUtil.rewriteMCBrand(pm);
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
        connection.getProxyPlayer().getConnection().write(buf.retain());
    }

    @Override
    public void exception(Throwable throwable) {
        connection.getProxyPlayer().handleConnectionException(connection.getServerInfo(), throwable);
    }

    private boolean canForwardMessage(PluginMessage message) {
        // TODO: Update for 1.13
        ClientPlaySessionHandler playerHandler =
                (ClientPlaySessionHandler) connection.getProxyPlayer().getConnection().getSessionHandler();
        return message.getChannel().startsWith("MC|") ||
                message.getChannel().startsWith("FML") ||
                message.getChannel().equals("FORGE") ||
                playerHandler.getClientPluginMsgChannels().contains(message.getChannel());
    }
}
