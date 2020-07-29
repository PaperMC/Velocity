package com.velocitypowered.proxy.command.builtin;

import com.velocitypowered.api.command.RawCommand;
import com.velocitypowered.proxy.VelocityServer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class ShutdownCommand implements RawCommand {

  private final VelocityServer server;

  public ShutdownCommand(VelocityServer server) {
    this.server = server;
  }

  @Override
  public void execute(final Invocation invocation) {
    String reason = invocation.arguments();
    server.shutdown(true, LegacyComponentSerializer.legacy('&').deserialize(reason));
  }

  @Override
  public boolean hasPermission(final Invocation invocation) {
    return invocation.source() == server.getConsoleCommandSource();
  }
}
