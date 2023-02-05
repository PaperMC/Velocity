/*
 * Copyright (C) 2023 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.command;

import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.velocitypowered.api.network.ProtocolVersion;
import net.kyori.adventure.key.Key;

/**
 * A Brigadier {@link ArgumentType} recognized by a Minecraft client, but
 * that provides no parsing nor suggestion provision logic in the proxy.
 *
 * <p>This class is useful when a plugin wants the client to parse the contents and
 * provide suggestions for an {@link ArgumentCommandNode} according to one of
 * the built-in argument parsers. The following example constructs a simplified
 * version of the {@code /give} command:
 * <pre>
 * CommandManager commandManager = ...;
 * OpaqueArgumentType itemType = commandManager.opaqueArgumentTypeBuilder(Key.key("item_stack"))
 *                                             .build();
 * final LiteralCommandNode&lt;CommandSource&gt; literal = LiteralArgumentBuilder
 *         .&lt;CommandSource&gt;literal("give")
 *         .then(argument("item", itemType))
 *         .build();
 * </pre>
 *
 * <p>The execution of an {@link ArgumentCommandNode} of this type is automatically
 * forwarded to the backend server. Thus, any {@link com.mojang.brigadier.Command},
 * predicate, or {@link SuggestionProvider} on the corresponding node is ignored.
 *
 * <p>Parsing of a command by Brigadier ends whenever a node with an opaque type is
 * encountered. For this reason, any {@link ParseResults} containing an argument
 * node with an opaque type may contain inaccurate data. This is a compromise
 * that must be made because the proxy does not know how to parse these types.
 *
 * <p>This type provides no suggestions nor examples. However, the client can often
 * provide rich suggestions for the represented argument type.
 *
 * <p>The identifier and serialized form of the parser properties may change between
 * different {@link ProtocolVersion protocol versions} of the game. As so, we
 * recommend using a library that provides {@link OpaqueArgumentType} maintained
 * definitions for the parsers implemented by the client, instead of manually creating
 * them through a {@link OpaqueArgumentType.Builder}. Velocity cannot include these
 * definitions in the API, since Minecraft updates might cause breaking changes.
 * This is not a hypothetical case: the {@code minecraft:mob_effect} parser was
 * removed in {@link ProtocolVersion#MINECRAFT_1_19_3}.
 *
 * @see CommandManager#opaqueArgumentTypeBuilder(Key) to construct an argument type from
 *        its string identifier.
 */
public interface OpaqueArgumentType extends ArgumentType<Void> {

  /**
   * Returns the argument parser identifier.
   *
   * @return the string identifier.
   */
  String getIdentifier();

  /**
   * Returns the parser properties in serialized form, following the protocol
   * at the given version.
   *
   * @param version the protocol version to use for serialization
   * @return the serialized argument parser properties.
   * @see <a href="https://wiki.vg/Command_Data#Properties">Properties specification</a>
   */
  byte[] getProperties(ProtocolVersion version);

  /**
   * Serializes the properties of an argument parser.
   *
   * @see <a href="https://wiki.vg/Command_Data#Properties">Properties specification</a>
   */
  interface PropertySerializer {
    /**
     * Serializes the properties of an argument parser, following the protocol
     * at the given version.
     *
     * @param version the protocol version to use for serialization
     * @return the serialized argument parser properties.
     */
    byte[] serialize(final ProtocolVersion version);
  }

  /**
   * Provides a fluent interface to create {@link OpaqueArgumentType opaque argument types}.
   */
  interface Builder {
    /**
     * Specifies the instance to use when serializing the parser properties of
     * the constructed argument type.
     *
     * @param serializer the properties serializer.
     * @return this builder, for chaining.
     */
    Builder withProperties(PropertySerializer serializer);

    /**
     * Specifies the parser properties in serialized form of the constructed
     * argument type. The serialized form of these properties generally depends
     * on the protocol version; this is a helper method when the wire format
     * is the same for all known {@link ProtocolVersion}.
     *
     * @param data the serialized form of the parser properties
     * @return this builder, for chaining.
     */
    default Builder withProperties(final byte[] data) {
      return withProperties(version -> data);
    }

    /**
     * Returns a newly-created {@link OpaqueArgumentType} based on the specified
     * identifier and properties. If no properties have been specified, then
     * a placeholder {@link PropertySerializer} whose
     * {@link PropertySerializer#serialize(ProtocolVersion)} method always returns
     * an empty byte array is used.
     *
     * @return the built opaque argument type.
     */
    OpaqueArgumentType build();
  }
}
