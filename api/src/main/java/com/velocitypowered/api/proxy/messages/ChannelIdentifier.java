package com.velocitypowered.api.proxy.messages;

/**
 * Represents a channel identifier for use with plugin messaging.
 */
public interface ChannelIdentifier {

  /**
   * Returns the textual representation of this identifier.
   *
   * @return the textual representation of the identifier
   */
  String getId();
}
