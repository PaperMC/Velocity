package com.velocitypowered.api.command;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.ArgumentCommandNode;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

/**
 * A Brigadier {@link ArgumentType} recognized by a Minecraft client, but
 * that provides no parsing nor suggestion provision logic in the proxy.
 * <p>
 * This class is useful when a plugin wants the client to parse the contents and
 * provide suggestions for an {@link ArgumentCommandNode} according to one of
 * the built-in argument parsers. The following example re-constructs the
 * {@code /give} command node:
 * <pre>
 * CommandManager commandManager = ...;
 * OpaqueArgumentType itemType = commandManager.getOpaqueArgumentType(Key.key("item_stack"));
 * final LiteralCommandNode<CommandSource> literal = LiteralArgumentBuilder
 *         .<CommandSource>literal("give")
 *         .then(argument("item", itemType))
 *         .build();
 * </pre>
 * <p>
 * The execution of an argument node of this type is automatically forwarded
 * to the backend server. Thus, any {@link com.mojang.brigadier.Command},
 * predicate, redirection, or {@link SuggestionProvider} on the corresponding
 * node is ignored.
 * <p>
 * Executing any methods from the {@link ArgumentType} interface on this class
 * result in a {@link UnsupportedOperationException} being thrown.
 */
public interface OpaqueArgumentType extends ArgumentType<Void> {

  // We don't provide a way to retrieve the identifiers, since these are version-dependent
  // and their type has changed from a string to a numerical value in Minecraft 1.19.
  // This prevents API breakage if Mojang were to change their format yet again.

  @Override
  default Void parse(StringReader reader) throws CommandSyntaxException {
    throw new UnsupportedOperationException("Opaque type doesn't support argument parsing");
  }

  @Override
  default <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
    throw new UnsupportedOperationException("Opaque type doesn't support suggestion provision");
  }

  @Override
  default Collection<String> getExamples() {
    throw new UnsupportedOperationException("Opaque type doesn't have any examples");
  }
}
