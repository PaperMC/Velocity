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
 * OpaqueArgumentType itemType = commandManager.getOpaqueArgumentType(Key.key("item_stack"));
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
 * @see CommandManager#getOpaqueArgumentType(Key) to obtain an argument type by
 *        its string identifier (used in Minecraft 1.18 and below).
 * @see CommandManager#getOpaqueArgumentType(ProtocolVersion, int) to obtain an argument
 *        type by its version-dependent numeric identifier (used in Minecraft 1.19 and above).
 */
public interface OpaqueArgumentType extends ArgumentType<Void> {
  // We don't provide a way to retrieve the identifiers, since these are version-dependent
  // and their type has changed from a string to a numerical value in Minecraft 1.19.
  // This prevents API breakage if Mojang were to change their format yet again.
}
