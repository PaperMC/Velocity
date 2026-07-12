/*
 * Copyright (C) 2026 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event.player;

/**
 * Describes operations Velocity can safely perform with a player-chat message on the current
 * protocol path.
 *
 * <p>These capabilities describe Velocity's forwarding behavior. They do not grant a trust model
 * and do not mean a plugin can safely rewrite, decorate, reroute, or re-emit signed chat unless the
 * corresponding method returns {@code true}.</p>
 *
 * @since 3.6.0
 */
public final class PlayerChatCapabilities {

  private final boolean safelyDeny;
  private final boolean safelyRewrite;
  private final boolean preserveSignature;
  private final boolean decorateSignedBody;
  private final boolean routeAsOriginalPlayerChat;
  private final boolean forwardAcrossServerBoundary;

  private PlayerChatCapabilities(boolean safelyDeny, boolean safelyRewrite,
      boolean preserveSignature, boolean decorateSignedBody, boolean routeAsOriginalPlayerChat,
      boolean forwardAcrossServerBoundary) {
    this.safelyDeny = safelyDeny;
    this.safelyRewrite = safelyRewrite;
    this.preserveSignature = preserveSignature;
    this.decorateSignedBody = decorateSignedBody;
    this.routeAsOriginalPlayerChat = routeAsOriginalPlayerChat;
    this.forwardAcrossServerBoundary = forwardAcrossServerBoundary;
  }

  /**
   * Returns whether Velocity can deny this message without creating an illegal signed-chat state.
   *
   * @return whether denying is protocol-safe
   */
  public boolean canSafelyDeny() {
    return safelyDeny;
  }

  /**
   * Returns whether Velocity can replace the submitted body without creating an illegal
   * signed-chat state.
   *
   * @return whether rewriting is protocol-safe
   */
  public boolean canSafelyRewrite() {
    return safelyRewrite;
  }

  /**
   * Returns whether forwarding the message unchanged can preserve the original signature.
   *
   * @return whether the original signature can be preserved
   */
  public boolean canPreserveSignature() {
    return preserveSignature;
  }

  /**
   * Returns whether Velocity can decorate the signed body while preserving signed-chat semantics.
   *
   * @return whether signed-body decoration is supported
   */
  public boolean canDecorateSignedBody() {
    return decorateSignedBody;
  }

  /**
   * Returns whether Velocity can route this packet onward as original player chat.
   *
   * @return whether original player-chat forwarding is supported
   */
  public boolean canRouteAsOriginalPlayerChat() {
    return routeAsOriginalPlayerChat;
  }

  /**
   * Returns whether Velocity can carry this exact message across the current client-to-backend
   * boundary.
   *
   * @return whether forwarding across the server boundary is supported
   */
  public boolean canForwardAcrossServerBoundary() {
    return forwardAcrossServerBoundary;
  }

  /**
   * Capabilities for legacy or unsigned chat that Velocity can rewrite using a fresh packet.
   *
   * @return unsigned capabilities
   */
  public static PlayerChatCapabilities unsigned() {
    return new PlayerChatCapabilities(true, true, false, false, true, true);
  }

  /**
   * Capabilities for signed chat that Velocity can forward unchanged but cannot safely rewrite.
   *
   * @return signed capabilities
   */
  public static PlayerChatCapabilities signedPassthrough() {
    return new PlayerChatCapabilities(false, false, true, false, true, true);
  }
}
