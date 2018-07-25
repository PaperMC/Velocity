package io.minimum.minecraft.velocity.proxy.client;

import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.minimum.minecraft.velocity.data.ServerPing;
import io.minimum.minecraft.velocity.protocol.MinecraftPacket;
import io.minimum.minecraft.velocity.protocol.packets.Ping;
import io.minimum.minecraft.velocity.protocol.packets.StatusRequest;
import io.minimum.minecraft.velocity.protocol.packets.StatusResponse;
import io.minimum.minecraft.velocity.proxy.MinecraftConnection;
import io.minimum.minecraft.velocity.proxy.MinecraftSessionHandler;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.serializer.GsonComponentSerializer;

public class StatusSessionHandler implements MinecraftSessionHandler {
    private static final Gson GSON = new GsonBuilder()
            .registerTypeHierarchyAdapter(Component.class, new GsonComponentSerializer())
            .create();
    private final MinecraftConnection connection;

    public StatusSessionHandler(MinecraftConnection connection) {
        this.connection = connection;
    }

    @Override
    public void handle(MinecraftPacket packet) {
        Preconditions.checkArgument(packet instanceof Ping || packet instanceof StatusRequest,
                "Unrecognized packet type " + packet.getClass().getName());

        if (packet instanceof Ping) {
            // Just send back the client's packet, no processing to do here.
            connection.closeWith(packet);
            return;
        }

        // Status request
        System.out.println("Got status request!");
        ServerPing ping = new ServerPing(
                new ServerPing.Version(340, "1.12.2"),
                new ServerPing.Players(0, 0),
                TextComponent.of("test"),
                null
        );
        StatusResponse response = new StatusResponse();
        response.setStatus(GSON.toJson(ping));
        connection.write(response);
    }
}
