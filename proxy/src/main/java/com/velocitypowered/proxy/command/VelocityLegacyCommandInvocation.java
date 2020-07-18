package com.velocitypowered.proxy.command;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.LegacyCommandInvocation;
import java.util.Arrays;
import org.checkerframework.checker.nullness.qual.NonNull;

final class VelocityLegacyCommandInvocation extends AbstractCommandInvocation
        implements LegacyCommandInvocation {

  static final CommandInvocationFactory<VelocityLegacyCommandInvocation> FACTORY =
          new Factory(false);

  static class Factory implements CommandInvocationFactory<VelocityLegacyCommandInvocation> {

    private boolean callDeprecatedMethods;

    public Factory(final boolean callDeprecatedMethods) {
      this.callDeprecatedMethods = callDeprecatedMethods;
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

    @Override
    public VelocityLegacyCommandInvocation create(final CommandSource source,
                                                  final String commandLine) {
      Preconditions.checkNotNull(source, "source");
      Preconditions.checkNotNull(commandLine, "line");
      final String[] arguments = split(commandLine);
      return new VelocityLegacyCommandInvocation(source, arguments, this.callDeprecatedMethods);
    }
  }

  private final String[] arguments;
  private final boolean callDeprecatedMethods;

  private VelocityLegacyCommandInvocation(final CommandSource source, final String[] arguments,
                                          final boolean callDeprecatedMethods) {
    super(source);
    this.arguments = arguments;
    this.callDeprecatedMethods = callDeprecatedMethods;
  }

  @Override
  public String @NonNull [] arguments() {
    return Arrays.copyOf(arguments, arguments.length);
  }

  public boolean shouldCallDeprecatedMethods() {
    return callDeprecatedMethods;
  }
}
