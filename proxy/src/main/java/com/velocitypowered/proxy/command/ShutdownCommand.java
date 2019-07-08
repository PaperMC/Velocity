package com.velocitypowered.proxy.command;

import com.velocitypowered.api.command.Command;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.proxy.VelocityServer;
import org.checkerframework.checker.nullness.qual.NonNull;

public class ShutdownCommand implements Command {

  private final VelocityServer server;

  public ShutdownCommand(VelocityServer server) {
    this.server = server;
  }

  @Override
  public void execute(CommandSource source, String @NonNull [] args) {
    server.shutdown(true);
  }

  @Override
  public boolean hasPermission(CommandSource source, String @NonNull [] args) {
    return source == server.getConsoleCommandSource();
  }
}
