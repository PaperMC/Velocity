package com.velocitypowered.proxy.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.Command;
import com.velocitypowered.api.command.CommandInvocation;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.InvocableCommand;
import com.velocitypowered.api.command.RawCommand;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.proxy.util.BrigadierUtils;

@FunctionalInterface
public interface CommandNodeFactory<T extends Command> {

  InvocableCommandNodeFactory<SimpleCommand.Invocation> SIMPLE =
      new InvocableCommandNodeFactory<SimpleCommand.Invocation>() {
        @Override
        protected SimpleCommand.Invocation createInvocation(
                final CommandContext<CommandSource> context) {
          return VelocitySimpleCommandInvocation.FACTORY.create(context);
        }
      };

  InvocableCommandNodeFactory<RawCommand.Invocation> RAW =
      new InvocableCommandNodeFactory<RawCommand.Invocation>() {
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
            return BrigadierCommand.FORWARD;
          }
          command.execute(source, args);
          return 1;
        },
        (context, builder) -> {
          String[] args = BrigadierUtils.getSplitArguments(context);
          if (!command.hasPermission(context.getSource(), args)) {
              return builder.buildFuture();
          }

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
              return BrigadierCommand.FORWARD;
            }
            command.execute(invocation);
            return 1;
          },
          (context, builder) -> {
            I invocation = createInvocation(context);

            if (!command.hasPermission(invocation)) {
                return builder.buildFuture();
            }
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
