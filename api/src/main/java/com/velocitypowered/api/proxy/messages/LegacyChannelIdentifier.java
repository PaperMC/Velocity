package com.velocitypowered.api.proxy.messages;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import java.util.Objects;

/**
 * Reperesents a legacy channel identifier (for Minecraft 1.12 and below). For modern 1.13 plugin messages, please see
 * {@link MinecraftChannelIdentifier}.
 */
public class LegacyChannelIdentifier implements ChannelIdentifier {
    private final String name;

    public LegacyChannelIdentifier(String name) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(name), "provided name is empty");
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "LegacyChannelIdentifier{" +
                "name='" + name + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LegacyChannelIdentifier that = (LegacyChannelIdentifier) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
