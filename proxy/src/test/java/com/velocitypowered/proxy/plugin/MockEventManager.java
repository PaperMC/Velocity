package com.velocitypowered.proxy.plugin;

/**
 * A mock {@link VelocityEventManager}. Must be shutdown after use!
 */
public class MockEventManager extends VelocityEventManager {

  public MockEventManager() {
    super(MockPluginManager.INSTANCE);
  }
}
