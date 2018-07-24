package io.minimum.minecraft.velocity.proxy.handler;

import com.google.common.base.Preconditions;
import io.minimum.minecraft.velocity.protocol.MinecraftPacket;
import io.minimum.minecraft.velocity.protocol.packets.Disconnect;
import io.minimum.minecraft.velocity.protocol.packets.ServerLogin;
import io.minimum.minecraft.velocity.proxy.InboundMinecraftConnection;
import io.minimum.minecraft.velocity.proxy.MinecraftSessionHandler;
import net.kyori.text.TextComponent;
import net.kyori.text.serializer.ComponentSerializers;

public class LoginSessionHandler implements MinecraftSessionHandler {
    private final InboundMinecraftConnection connection;

    public LoginSessionHandler(InboundMinecraftConnection connection) {
        this.connection = Preconditions.checkNotNull(connection, "connection");
    }

    @Override
    public void handle(MinecraftPacket packet) {
        Preconditions.checkArgument(packet instanceof ServerLogin, "Expected a ServerLogin packet, not " + packet.getClass().getName());

        // Disconnect with test message
        Disconnect disconnect = new Disconnect();
        disconnect.setReason(ComponentSerializers.JSON.serialize(TextComponent.of("Hi there!")));
        connection.closeWith(disconnect);
    }
}
