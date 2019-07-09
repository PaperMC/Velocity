package com.velocitypowered.proxy.command;

import net.kyori.text.TranslatableComponent;

class CommandMessages {

  public static final TranslatableComponent ONLY_PLAYERS_CAN_EXECUTE = TranslatableComponent
      .of("velocity.command.only-players-can-execute");
  public static final TranslatableComponent TOO_MANY_ARGUMENTS = TranslatableComponent
      .of("velocity.command.too-many-arguments");
  public static final TranslatableComponent SERVER_DOESNT_EXIST = TranslatableComponent
      .of("velocity.command.server-doesnt-exist");

  private CommandMessages() {
    throw new AssertionError();
  }
}
