package com.velocitypowered.api.command;

import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.proxy.Player;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.checkerframework.checker.nullness.qual.NonNull;

// TODO Review javadoc
/**
 * Represents a command that can be executed by a {@link CommandSource}, such as
 * a {@link Player} or the console.
 *
 * <p>Velocity 1.1.0 introduces specialized command subinterfaces to separate
 * command parsing concerns. These include, in order of preference:
 *
 * <ul>
 * <li>{@link BrigadierCommand}, which supports parameterized arguments and
 * specialized execution, tab complete suggestions and permission-checking logic.
 *
 * <li>{@link LegacyCommand}, modelled after the convention popularized by
 * Bukkit and BungeeCord. Older classes directly implementing {@link Command}
 * are suggested to migrate to this interface.
 *
 * <li>{@link RawCommand}, useful for bolting on external command frameworks
 * to Velocity.
 *
 * </ul>
 *
 * For this reason, the legacy {@code execute()}, {@code suggest()},
 * and {@code hasPermission()} methods are deprecated and will be removed in Velocity 2.0.0.
 * We suggest implementing one of the more specific subinterfaces instead.
 * The legacy methods are only executed by a {@link CommandManager} if
 * the given command <b>directly</b> implements this interface.
 */
public interface Command {

  /**
   * Executes the command for the specified source.
   *
   * @param source the source to execute the command for
   * @param args the arguments for the command
   * @deprecated see {@link Command}
   */
  @Deprecated
  default void execute(final CommandSource source, final String @NonNull [] args) {
    throw new UnsupportedOperationException();
  }

  /**
   * Provides tab complete suggestions for the specified source.
   *
   * @param source the source to execute the command for
   * @param currentArgs the partial arguments for the command
   * @return the tab complete suggestions
   * @deprecated see {@link Command}
   */
  @Deprecated
  default List<String> suggest(final CommandSource source, final String @NonNull [] currentArgs) {
    return ImmutableList.of();
  }

  /**
   * Provides tab complete suggestions for the specified source.
   *
   * @param source the source to execute the command for
   * @param currentArgs the partial arguments for the command
   * @return the tab complete suggestions
   * @deprecated see {@link Command}
   */
  @Deprecated
  default CompletableFuture<List<String>> suggestAsync(final CommandSource source,
                                                       String @NonNull [] currentArgs) {
    return CompletableFuture.completedFuture(suggest(source, currentArgs));
  }

  /**
   * Tests to check if the source has permission to perform the command with
   * the provided arguments.
   *
   * @param source the source to execute the command for
   * @param args the arguments for the command
   * @return {@code true} if the source has permission
   * @deprecated see {@link Command}
   */
  @Deprecated
  default boolean hasPermission(final CommandSource source, final String @NonNull [] args) {
    return true;
  }

  /**
   * Provides a fluent interface to register a command.
   *
   * @param <T> the type of the built command
   * @param <B> the type of this builder
   */
  // TODO Is this useful to API users? Velocity only uses it for building Brigadier commands
  // See https://community.oracle.com/blogs/emcmanus/2010/10/24/using-builder-pattern-subclasses
  interface Builder<T, B extends Builder<T, B>> {

    /**
     * Specifies additional aliases that can be used to execute the command.
     *
     * @param aliases the command aliases
     * @return this builder, for chaining
     */
    B aliases(String... aliases);
  }
}
