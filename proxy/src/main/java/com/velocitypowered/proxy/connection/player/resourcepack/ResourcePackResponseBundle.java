package com.velocitypowered.proxy.connection.player.resourcepack;

import com.velocitypowered.api.event.player.PlayerResourcePackStatusEvent;
import java.util.UUID;

@SuppressWarnings("checkstyle:MissingJavadocType")
public record ResourcePackResponseBundle(UUID uuid, PlayerResourcePackStatusEvent.Status status) {
}
