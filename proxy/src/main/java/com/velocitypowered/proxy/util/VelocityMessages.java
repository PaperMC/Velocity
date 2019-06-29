package com.velocitypowered.proxy.util;

import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;

public class VelocityMessages {

  public static final Component ONLINE_MODE_ONLY = TextComponent
      .builder("This server only accepts connections from online-mode clients.")
      .color(TextColor.RED)
      .append(
          TextComponent.of("\n\nDid you change your username? Sign out of Minecraft, sign back in, "
              + "and try again.", TextColor.GRAY)
      )
      .build();
  public static final Component NO_PROXY_BEHIND_PROXY = TextComponent
      .of("Running Velocity behind Velocity isn't supported.", TextColor.RED);
  public static final Component NO_AVAILABLE_SERVERS = TextComponent
      .of("No available servers", TextColor.RED);
  public static final Component ALREADY_CONNECTED = TextComponent
      .of("You are already connected to this proxy!", TextColor.RED);
  public static final Component MOVED_TO_NEW_SERVER = TextComponent
      .of("You were moved from the server you were on because you were kicked", TextColor.RED);

  private VelocityMessages() {
    throw new AssertionError();
  }
}
