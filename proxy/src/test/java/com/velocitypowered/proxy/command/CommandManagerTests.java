package com.velocitypowered.proxy.command;

import com.velocitypowered.proxy.plugin.MockEventManager;
import com.velocitypowered.proxy.plugin.VelocityEventManager;

public class CommandManagerTests {

  private static final VelocityEventManager EVENT_MANAGER = new MockEventManager();

  static {
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      try {
        EVENT_MANAGER.shutdown();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }));
  }

  static VelocityCommandManager createManager() {
    return new VelocityCommandManager(EVENT_MANAGER);
  }


}
