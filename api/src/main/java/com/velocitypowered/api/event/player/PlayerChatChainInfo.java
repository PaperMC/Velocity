/*
 * Copyright (C) 2026 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event.player;

import com.google.common.base.Preconditions;
import java.util.BitSet;
import java.util.List;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Immutable last-seen or message-chain metadata carried by the original chat packet.
 *
 * @since 3.6.0
 */
public final class PlayerChatChainInfo {

  private final @Nullable Integer lastSeenOffset;
  private final @Nullable BitSet acknowledged;
  private final @Nullable Byte checksum;
  private final List<PlayerChatMessageLink> previousMessages;
  private final @Nullable PlayerChatMessageLink lastMessage;

  /**
   * Creates chain metadata.
   *
   * @param lastSeenOffset modern last-seen offset, if present
   * @param acknowledged modern acknowledged bit set, if present
   * @param checksum modern last-seen checksum, if present
   * @param previousMessages keyed-chat previous message links
   * @param lastMessage keyed-chat last message link, if present
   */
  public PlayerChatChainInfo(@Nullable Integer lastSeenOffset, @Nullable BitSet acknowledged,
      @Nullable Byte checksum, List<PlayerChatMessageLink> previousMessages,
      @Nullable PlayerChatMessageLink lastMessage) {
    this.lastSeenOffset = lastSeenOffset;
    this.acknowledged = acknowledged == null ? null : (BitSet) acknowledged.clone();
    this.checksum = checksum;
    this.previousMessages = List.copyOf(Preconditions.checkNotNull(previousMessages,
        "previousMessages"));
    this.lastMessage = lastMessage;
  }

  /**
   * Returns the modern last-seen offset.
   *
   * @return the offset, if present
   */
  public Optional<Integer> getLastSeenOffset() {
    return Optional.ofNullable(lastSeenOffset);
  }

  /**
   * Returns the modern acknowledged last-seen window.
   *
   * @return the acknowledged window, if present
   */
  public Optional<BitSet> getAcknowledged() {
    return acknowledged == null ? Optional.empty() : Optional.of((BitSet) acknowledged.clone());
  }

  /**
   * Returns the modern last-seen checksum.
   *
   * @return the checksum, if present
   */
  public Optional<Byte> getChecksum() {
    return Optional.ofNullable(checksum);
  }

  /**
   * Returns keyed-chat previous-message links.
   *
   * @return immutable previous-message links
   */
  public List<PlayerChatMessageLink> getPreviousMessages() {
    return previousMessages;
  }

  /**
   * Returns the keyed-chat last-message link.
   *
   * @return the last-message link, if present
   */
  public Optional<PlayerChatMessageLink> getLastMessage() {
    return Optional.ofNullable(lastMessage);
  }
}
