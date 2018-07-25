package io.minimum.minecraft.velocity.proxy.backend;

import io.minimum.minecraft.velocity.protocol.MinecraftPacket;
import io.minimum.minecraft.velocity.protocol.packets.Disconnect;
import io.minimum.minecraft.velocity.protocol.packets.Ping;
import io.minimum.minecraft.velocity.proxy.MinecraftSessionHandler;
import io.netty.buffer.ByteBuf;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import net.kyori.text.serializer.ComponentSerializers;

public class PlaySessionHandler implements MinecraftSessionHandler {
    private final ServerConnection connection;

    public PlaySessionHandler(ServerConnection connection) {
        this.connection = connection;
    }

    @Override
    public void handle(MinecraftPacket packet) {
        if (packet instanceof Ping) {
            // Make sure to reply back to the server so it doesn't think we're gone.
            connection.getChannel().write(packet);
            connection.getProxyPlayer().getConnection().write(packet);
        } else if (packet instanceof Disconnect) {
            // The server wants to disconnect us. TODO fallback handling
            Disconnect original = (Disconnect) packet;
            TextComponent reason = TextComponent.builder()
                    .content("Disconnected from " + connection.getServerInfo().getName() + ":")
                    .color(TextColor.RED)
                    .append(TextComponent.of(" ", TextColor.WHITE))
                    .append(ComponentSerializers.JSON.deserialize(original.getReason()))
                    .build();
            connection.getProxyPlayer().close(reason);
        } else {
            // Just forward the packet on. We don't have anything to handle at this time.
            connection.getProxyPlayer().getConnection().write(packet);
        }
    }

    @Override
    public void handleUnknown(ByteBuf buf) {
        connection.getProxyPlayer().getConnection().write(buf.retain());
    }
}
