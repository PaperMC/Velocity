package com.velocitypowered.proxy.connection.util;

import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;

public class ConnectionMessages {

  public static final TextComponent ALREADY_CONNECTED = TextComponent
      .of("You are already connected to this server!", NamedTextColor.RED);
  public static final TextComponent IN_PROGRESS = TextComponent
      .of("You are already connecting to a server!", NamedTextColor.RED);
  public static final TextComponent INTERNAL_SERVER_CONNECTION_ERROR = TextComponent
      .of("An internal server connection error occurred.", NamedTextColor.RED);

  private ConnectionMessages() {
    throw new AssertionError();
  }
}
