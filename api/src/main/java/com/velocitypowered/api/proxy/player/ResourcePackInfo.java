/*
 * Copyright (C) 2021-2023 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.proxy.player;

import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Represents the information for a resource pack to apply that can be sent to the client.
 */
public interface ResourcePackInfo {

  /**
   * Gets the link the resource-pack can be found at.
   *
   * @return the location of the resource-pack
   */
  String getUrl();

  /**
   * Gets the {@link Component} that is displayed on the resource-pack prompt.
   * This is only displayed if the client version is 1.17 or newer.
   *
   * @return the prompt if present or null otherwise
   */
  @Nullable
  Component getPrompt();

  /**
   * Gets whether or not the acceptance of the resource-pack is enforced.
   * See {@link Builder#setShouldForce(boolean)} for more information.
   *
   * @return whether or not to force usage of this resource-pack
   */
  boolean getShouldForce();

  /**
   * Gets the SHA-1 hash of the resource-pack
   * See {@link Builder#setHash(byte[])} for more information.
   *
   * @return the hash if present or null otherwise
   */
  @Nullable
  byte[] getHash();

  /**
   * Gets the {@link Origin} of this resource-pack.
   *
   * @return the origin of the resource pack
   */
  Origin getOrigin();

  /**
   * Gets the original {@link Origin} of the resource-pack.
   * The original origin may differ if the resource pack was altered in the event
   * {@link com.velocitypowered.api.event.player.ServerResourcePackSendEvent}.
   *
   * @return the origin of the resource pack
   */
  Origin getOriginalOrigin();

  /**
   * Returns a copy of this {@link ResourcePackInfo} instance as a builder so that it can
   * be modified.
   * It is <b>not</b> guaranteed that
   * {@code resourcePackInfo.asBuilder().build().equals(resourcePackInfo)} is true. That is due to
   * the transient {@link ResourcePackInfo#getOrigin()} and
   * {@link ResourcePackInfo#getOriginalOrigin()} fields.
   *
   *
   * @return a content-copy of this instance as a {@link ResourcePackInfo.Builder}
   */
  ResourcePackInfo.Builder asBuilder();

  /**
   * Returns a copy of this {@link ResourcePackInfo} instance as a builder, using the new URL.
   * <p/>
   * It is <b>not</b> guaranteed that
   * {@code resourcePackInfo.asBuilder(resourcePackInfo.getUrl()).build().equals(resourcePackInfo)}
   * is true, because the {@link ResourcePackInfo#getOrigin()} and
   * {@link ResourcePackInfo#getOriginalOrigin()} fields are transient.
   *
   * @param newUrl The new URL to use in the updated builder.
   *
   * @return a content-copy of this instance as a {@link ResourcePackInfo.Builder}
   */
  ResourcePackInfo.Builder asBuilder(String newUrl);

  /**
   * Builder for {@link ResourcePackInfo} instances.
   */
  interface Builder {

    /**
     * Sets the resource-pack as required to play on the network.
     * This feature was introduced in 1.17.
     * Setting this to true has one of two effects:
     * If the client is on 1.17 or newer:
     *  - The resource-pack prompt will display without a decline button
     *  - Accept or disconnect are the only available options but players may still press escape.
     *  - Forces the resource-pack offer prompt to display even if the player has
     *    previously declined or disabled resource packs
     *  - The player will be disconnected from the network if they close/skip the prompt.
     * If the client is on a version older than 1.17:
     *  - If the player accepts the resource pack or has previously accepted a resource-pack
     *    then nothing else will happen.
     *  - If the player declines the resource pack or has previously declined a resource-pack
     *    the player will be disconnected from the network
     *
     * @param shouldForce whether or not to force the client to accept the resource pack
     */
    Builder setShouldForce(boolean shouldForce);

    /**
     * Sets the SHA-1 hash of the provided resource pack.
     * Note: It is recommended to always set this hash.
     * If this hash is not set/ not present then the client will always download
     * the resource pack even if it may still be cached. By having this hash present,
     * the client will check first whether or not a resource pack by this hash is cached
     * before downloading.
     *
     * @param hash the SHA-1 hash of the resource-pack
     */
    Builder setHash(@Nullable byte[] hash);

    /**
     * Sets a {@link Component} to display on the download prompt.
     * This will only display if the client version is 1.17 or newer.
     *
     * @param prompt the component to display
     */
    Builder setPrompt(@Nullable Component prompt);

    /**
     * Builds the {@link ResourcePackInfo} from the provided info for use with
     * {@link com.velocitypowered.api.proxy.Player#sendResourcePackOffer(ResourcePackInfo)}.
     * Note: Some features may be version-dependent. Check before use.
     *
     * @return a ResourcePackInfo instance from the provided information
     */
    ResourcePackInfo build();
  }

  /**
   * Represents the origin of the resource-pack.
   */
  enum Origin {
    /**
     * Resource-pack originated from the downstream server.
     */
    DOWNSTREAM_SERVER,
    /**
     * The resource-pack originated from a plugin on this proxy.
     */
    PLUGIN_ON_PROXY
  }
}
