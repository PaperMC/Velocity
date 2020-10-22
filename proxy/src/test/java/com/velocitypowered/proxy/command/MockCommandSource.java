package com.velocitypowered.proxy.command;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.permission.Tristate;

public class MockCommandSource implements CommandSource {

  public static final CommandSource INSTANCE = new MockCommandSource();

  @Override
  public Tristate getPermissionValue(final String permission) {
    return Tristate.UNDEFINED;
  }
}
