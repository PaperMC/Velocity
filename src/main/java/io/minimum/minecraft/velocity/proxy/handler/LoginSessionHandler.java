package io.minimum.minecraft.velocity.proxy.handler;

import com.google.common.base.Preconditions;
import io.minimum.minecraft.velocity.protocol.MinecraftPacket;
import io.minimum.minecraft.velocity.protocol.packets.ServerLogin;
import io.minimum.minecraft.velocity.protocol.packets.ServerLoginSuccess;
import io.minimum.minecraft.velocity.proxy.InboundMinecraftConnection;
import io.minimum.minecraft.velocity.proxy.MinecraftSessionHandler;

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

        String username = ((ServerLogin) packet).getUsername();
        ServerLoginSuccess success = new ServerLoginSuccess();
        success.setUsername(username);
        success.setUuid(generateOfflinePlayerUuid(username));

        connection.write(success);
    }

    private static UUID generateOfflinePlayerUuid(String username) {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes(StandardCharsets.UTF_8));
    }
}
