package com.velocitypowered.natives.util;

public enum BufferPreference {
  /**
   * A heap buffer is preferred (but not required).
   */
  HEAP_PREFERRED,
  /**
   * A direct buffer is preferred (but not required).
   */
  DIRECT_PREFERRED,
  /**
   * A direct buffer is required.
   */
  DIRECT_REQUIRED
}
