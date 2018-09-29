package com.velocitypowered.api.proxy.messages;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Represents a Minecraft 1.13+ channel identifier. This class is immutable and safe for multi-threaded use.
 */
public final class MinecraftChannelIdentifier implements ChannelIdentifier {
    private static final Pattern VALID_IDENTIFIER_REGEX = Pattern.compile("[a-z0-9\\-_]+");

    private final String namespace;
    private final String name;

    private MinecraftChannelIdentifier(String namespace, String name) {
        this.namespace = namespace;
        this.name = name;
    }

    /**
     * Creates an identifier in the default namespace ({@code minecraft}). Plugins are strongly encouraged to provide
     * their own namespace.
     * @param name the name in the default namespace to use
     * @return a new channel identifier
     */
    public static MinecraftChannelIdentifier forDefaultNamespace(String name) {
        return new MinecraftChannelIdentifier("minecraft", name);
    }

    /**
     * Creates an identifier in the specified namespace.
     * @param namespace the namespace to use
     * @param name the channel name inside the specified namespace
     * @return a new channel identifier
     */
    public static MinecraftChannelIdentifier create(String namespace, String name) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(namespace), "namespace is null or empty");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(name), "namespace is null or empty");
        Preconditions.checkArgument(VALID_IDENTIFIER_REGEX.matcher(namespace).matches(), "namespace is not valid");
        Preconditions.checkArgument(VALID_IDENTIFIER_REGEX.matcher(name).matches(), "name is not valid");
        return new MinecraftChannelIdentifier(namespace, name);
    }

    public String getNamespace() {
        return namespace;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return getId() + " (modern)";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MinecraftChannelIdentifier that = (MinecraftChannelIdentifier) o;
        return Objects.equals(namespace, that.namespace) &&
                Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(namespace, name);
    }

    @Override
    public String getId() {
        return namespace + ":" + name;
    }
}
