package com.velocitypowered.proxy.command;

import com.velocitypowered.api.command.Command;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.proxy.VelocityServer;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import org.checkerframework.checker.nullness.qual.NonNull;

public class ShutdownCommand implements Command {

  private final VelocityServer server;

  public ShutdownCommand(VelocityServer server) {
    this.server = server;
  }

  @Override
  public void execute(CommandSource source, String @NonNull [] args) {
    if (source != server.getConsoleCommandSource()) {
      source
          .sendMessage(TextComponent.of("You are not allowed to use this command.", TextColor.RED));
      return;
    }
    server.shutdown();
  }

  @Override
  public boolean hasPermission(CommandSource source, String @NonNull [] args) {
    return source == server.getConsoleCommandSource();
  }
}
