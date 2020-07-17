package com.velocitypowered.proxy.command;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.LegacyCommandExecutionContext;
import java.util.Arrays;
import org.checkerframework.checker.nullness.qual.NonNull;

final class VelocityLegacyCommandExecutionContext extends AbstractCommandExecutionContext
        implements LegacyCommandExecutionContext {

  static final CommandExecutionContextFactory<LegacyCommandExecutionContext> FACTORY = new Factory();

  private static class Factory implements CommandExecutionContextFactory<LegacyCommandExecutionContext> {

    @Override
    public LegacyCommandExecutionContext createContext(final CommandSource source, final String commandLine) {
      Preconditions.checkNotNull(source, "source");
      Preconditions.checkNotNull(commandLine, "line");

      final String[] arguments = split(commandLine);
      return new VelocityLegacyCommandExecutionContext(source, arguments);
    }
  }

  private static String[] split(final String line) {
    if (line.isEmpty()) {
      return new String[0];
    }

    String[] trimmed = line.trim().split(" ", -1);
    if (line.endsWith(" ") && !line.trim().isEmpty()) {
      // To work around a 1.13+ specific bug we have to inject a space at the end of the arguments
      trimmed = Arrays.copyOf(trimmed, trimmed.length + 1);
      trimmed[trimmed.length - 1] = "";
    }
    return trimmed;
  }

  private final String[] arguments;

  private VelocityLegacyCommandExecutionContext(final CommandSource source, final String[] arguments) {
    super(source);
    this.arguments = arguments;
  }

  @Override
  public String @NonNull [] arguments() {
    // TODO Defensive copy?
    return arguments;
  }
}
