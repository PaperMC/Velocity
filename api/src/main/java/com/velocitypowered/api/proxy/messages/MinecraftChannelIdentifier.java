/*
 * Copyright (C) 2018-2023 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.proxy.messages;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.Strings;
import java.util.Objects;
import net.kyori.adventure.key.Key;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Represents a Minecraft 1.13+ channel identifier. This class is immutable and safe for
 * multi-threaded use.
 */
public final class MinecraftChannelIdentifier implements ChannelIdentifier {

  private final String namespace;
  private final String name;

  private MinecraftChannelIdentifier(String namespace, String name) {
    this.namespace = namespace;
    this.name = name;
  }

  /**
   * Creates an identifier in the default namespace ({@code minecraft}). Plugins are strongly
   * encouraged to provide their own namespace.
   *
   * @param name the name in the default namespace to use
   * @return a new channel identifier
   */
  public static MinecraftChannelIdentifier forDefaultNamespace(String name) {
    return new MinecraftChannelIdentifier(Key.MINECRAFT_NAMESPACE, name);
  }

  /**
   * Creates an identifier in the specified namespace.
   *
   * @param namespace the namespace to use
   * @param name the channel name inside the specified namespace
   * @return a new channel identifier
   */
  public static MinecraftChannelIdentifier create(String namespace, String name) {
    checkArgument(!Strings.isNullOrEmpty(namespace), "namespace is null or empty");
    checkArgument(name != null, "namespace is null or empty");
    checkArgument(Key.parseableNamespace(namespace),
        "namespace is not valid, must match: [a-z0-9_.-] got %s", namespace);
    checkArgument(Key.parseableValue(name),
        "name is not valid, must match: [a-z0-9/._-] got %s", name);
    return new MinecraftChannelIdentifier(namespace, name);
  }

  /**
   * Creates an channel identifier from the specified Minecraft identifier.
   *
   * @param identifier the Minecraft identifier
   * @return a new channel identifier
   */
  public static MinecraftChannelIdentifier from(String identifier) {
    int colonPos = identifier.indexOf(':');
    if (colonPos == -1) {
      return create(Key.MINECRAFT_NAMESPACE, identifier);
    } else if (colonPos == 0) {
      return create(Key.MINECRAFT_NAMESPACE, identifier.substring(1));
    }
    String namespace = identifier.substring(0, colonPos);
    String name = identifier.substring(colonPos + 1);
    return create(namespace, name);
  }

  /**
   * Creates an channel identifier from the specified Minecraft identifier.
   *
   * @param key the Minecraft key to use
   * @return a new channel identifier
   */
  public static MinecraftChannelIdentifier from(Key key) {
    return create(key.namespace(), key.value());
  }

  /**
   * Returns the namespace of this channel identifier.
   *
   * @return the namespace string (e.g., {@code minecraft})
   */
  public String getNamespace() {
    return namespace;
  }

  /**
   * Returns the name of the channel within its namespace.
   *
   * @return the channel name string
   */
  public String getName() {
    return name;
  }

  /**
   * Converts this channel identifier to a {@link Key} object.
   *
   * @return a {@link Key} representing this identifier
   */
  public Key asKey() {
    return Key.key(namespace, name);
  }

  @Override
  public String toString() {
    return namespace + ":" + name + " (modern)";
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MinecraftChannelIdentifier that = (MinecraftChannelIdentifier) o;
    return Objects.equals(namespace, that.namespace)
        && Objects.equals(name, that.name);
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
