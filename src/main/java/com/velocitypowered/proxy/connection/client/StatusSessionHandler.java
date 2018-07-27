package com.velocitypowered.proxy.connection.client;

import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.packets.Ping;
import com.velocitypowered.proxy.protocol.packets.StatusRequest;
import com.velocitypowered.proxy.protocol.packets.StatusResponse;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.data.ServerPing;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.serializer.GsonComponentSerializer;

public class StatusSessionHandler implements MinecraftSessionHandler {
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
        ServerPing ping = new ServerPing(
                new ServerPing.Version(340, "1.12.2"),
                new ServerPing.Players(0, 0),
                TextComponent.of("test"),
                null
        );
        StatusResponse response = new StatusResponse();
        response.setStatus(VelocityServer.GSON.toJson(ping));
        connection.write(response);
    }
}
