/*
 * Copyright (C) 2018-2023 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event.player;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.event.annotation.AwaitingEvent;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.player.ResourcePackInfo;
import java.util.UUID;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * This event is fired when the status of a resource pack sent to the player by the server is
 * changed. Depending on the result of this event (which Velocity will wait until completely fired),
 * the player may be kicked from the server.
 */
@AwaitingEvent
public class PlayerResourcePackStatusEvent {

  private final Player player;
  private final UUID packId;
  private final Status status;
  private final @MonotonicNonNull ResourcePackInfo packInfo;
  private boolean overwriteKick;

  /**
   * Instantiates this event.
   */
  public PlayerResourcePackStatusEvent(
          Player player, UUID packId, Status status, ResourcePackInfo packInfo) {
    this.player = Preconditions.checkNotNull(player, "player");
    this.packId = Preconditions.checkNotNull(packId, "packId");
    this.status = Preconditions.checkNotNull(status, "status");
    this.packInfo = packInfo;
  }

  /**
   * Returns the player affected by the change in resource pack status.
   *
   * @return the player
   */
  public Player getPlayer() {
    return player;
  }

  /**
   * Returns the id of the resource pack.
   *
   * @return the id
   */
  public UUID getPackId() {
    return packId;
  }

  /**
   * Returns the new status for the resource pack.
   *
   * @return the new status
   */
  public Status getStatus() {
    return status;
  }

  /**
   * Returns the {@link ResourcePackInfo} this response is for.
   *
   * @return the resource-pack info or null if no request was recorded
   */
  @Nullable
  public ResourcePackInfo getPackInfo() {
    return packInfo;
  }

  /**
   * Gets whether or not to override the kick resulting from
   * {@link ResourcePackInfo#getShouldForce()} being true.
   *
   * @return whether or not to overwrite the result
   */
  public boolean isOverwriteKick() {
    return overwriteKick;
  }

  /**
   * Set to true to prevent {@link ResourcePackInfo#getShouldForce()}
   * from kicking the player.
   * Overwriting this kick is only possible on versions older than 1.17,
   * as the client or server will enforce this regardless. Cancelling the resulting
   * kick-events will not prevent the player from disconnecting from the proxy.
   *
   * @param overwriteKick whether or not to cancel the kick
   * @throws IllegalArgumentException if the player version is 1.17 or newer
   */
  public void setOverwriteKick(boolean overwriteKick) {
    Preconditions.checkArgument(player.getProtocolVersion()
            .lessThan(ProtocolVersion.MINECRAFT_1_17),
            "overwriteKick is not supported on 1.17 or newer");
    this.overwriteKick = overwriteKick;
  }

  @Override
  public String toString() {
    return "PlayerResourcePackStatusEvent{"
        + "player=" + player
        + ", status=" + status
        + ", packInfo=" + packInfo
        + '}';
  }

  /**
   * Represents the possible statuses for the resource pack.
   */
  public enum Status {
    /**
     * The resource pack was applied successfully.
     */
    SUCCESSFUL,
    /**
     * The player declined to download the resource pack.
     */
    DECLINED,
    /**
     * The player could not download the resource pack.
     */
    FAILED_DOWNLOAD,
    /**
     * The player has accepted the resource pack and is now downloading it.
     */
    ACCEPTED,
    /**
     * The player has downloaded the resource pack.
     */
    DOWNLOADED,
    /**
     * The URL of the resource pack failed to load.
     */
    INVALID_URL,
    /**
     * The player failed to reload the resource pack.
     */
    FAILED_RELOAD,
    /**
     * The resource pack was discarded.
     */
    DISCARDED;

    /**
     * Returns true if the resource pack status is intermediate, indicating that the player has
     * either accepted the resource pack and is currently downloading it or has successfully
     * downloaded it.
     *
     * @return true if the status is intermediate (ACCEPTED or DOWNLOADED), false otherwise
     */
    public boolean isIntermediate() {
      return this == ACCEPTED || this == DOWNLOADED;
    }
  }
}