package com.velocitypowered.proxy.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;

public final class VelocityMessages {

  public static final Component ONLINE_MODE_ONLY = TextComponent
      .builder("This server only accepts connections from online-mode clients.")
      .color(NamedTextColor.RED)
      .append(
          TextComponent.of("\n\nDid you change your username? Sign out of Minecraft, sign back in, "
              + "and try again.", NamedTextColor.GRAY)
      )
      .build();
  public static final Component NO_AVAILABLE_SERVERS = TextComponent
      .of("No available servers", NamedTextColor.RED);
  public static final Component ALREADY_CONNECTED = TextComponent
      .of("You are already connected to this proxy!", NamedTextColor.RED);
  public static final Component MOVED_TO_NEW_SERVER = TextComponent
      .of("The server you were on kicked you: ", NamedTextColor.RED);

  private VelocityMessages() {
    throw new AssertionError();
  }
}
