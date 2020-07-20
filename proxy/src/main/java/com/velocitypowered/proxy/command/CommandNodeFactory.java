package com.velocitypowered.proxy.command;

import static com.velocitypowered.proxy.command.VelocityCommandManager.createRawArgsNode;
import static com.velocitypowered.proxy.command.VelocityLegacyCommandInvocation.split;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.*;

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

  CommandNodeFactory<Command> FALLBACK = new CommandNodeFactory<>() {

    @Override
    public LiteralCommandNode<CommandSource> create(final String alias, final Command command) {
      return createRawArgsNode(alias,
          context -> {
            CommandSource source = context.getSource();
            String[] args = parseArguments(context.getInput());

            if (!command.hasPermission(source, args)) {
              return VelocityCommandManager.NO_PERMISSION;
            }
            command.execute(source, args);
            return 1;
          },
          (context, builder) -> {
            CommandSource source = context.getSource();
            String[] args = parseArguments(context.getInput());

            return command.suggestAsync(source, args).thenApply(values -> {
              for (String value : values) {
                builder.suggest(value);
              }

              return builder.build();
            });
          });
    }

    private String[] parseArguments(final String cmdLine) {
      // This ugly parsing will be removed on Velocity 2.0,
      // see VelocityLegacyCommandInvocation for replacement.
      int firstSpace = cmdLine.indexOf(' ');
      if (firstSpace == -1) {
        return new String[0];
      }
      return split(cmdLine.substring(firstSpace + 1));
    }
  };

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
      return createRawArgsNode(alias,
          context -> {
            I invocation = createInvocation(context);
            if (!command.hasPermission(invocation)) {
              return VelocityCommandManager.NO_PERMISSION;
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
