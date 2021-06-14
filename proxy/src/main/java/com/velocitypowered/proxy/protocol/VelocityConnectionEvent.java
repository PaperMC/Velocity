package com.velocitypowered.proxy.protocol;

/**
 * Describes various events fired during the course of a connection.
 */
public enum VelocityConnectionEvent {
  COMPRESSION_ENABLED,
  COMPRESSION_DISABLED,
  ENCRYPTION_ENABLED,
  PROTOCOL_VERSION_CHANGED
}
