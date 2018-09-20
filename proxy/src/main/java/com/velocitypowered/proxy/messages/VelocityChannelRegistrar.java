package com.velocitypowered.proxy.messages;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.ChannelRegistrar;
import com.velocitypowered.api.proxy.messages.LegacyChannelIdentifier;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class VelocityChannelRegistrar implements ChannelRegistrar {
    private final Map<String, ChannelIdentifier> identifierMap = new ConcurrentHashMap<>();

    @Override
    public void register(ChannelIdentifier... identifiers) {
        for (ChannelIdentifier identifier : identifiers) {
            Preconditions.checkArgument(identifier instanceof LegacyChannelIdentifier || identifier instanceof MinecraftChannelIdentifier,
                    "identifier is unknown");
        }

        for (ChannelIdentifier identifier : identifiers) {
            identifierMap.put(identifier.getId(), identifier);
        }
    }

    @Override
    public void unregister(ChannelIdentifier... identifiers) {
        for (ChannelIdentifier identifier : identifiers) {
            Preconditions.checkArgument(identifier instanceof LegacyChannelIdentifier || identifier instanceof MinecraftChannelIdentifier,
                    "identifier is unknown");
        }

        for (ChannelIdentifier identifier : identifiers) {
            identifierMap.remove(identifier.getId());
        }
    }

    public Collection<String> getIdsForLegacyConnections() {
        return ImmutableList.copyOf(identifierMap.keySet());
    }

    public Collection<String> getModernChannelIds() {
        return identifierMap.values().stream()
                .filter(i -> i instanceof MinecraftChannelIdentifier)
                .map(ChannelIdentifier::getId)
                .collect(Collectors.toList());
    }

    public boolean registered(String id) {
        return identifierMap.containsKey(id);
    }

    public ChannelIdentifier getFromId(String id) {
        return identifierMap.get(id);
    }
}
