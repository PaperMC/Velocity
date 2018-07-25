package io.minimum.minecraft.velocity.proxy.handler;

import com.google.common.base.Preconditions;
import io.minimum.minecraft.velocity.data.ServerInfo;
import io.minimum.minecraft.velocity.protocol.MinecraftPacket;
import io.minimum.minecraft.velocity.protocol.StateRegistry;
import io.minimum.minecraft.velocity.protocol.packets.ServerLogin;
import io.minimum.minecraft.velocity.protocol.packets.ServerLoginSuccess;
import io.minimum.minecraft.velocity.proxy.*;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class LoginSessionHandler implements MinecraftSessionHandler {
    private final InboundMinecraftConnection connection;

    public LoginSessionHandler(InboundMinecraftConnection connection) {
        this.connection = Preconditions.checkNotNull(connection, "connection");
    }

    @Override
    public void handle(MinecraftPacket packet) {
        Preconditions.checkArgument(packet instanceof ServerLogin, "Expected a ServerLogin packet, not " + packet.getClass().getName());

        // TODO: Encryption and compression
        String username = ((ServerLogin) packet).getUsername();
        ServerLoginSuccess success = new ServerLoginSuccess();
        success.setUsername(username);
        success.setUuid(generateOfflinePlayerUuid(username));
        connection.write(success);

        connection.setStatus(StateRegistry.PLAY);

        ConnectedPlayer player = new ConnectedPlayer(username, generateOfflinePlayerUuid(username), connection);

        ServerInfo info = new ServerInfo("test", new InetSocketAddress("127.0.0.1", 25565));
        ServerConnection connection = new ServerConnection(info, player, VelocityServer.getServer());
        connection.connect();
    }

    private static UUID generateOfflinePlayerUuid(String username) {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes(StandardCharsets.UTF_8));
    }
}
