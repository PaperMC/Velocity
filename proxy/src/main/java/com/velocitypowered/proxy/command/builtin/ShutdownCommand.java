package com.velocitypowered.proxy.command.builtin;

import com.velocitypowered.api.command.LegacyCommand;
import com.velocitypowered.proxy.VelocityServer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class ShutdownCommand implements LegacyCommand {

  private final VelocityServer server;

  public ShutdownCommand(VelocityServer server) {
    this.server = server;
  }

  @Override
  public void execute(final LegacyCommand.Invocation invocation) {
    final String[] args = invocation.arguments();
    if (args.length == 0) {
      server.shutdown(true);
    } else {
      String reason = String.join(" ", args);
      server.shutdown(true, LegacyComponentSerializer.legacy('&').deserialize(reason));
    }
  }

  @Override
  public boolean hasPermission(final LegacyCommand.Invocation invocation) {
    return invocation.source() == server.getConsoleCommandSource();
  }
}
