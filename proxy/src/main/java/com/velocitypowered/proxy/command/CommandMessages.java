package com.velocitypowered.proxy.command;

import com.velocitypowered.proxy.text.translation.Translatable;

class CommandMessages {

  public static final Translatable ONLY_PLAYERS_CAN_EXECUTE = Translatable
      .of("velocity.command.only-players-can-execute");
  public static final Translatable TOO_MANY_ARGUMENTS = Translatable
      .of("velocity.command.too-many-arguments");
  public static final Translatable SERVER_DOESNT_EXIST = Translatable
      .of("velocity.command.server-doesnt-exist");

  private CommandMessages() {
    throw new AssertionError();
  }
}
