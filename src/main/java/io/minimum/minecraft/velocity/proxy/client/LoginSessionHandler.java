package io.minimum.minecraft.velocity.proxy.client;

import com.google.common.base.Preconditions;
import io.minimum.minecraft.velocity.data.ServerInfo;
import io.minimum.minecraft.velocity.protocol.MinecraftPacket;
import io.minimum.minecraft.velocity.protocol.StateRegistry;
import io.minimum.minecraft.velocity.protocol.packets.ServerLogin;
import io.minimum.minecraft.velocity.protocol.packets.ServerLoginSuccess;
import io.minimum.minecraft.velocity.proxy.*;
import io.minimum.minecraft.velocity.proxy.backend.ServerConnection;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class LoginSessionHandler implements MinecraftSessionHandler {
    private final MinecraftConnection inbound;

    public LoginSessionHandler(MinecraftConnection inbound) {
        this.inbound = Preconditions.checkNotNull(inbound, "inbound");
    }

    @Override
    public void handle(MinecraftPacket packet) {
        Preconditions.checkArgument(packet instanceof ServerLogin, "Expected a ServerLogin packet, not " + packet.getClass().getName());

        // TODO: Encryption
        inbound.setCompressionThreshold(256);

        String username = ((ServerLogin) packet).getUsername();
        ServerLoginSuccess success = new ServerLoginSuccess();
        success.setUsername(username);
        success.setUuid(generateOfflinePlayerUuid(username));
        inbound.write(success);

        // Initiate a regular connection and move over to it.
        ConnectedPlayer player = new ConnectedPlayer(success.getUsername(), success.getUuid(), inbound);
        ServerInfo info = new ServerInfo("test", new InetSocketAddress("localhost", 25565));
        ServerConnection connection = new ServerConnection(info, player, VelocityServer.getServer());

        inbound.setState(StateRegistry.PLAY);
        inbound.setSessionHandler(new InitialConnectSessionHandler(player));
        connection.connect();
    }

    private static UUID generateOfflinePlayerUuid(String username) {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes(StandardCharsets.UTF_8));
    }
}
