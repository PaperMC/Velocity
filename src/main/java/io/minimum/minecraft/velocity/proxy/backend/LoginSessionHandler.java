package io.minimum.minecraft.velocity.proxy.backend;

import io.minimum.minecraft.velocity.protocol.MinecraftPacket;
import io.minimum.minecraft.velocity.protocol.StateRegistry;
import io.minimum.minecraft.velocity.protocol.netty.MinecraftPipelineUtils;
import io.minimum.minecraft.velocity.protocol.packets.Disconnect;
import io.minimum.minecraft.velocity.protocol.packets.ServerLoginSuccess;
import io.minimum.minecraft.velocity.protocol.packets.SetCompression;
import io.minimum.minecraft.velocity.proxy.MinecraftSessionHandler;

public class LoginSessionHandler implements MinecraftSessionHandler {
    private final ServerConnection connection;

    public LoginSessionHandler(ServerConnection connection) {
        this.connection = connection;
    }

    @Override
    public void handle(MinecraftPacket packet) {
        if (packet instanceof Disconnect) {
            Disconnect disconnect = (Disconnect) packet;
            connection.disconnect();
            connection.getProxyPlayer().handleConnectionException(connection.getServerInfo(), disconnect);
        }

        if (packet instanceof SetCompression) {
            System.out.println("Enabling compression on server connection, this is inefficient!");
            SetCompression sc = (SetCompression) packet;
            connection.getChannel().setCompressionThreshold(sc.getThreshold());
        }

        if (packet instanceof ServerLoginSuccess) {
            // the player has been logged on.
            System.out.println("Player connected to remote server");
            connection.getChannel().setState(StateRegistry.PLAY);
            connection.getProxyPlayer().setConnectedServer(connection);
            connection.getProxyPlayer().getConnection().setSessionHandler(new io.minimum.minecraft.velocity.proxy.client.PlaySessionHandler(connection.getProxyPlayer()));
            connection.getChannel().setSessionHandler(new PlaySessionHandler(connection));
        }
    }
}
