/*
 * Copyright (C) 2018-2021 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event.player;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.proxy.Player;

/**
 * Fired when a {@link Player} is sent the <code>minecraft:brand</code> plugin message.
 */
public final class PlayerServerBrandEvent {
  private final Player player;
  private final String originalBrand;
  private String brand;

  /**
   * Creates a new instance.
   *
   * @param player        the {@link Player} receiving the brand.
   * @param originalBrand the brand the backend server wanted to send.
   * @param brand         the brand to send. (defaults to {@code Backend (Velocity)}
   */
  public PlayerServerBrandEvent(Player player, String originalBrand, String brand) {
    this.player = Preconditions.checkNotNull(player);
    this.originalBrand = Preconditions.checkNotNull(originalBrand);
    this.brand = Preconditions.checkNotNull(brand);
  }

  /**
   * Returns the player receiving the brand packet.
   *
   * @return the player receiving the brand packet.
   */
  public Player getPlayer() {
    return player;
  }

  /**
   * Returns original brand that the backend server wanted to send before the proxy has an influence.
   *
   * @return the original backend server brand.
   */
  public String getOriginalBrand() {
    return originalBrand;
  }

  /**
   * Returns the brand that the player will receive.
   * If this has not yet been modified via this event, it will be the default velocity brand of {@code Backend (Velocity)}
   *
   * @return the brand that the player will receive.
   */
  public String getBrand() {
    return brand;
  }

  /**
   * Set the brand that the player will receive.
   *
   * @param brand the new brand for the player to receive.
   */
  public void setBrand(String brand) {
    this.brand = brand;
  }

  @Override
  public String toString() {
    return "PlayerServerBrandEvent{"
            + "player=" + player
            + ", originalBrand='" + originalBrand + "'"
            + ", brand='" + brand + "'"
            + '}';
  }
}
