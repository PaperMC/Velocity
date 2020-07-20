package com.velocitypowered.proxy.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.Command;
import com.velocitypowered.api.command.CommandInvocation;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.InvocableCommand;
import com.velocitypowered.api.command.LegacyCommand;
import com.velocitypowered.api.command.RawCommand;
import com.velocitypowered.proxy.util.BrigadierUtils;

@FunctionalInterface
public interface CommandNodeFactory<T extends Command> {

  InvocableCommandNodeFactory<LegacyCommand.Invocation> LEGACY =
      new InvocableCommandNodeFactory<>() {
        @Override
        protected LegacyCommand.Invocation createInvocation(
                final CommandContext<CommandSource> context) {
          return VelocityLegacyCommandInvocation.FACTORY.create(context);
        }
      };

  InvocableCommandNodeFactory<RawCommand.Invocation> RAW =
      new InvocableCommandNodeFactory<>() {
        @Override
        protected RawCommand.Invocation createInvocation(
                final CommandContext<CommandSource> context) {
          return VelocityRawCommandInvocation.FACTORY.create(context);
        }
      };

  CommandNodeFactory<Command> FALLBACK = (alias, command) ->
      BrigadierUtils.buildRawArgumentsLiteral(alias,
        context -> {
          CommandSource source = context.getSource();
          String[] args = BrigadierUtils.getSplitArguments(context);

          if (!command.hasPermission(source, args)) {
            return BrigadierUtils.NO_PERMISSION;
          }
          command.execute(source, args);
          return 1;
        },
        (context, builder) -> {
          String[] args = BrigadierUtils.getSplitArguments(context);
          return command.suggestAsync(context.getSource(), args).thenApply(values -> {
            for (String value : values) {
              builder.suggest(value);
            }

            return builder.build();
          });
        });

  /**
   * Returns a Brigadier node for the execution of the given command.
   *
   * @param alias the command alias
   * @param command the command to execute
   * @return the command node
   */
  LiteralCommandNode<CommandSource> create(String alias, T command);

  abstract class InvocableCommandNodeFactory<I extends CommandInvocation<?>>
          implements CommandNodeFactory<InvocableCommand<I>> {

    @Override
    public LiteralCommandNode<CommandSource> create(
            final String alias, final InvocableCommand<I> command) {
      return BrigadierUtils.buildRawArgumentsLiteral(alias,
          context -> {
            I invocation = createInvocation(context);
            if (!command.hasPermission(invocation)) {
              return BrigadierUtils.NO_PERMISSION;
            }
            command.execute(invocation);
            return 1;
          },
          (context, builder) -> {
            I invocation = createInvocation(context);

            return command.suggestAsync(invocation).thenApply(values -> {
              for (String value : values) {
                builder.suggest(value);
              }

              return builder.build();
            });
          });
    }

    protected abstract I createInvocation(final CommandContext<CommandSource> context);
  }
}
