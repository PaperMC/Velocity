package com.velocitypowered.api.proxy.messages;

/**
 * Represents a kind of channel identifier.
 */
public interface ChannelIdentifier {

  /**
   * Returns the textual representation of this identifier.
   *
   * @return the textual representation of the identifier
   */
  String getId();
}
