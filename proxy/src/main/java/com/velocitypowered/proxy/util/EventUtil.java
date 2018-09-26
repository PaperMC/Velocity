package com.velocitypowered.proxy.util;

import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.packet.Chat;

import javax.annotation.Nullable;

public class EventUtil {

    public static void callPlayerChatEvent(VelocityServer server, ConnectedPlayer player, String message, Chat original){
        PlayerChatEvent event = new PlayerChatEvent(player, message);
        server.getEventManager().fire(event)
                .thenAcceptAsync(pme -> {
                    if (pme.getResult().equals(ResultedEvent.ChatResult.allowed())){
                        player.getConnectedServer().getMinecraftConnection().write(original);
                    } else if (pme.getResult().isAllowed() && pme.getResult().getMessage().isPresent()){
                        player.getConnectedServer().getMinecraftConnection().write(Chat.createServerbound(pme.getResult().getMessage().get()));
                    }
                }, player.getConnectedServer().getMinecraftConnection().getChannel().eventLoop());
    }

}
