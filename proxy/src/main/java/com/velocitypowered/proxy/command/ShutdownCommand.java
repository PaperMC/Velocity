package com.velocitypowered.proxy.command;

import com.velocitypowered.api.command.Command;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.proxy.VelocityServer;
import net.kyori.text.serializer.legacy.LegacyComponentSerializer;
import org.checkerframework.checker.nullness.qual.NonNull;

public class ShutdownCommand implements Command {

  private final VelocityServer proxy;

  public ShutdownCommand(VelocityServer proxy) {
    this.proxy = proxy;
  }

  @Override
  public void execute(CommandSource source, String @NonNull [] args) {
    if (args.length == 0) {
      proxy.shutdown(true);
    } else {
      String reason = String.join(" ", args);
      proxy.shutdown(true, LegacyComponentSerializer.legacy().deserialize(reason, '&'));
    }
  }

  @Override
  public boolean hasPermission(CommandSource source, String @NonNull [] args) {
    return source == proxy.getConsoleCommandSource();
  }
}
