package com.velocitypowered.api.proxy.messages;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import java.util.Objects;
import java.util.regex.Pattern;
import net.kyori.minecraft.Key;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Represents a Minecraft 1.13+ channel identifier. This class is immutable and safe for
 * multi-threaded use.
 */
public final class MinecraftChannelIdentifier implements ChannelIdentifier {

  private static final Pattern VALID_IDENTIFIER_REGEX = Pattern.compile("[a-z0-9/\\-_]*");

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
    return new MinecraftChannelIdentifier("minecraft", name);
  }

  /**
   * Creates an identifier in the specified namespace.
   *
   * @param namespace the namespace to use
   * @param name the channel name inside the specified namespace
   * @return a new channel identifier
   */
  public static MinecraftChannelIdentifier create(String namespace, String name) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(namespace), "namespace is null or empty");
    Preconditions.checkArgument(name != null, "namespace is null or empty");
    Preconditions.checkArgument(VALID_IDENTIFIER_REGEX.matcher(namespace).matches(),
        "namespace is not valid");
    Preconditions
        .checkArgument(VALID_IDENTIFIER_REGEX.matcher(name).matches(), "name is not valid");
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
      throw new IllegalArgumentException("Identifier does not contain a colon.");
    }
    if (colonPos + 1 == identifier.length()) {
      throw new IllegalArgumentException("Identifier is empty.");
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

  public String getNamespace() {
    return namespace;
  }

  public String getName() {
    return name;
  }

  public Key asKey() {
    return Key.of(namespace, name);
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
