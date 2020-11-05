package com.velocitypowered.proxy.event;

import com.velocitypowered.proxy.plugin.MockPluginManager;

/**
 * A mock {@link VelocityEventManager}. Must be shutdown after use!
 */
public class MockEventManager extends VelocityEventManager {

  public MockEventManager() {
    super(MockPluginManager.INSTANCE);
  }
}
