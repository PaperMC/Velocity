package com.velocitypowered.proxy.command;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.LegacyCommandInvocation;
import java.util.Arrays;
import org.checkerframework.checker.nullness.qual.NonNull;

final class VelocityLegacyCommandInvocation extends AbstractCommandInvocation implements LegacyCommandInvocation {

  static final CommandInvocationFactory<LegacyCommandInvocation> FACTORY = new Factory();

  private static class Factory implements CommandInvocationFactory<LegacyCommandInvocation> {

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

    @Override
    public LegacyCommandInvocation create(final CommandSource source, final String commandLine) {
      Preconditions.checkNotNull(source, "source");
      Preconditions.checkNotNull(commandLine, "line");
      final String[] arguments = split(commandLine);
      return new VelocityLegacyCommandInvocation(source, arguments);
    }
  }

  private final String[] arguments;

  private VelocityLegacyCommandInvocation(final CommandSource source, final String[] arguments) {
    super(source);
    this.arguments = arguments;
  }

  @Override
  public String @NonNull [] arguments() {
    return Arrays.copyOf(arguments, arguments.length);
  }
}
