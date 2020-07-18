package com.velocitypowered.proxy.command;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.permission.Tristate;
import net.kyori.text.Component;

public class MockCommandSource implements CommandSource {

  public static CommandSource INSTANCE = new MockCommandSource();

  @Override
  public void sendMessage(final Component component) {

  }

  @Override
  public Tristate getPermissionValue(final String permission) {
    return Tristate.UNDEFINED;
  }
}
