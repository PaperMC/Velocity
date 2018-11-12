package com.velocitypowered.api.util;

import java.util.UUID;

/**
 * Represents an object that can be identified by its UUID
 */
public interface Identifiable {

  /**
   * Returns the {@code UUID} attached to this object.
   *
   * @return the UUID
   */
  UUID getUniqueId();
}
