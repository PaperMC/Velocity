package com.velocitypowered.proxy.util;

import net.kyori.text.Component;
import net.kyori.text.TranslatableComponent;

public class VelocityMessages {

  public static final Component ONLINE_MODE_ONLY = TranslatableComponent
      .of("velocity.only-online-mode");
  public static final Component NO_PROXY_BEHIND_PROXY = TranslatableComponent
      .of("velocity.no-proxy-behind-proxy");
  public static final Component NO_AVAILABLE_SERVERS = TranslatableComponent
      .of("velocity.no-available-servers");
  public static final Component ALREADY_CONNECTED = TranslatableComponent
      .of("velocity.already-connected");
  public static final Component MOVED_TO_NEW_SERVER = TranslatableComponent
      .of("velocity.moved-to-new-server");

  private VelocityMessages() {
    throw new AssertionError();
  }
}
