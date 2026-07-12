/*
 * Copyright (C) 2026 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event.player;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.proxy.Player;
import java.util.EnumSet;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Immutable representation of the original Minecraft player-chat message submitted by a client.
 *
 * <p>This object describes protocol metadata only. It is separate from
 * {@link PlayerChatEvent.ChatResult}, which describes whether Velocity will forward, deny, or
 * replace the message after plugins run. Changing the event result never mutates this original
 * message model.</p>
 *
 * <p>Legacy keyed chat and modern session chat are intentionally exposed separately. A signed
 * message is not necessarily validated; validation flags report only what Velocity actually knows
 * or performs on the current path.</p>
 *
 * @since 3.6.0
 */
public final class PlayerChatMessage {

  private final Player sender;
  private final String message;
  private final PlayerChatProtocol protocol;
  private final PlayerChatSignedState signedState;
  private final @Nullable PlayerChatSignature signature;
  private final @Nullable PlayerChatKeyInfo keyInfo;
  private final @Nullable PlayerChatSessionInfo sessionInfo;
  private final @Nullable PlayerChatChainInfo chainInfo;
  private final EnumSet<PlayerChatValidationFlag> validationFlags;
  private final PlayerChatCapabilities capabilities;

  /**
   * Creates a player-chat message.
   *
   * @param sender the player who submitted the message
   * @param message the exact body submitted by the client
   * @param protocol the chat protocol generation
   * @param signedState the signing state
   * @param signature the signature fields, if present
   * @param keyInfo legacy keyed-chat key metadata, if present
   * @param sessionInfo modern session-chat metadata, if present
   * @param chainInfo last-seen or message-chain metadata, if present
   * @param validationFlags validation facts known to Velocity
   * @param capabilities operations Velocity can safely perform
   */
  public PlayerChatMessage(Player sender, String message, PlayerChatProtocol protocol,
      PlayerChatSignedState signedState, @Nullable PlayerChatSignature signature,
      @Nullable PlayerChatKeyInfo keyInfo, @Nullable PlayerChatSessionInfo sessionInfo,
      @Nullable PlayerChatChainInfo chainInfo, EnumSet<PlayerChatValidationFlag> validationFlags,
      PlayerChatCapabilities capabilities) {
    this.sender = Preconditions.checkNotNull(sender, "sender");
    this.message = Preconditions.checkNotNull(message, "message");
    this.protocol = Preconditions.checkNotNull(protocol, "protocol");
    this.signedState = Preconditions.checkNotNull(signedState, "signedState");
    this.signature = signature;
    this.keyInfo = keyInfo;
    this.sessionInfo = sessionInfo;
    this.chainInfo = chainInfo;
    this.validationFlags = EnumSet.noneOf(PlayerChatValidationFlag.class);
    this.validationFlags.addAll(Preconditions.checkNotNull(validationFlags, "validationFlags"));
    this.capabilities = Preconditions.checkNotNull(capabilities, "capabilities");
  }

  /**
   * Returns the player who submitted this message.
   *
   * @return the sender
   */
  public Player getSender() {
    return sender;
  }

  /**
   * Returns the exact message body submitted by the client.
   *
   * @return the original message body
   */
  public String getMessage() {
    return message;
  }

  /**
   * Returns the Minecraft chat protocol generation.
   *
   * @return the protocol generation
   */
  public PlayerChatProtocol getProtocol() {
    return protocol;
  }

  /**
   * Returns the signing state.
   *
   * @return the signing state
   */
  public PlayerChatSignedState getSignedState() {
    return signedState;
  }

  /**
   * Returns whether the original packet carried signature bytes.
   *
   * @return whether a signature was present
   */
  public boolean hasSignature() {
    return signature != null;
  }

  /**
   * Returns the original signature fields.
   *
   * @return signature fields, if present
   */
  public Optional<PlayerChatSignature> getSignature() {
    return Optional.ofNullable(signature);
  }

  /**
   * Returns legacy keyed-chat key metadata.
   *
   * @return keyed-chat metadata, if present
   */
  public Optional<PlayerChatKeyInfo> getKeyInfo() {
    return Optional.ofNullable(keyInfo);
  }

  /**
   * Returns modern session-chat metadata.
   *
   * @return session-chat metadata, if present
   */
  public Optional<PlayerChatSessionInfo> getSessionInfo() {
    return Optional.ofNullable(sessionInfo);
  }

  /**
   * Returns last-seen or message-chain metadata.
   *
   * @return chain metadata, if present
   */
  public Optional<PlayerChatChainInfo> getChainInfo() {
    return Optional.ofNullable(chainInfo);
  }

  /**
   * Returns validation facts Velocity knows about this message.
   *
   * @return validation flags
   */
  public EnumSet<PlayerChatValidationFlag> getValidationFlags() {
    EnumSet<PlayerChatValidationFlag> copy = EnumSet.noneOf(PlayerChatValidationFlag.class);
    copy.addAll(validationFlags);
    return copy;
  }

  /**
   * Returns whether the validation flag is present.
   *
   * @param flag the flag to test
   * @return whether the flag is present
   */
  public boolean hasValidationFlag(PlayerChatValidationFlag flag) {
    return validationFlags.contains(Preconditions.checkNotNull(flag, "flag"));
  }

  /**
   * Returns the operations Velocity can safely perform for this message.
   *
   * @return the capabilities
   */
  public PlayerChatCapabilities getCapabilities() {
    return capabilities;
  }

  /**
   * Creates metadata for pre-1.19 legacy chat.
   *
   * @param sender the sender
   * @param message the original body
   * @return legacy message metadata
   */
  public static PlayerChatMessage legacy(Player sender, String message) {
    return new PlayerChatMessage(sender, message, PlayerChatProtocol.LEGACY_UNSIGNED,
        PlayerChatSignedState.LEGACY, null, null, null, null,
        EnumSet.of(PlayerChatValidationFlag.UNSIGNED), PlayerChatCapabilities.unsigned());
  }

  /**
   * Creates metadata for unsigned modern chat.
   *
   * @param sender the sender
   * @param message the original body
   * @param protocol the protocol generation
   * @param chainInfo chain metadata, if present
   * @return unsigned message metadata
   */
  public static PlayerChatMessage unsigned(Player sender, String message,
      PlayerChatProtocol protocol, @Nullable PlayerChatChainInfo chainInfo) {
    EnumSet<PlayerChatValidationFlag> flags = EnumSet.of(PlayerChatValidationFlag.UNSIGNED);
    if (chainInfo != null) {
      flags.add(PlayerChatValidationFlag.CHAIN_STATE_AVAILABLE);
    }
    return new PlayerChatMessage(sender, message, protocol, PlayerChatSignedState.UNSIGNED,
        null, null, null, chainInfo, flags, PlayerChatCapabilities.unsigned());
  }
}
