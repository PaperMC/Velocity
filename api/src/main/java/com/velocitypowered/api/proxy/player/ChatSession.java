package com.velocitypowered.api.proxy.player;

import com.velocitypowered.api.proxy.crypto.KeyIdentifiable;
import java.util.UUID;

/**
 * Represents a chat session held by a player.
 */
public interface ChatSession extends KeyIdentifiable {
  /**
   * Returns the {@link UUID} of the session.
   *
   * @return the session UUID
   */
  UUID getSessionId();
}
