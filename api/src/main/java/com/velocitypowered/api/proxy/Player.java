/*
 * Copyright (C) 2018-2023 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.proxy;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.event.player.CookieReceiveEvent;
import com.velocitypowered.api.event.player.PlayerResourcePackStatusEvent;
import com.velocitypowered.api.proxy.crypto.KeyIdentifiable;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.ChannelMessageSink;
import com.velocitypowered.api.proxy.messages.ChannelMessageSource;
import com.velocitypowered.api.proxy.messages.PluginMessageEncoder;
import com.velocitypowered.api.proxy.player.PlayerSettings;
import com.velocitypowered.api.proxy.player.ResourcePackInfo;
import com.velocitypowered.api.proxy.player.TabList;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.util.GameProfile;
import com.velocitypowered.api.util.ModInfo;
import com.velocitypowered.api.util.ServerLink;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.function.UnaryOperator;
import net.kyori.adventure.dialog.DialogLike;
import net.kyori.adventure.identity.Identified;
import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.Keyed;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.SoundStop;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.event.HoverEventSource;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a player who is connected to the proxy.
 */
public interface Player extends
    /* Fundamental Velocity interfaces */
    CommandSource, InboundConnection, ChannelMessageSource, ChannelMessageSink,
    /* Adventure-specific interfaces */
    Identified, HoverEventSource<HoverEvent.ShowEntity>, Keyed, KeyIdentifiable, Sound.Emitter {

  /**
   * Returns the player's current username.
   *
   * @return the username
   */
  String getUsername();

  /**
   * Returns the locale the proxy will use to send messages translated via the Adventure global
   * translator. By default, the value of {@link PlayerSettings#getLocale()} is used.
   *
   * <p>This can be {@code null} when the client has not yet connected to any server.</p>
   *
   * @return the locale.
   */
  @Nullable Locale getEffectiveLocale();

  /**
   * Change the locale the proxy will be translating its messages to.
   *
   * @param locale the locale to translate to
   */
  void setEffectiveLocale(Locale locale);

  /**
   * Returns the player's UUID.
   *
   * @return the UUID
   */
  UUID getUniqueId();

  /**
   * Returns the server that the player is currently connected to.
   *
   * @return an {@link Optional} the server that the player is connected to, which may be empty
   */
  Optional<ServerConnection> getCurrentServer();

  /**
   * Returns the player's client settings.
   *
   * @return the settings
   */
  PlayerSettings getPlayerSettings();

  /**
   * Returns whether the player has sent its client settings.
   *
   * @return true if the player has sent its client settings
   */
  boolean hasSentPlayerSettings();

  /**
   * Returns the player's mod info if they have a modded client.
   *
   * @return an {@link Optional} the mod info. which may be empty
   */
  Optional<ModInfo> getModInfo();

  /**
   * Gets the player's estimated ping in milliseconds.
   *
   * @return the player's ping or -1 if ping information is currently unknown
   */
  long getPing();

  /**
   * Returns the player's connection status.
   *
   * @return true if the player is authenticated with Mojang servers
   */
  boolean isOnlineMode();

  /**
   * Creates a new connection request so that the player can connect to another server.
   *
   * @param server the server to connect to
   * @return a new connection request
   */
  ConnectionRequestBuilder createConnectionRequest(RegisteredServer server);

  /**
   * Gets the player's profile properties.
   *
   * <p>The returned list may be unmodifiable.</p>
   *
   * @return the player's profile properties
   */
  List<GameProfile.Property> getGameProfileProperties();

  /**
   * Sets the player's profile properties.
   *
   * @param properties the properties
   */
  void setGameProfileProperties(List<GameProfile.Property> properties);

  /**
   * Returns the player's game profile.
   *
   * @return the player's profile
   */
  GameProfile getGameProfile();

  /**
   * Clears the tab list header and footer for the player.
   *
   * @deprecated Use {@link Player#clearPlayerListHeaderAndFooter()}.
   */
  @Deprecated
  default void clearHeaderAndFooter() {
    clearPlayerListHeaderAndFooter();
  }

  /**
   * Clears the player list header and footer.
   */
  void clearPlayerListHeaderAndFooter();

  /**
   * Returns the player's player list header.
   *
   * @return this player's player list header
   */
  Component getPlayerListHeader();

  /**
   * Returns the player's player list footer.
   *
   * @return this player's tab list
   */
  Component getPlayerListFooter();

  /**
   * Returns the player's tab list.
   *
   * @return this player's tab list
   */
  TabList getTabList();

  /**
   * Disconnects the player with the specified reason. Once this method is called, further calls to
   * other {@link Player} methods will become undefined.
   *
   * @param reason component with the reason
   */
  void disconnect(Component reason);

  /**
   * Sends chat input onto the players current server as if they typed it into the client chat box.
   *
   * @param input the chat input to send
   */
  void spoofChatInput(String input);

  /**
   * Sends the specified resource pack from {@code url} to the user. If at all possible, send the
   * resource pack using {@link #sendResourcePack(String, byte[])}. To monitor the status of the
   * sent resource pack, subscribe to {@link PlayerResourcePackStatusEvent}.
   *
   * @param url the URL for the resource pack
   * @deprecated Use {@link #sendResourcePackOffer(ResourcePackInfo)} instead
   */
  @Deprecated
  void sendResourcePack(String url);

  /**
   * Sends the specified resource pack from {@code url} to the user, using the specified 20-byte
   * SHA-1 hash. To monitor the status of the sent resource pack, subscribe to
   * {@link PlayerResourcePackStatusEvent}.
   *
   * @param url the URL for the resource pack
   * @param hash the SHA-1 hash value for the resource pack
   * @deprecated Use {@link #sendResourcePackOffer(ResourcePackInfo)} instead
   */
  @Deprecated
  void sendResourcePack(String url, byte[] hash);

  /**
   * Queues and sends a new Resource-pack offer to the player.
   * To monitor the status of the sent resource pack, subscribe to
   * {@link PlayerResourcePackStatusEvent}.
   * To create a {@link ResourcePackInfo} use the
   * {@link ProxyServer#createResourcePackBuilder(String)} builder.
   *
   * @param packInfo the resource-pack in question
   */
  void sendResourcePackOffer(ResourcePackInfo packInfo);

  /**
   * Gets the {@link ResourcePackInfo} of the currently applied
   * resource-pack or null if none.
   *
   * <p>Note that since 1.20.3 it is no longer recommended to use
   * this method as it will only return the last applied
   * resource pack. To get all applied resource packs, use
   * {@link #getAppliedResourcePacks()} instead.</p>
   *
   * @return the applied resource pack or null if none.
   */
  @Nullable
  @Deprecated
  ResourcePackInfo getAppliedResourcePack();

  /**
   * Gets the {@link ResourcePackInfo} of the resource pack
   * the user is currently downloading or is currently
   * prompted to install or null if none.
   *
   * <p>Note that since 1.20.3 it is no longer recommended to use
   * this method as it will only return the last pending
   * resource pack. To get all pending resource packs, use
   * {@link #getPendingResourcePacks()} instead.</p>
   *
   * @return the pending resource pack or null if none
   */
  @Nullable
  @Deprecated
  ResourcePackInfo getPendingResourcePack();

  /**
   * Gets the {@link ResourcePackInfo} of the currently applied
   * resource-packs.
   *
   * @return collection of the applied resource packs.
   */
  @NotNull Collection<ResourcePackInfo> getAppliedResourcePacks();

  /**
   * Gets the {@link ResourcePackInfo} of the resource packs
   * the user is currently downloading or is currently
   * prompted to install.
   *
   * @return collection of the pending resource packs
   */
  @NotNull Collection<ResourcePackInfo> getPendingResourcePacks();

  /**
   * {@inheritDoc}
   *
   * <p><strong>Note that this method does not send a plugin message to the server the player
   * is connected to.</strong> You should only use this method if you are trying to communicate
   * with a mod that is installed on the player's client.</p>
   *
   * <p>To send a plugin message to the server
   * from the player, you should use the equivalent method on the instance returned by
   * {@link #getCurrentServer()}.
   *
   * <pre>
   *    final ChannelIdentifier identifier;
   *    final Player player;
   *    player.getCurrentServer()
   *          .map(ServerConnection::getServer)
   *          .ifPresent((RegisteredServer server) -> {
   *            server.sendPluginMessage(identifier, data);
   *          });
   *  </pre>
   *
   */
  @Override
  boolean sendPluginMessage(@NotNull ChannelIdentifier identifier, byte @NotNull [] data);

  /**
   * {@inheritDoc}
   *
   * <p><strong>Note that this method does not send a plugin message to the server the player
   * is connected to.</strong> You should only use this method if you are trying to communicate
   * with a mod that is installed on the player's client.</p>
   *
   * <p>To send a plugin message to the server
   * from the player, you should use the equivalent method on the instance returned by
   * {@link #getCurrentServer()}.
   */
  @Override
  boolean sendPluginMessage(@NotNull ChannelIdentifier identifier, @NotNull PluginMessageEncoder dataEncoder);

  @Override
  default @NotNull Key key() {
    return Key.key("player");
  }

  @Override
  default @NotNull HoverEvent<HoverEvent.ShowEntity> asHoverEvent(
          @NotNull UnaryOperator<HoverEvent.ShowEntity> op) {
    return HoverEvent.showEntity(op.apply(HoverEvent.ShowEntity.showEntity(this, getUniqueId(),
            Component.text(getUsername()))));
  }

  /**
   * Gets the player's client brand.
   *
   * @return the player's client brand
   */
  @Nullable String getClientBrand();

  //
  // Custom Chat Completions API
  //

  /**
   * Add custom chat completion suggestions shown to the player while typing a message.
   *
   * @param completions the completions to send
   */
  void addCustomChatCompletions(@NotNull Collection<String> completions);

  /**
   * Remove custom chat completion suggestions shown to the player while typing a message.
   *
   * <p>Online player names can't be removed with this method, it will only affect
   * custom completions added by {@link #addCustomChatCompletions(Collection)}
   * or {@link #setCustomChatCompletions(Collection)}.
   *
   * @param completions the completions to remove
   */
  void removeCustomChatCompletions(@NotNull Collection<String> completions);

  /**
   * Set the list of chat completion suggestions shown to the player while typing a message.
   *
   * <p>If completions were set previously, this method will remove them all
   * and replace them with the provided completions.
   *
   * @param completions the completions to set
   */
  void setCustomChatCompletions(@NotNull Collection<String> completions);

  //
  // Non Supported Adventure Operations
  // TODO: Service API
  //

  /**
   * {@inheritDoc}
   *
   * @apiNote <b>This method is not currently implemented in Velocity
   *     and will not perform any actions.</b>
   * @see #playSound(Sound, Sound.Emitter)
   * @see <a href="https://docs.papermc.io/velocity/dev/pitfalls/#audience-operations-are-not-fully-supported">
   *     Unsupported Adventure Operations</a>
   */
  @Override
  default void playSound(@NotNull Sound sound) {
  }

  /**
   * {@inheritDoc}
   *
   * @apiNote <b>This method is not currently implemented in Velocity
   *     and will not perform any actions.</b>
   * @see #playSound(Sound, Sound.Emitter)
   * @see <a href="https://docs.papermc.io/velocity/dev/pitfalls/#audience-operations-are-not-fully-supported">
   *     Unsupported Adventure Operations</a>
   */
  @Override
  default void playSound(@NotNull Sound sound, double x, double y, double z) {
  }

  /**
   * {@inheritDoc}
   *
   * <p><b>Note</b>: Due to <a href="https://bugs.mojang.com/browse/MC/issues/MC-146721">MC-146721</a>, stereo sounds are always played globally in 1.14+.
   *
   * <p><b>Note</b>: Due to <a href="https://bugs.mojang.com/browse/MC/issues/MC-138832">MC-138832</a>, the volume and pitch are ignored when using this method in 1.14 to 1.16.5.
   *
   * @param sound the sound to play
   * @param emitter the emitter of the sound; may be another player of this player's server
   * @since 3.4.0
   * @sinceMinecraft 1.19.3
   * @apiNote This method is currently only implemented for players on 1.19.3+
   *     and requires a present {@link #getCurrentServer} for the emitting player as well as this player.
   */
  @Override
  default void playSound(@NotNull Sound sound, @NotNull Sound.Emitter emitter) {
  }

  /**
   * {@inheritDoc}
   *
   * @param stop the sound and/or a sound source, to stop
   * @since 3.4.0
   * @sinceMinecraft 1.19.3
   * @apiNote This method is currently only implemented for players on 1.19.3+.
   */
  @Override
  default void stopSound(@NotNull SoundStop stop) {
  }

  /**
   * {@inheritDoc}
   *
   * <b>This method is not currently implemented in Velocity
   * and will not perform any actions.</b>
   *
   * @see <a href="https://docs.papermc.io/velocity/dev/pitfalls/#audience-operations-are-not-fully-supported">
   *     Unsupported Adventure Operations</a>
   */
  @Override
  default void openBook(@NotNull Book book) {
  }

  /**
   * {@inheritDoc}
   *
   * <b>This method is not currently implemented in Velocity
   * and will not perform any actions.</b>
   *
   * @see <a href="https://docs.papermc.io/velocity/dev/pitfalls/#audience-operations-are-not-fully-supported">
   *     Unsupported Adventure Operations</a>
   */
  @Override
  default void showDialog(@NotNull DialogLike dialog) {
  }

  /**
   * {@inheritDoc}
   *
   * <b>This method is not currently implemented in Velocity
   * and will not perform any actions.</b>
   *
   * @see <a href="https://docs.papermc.io/velocity/dev/pitfalls/#audience-operations-are-not-fully-supported">
   *     Unsupported Adventure Operations</a>
   */
  @Override
  default void closeDialog() {
  }

  /**
   * Transfers a Player to a host.
   *
   * @param address the host address
   * @throws IllegalArgumentException if the player is from a version lower than 1.20.5
   * @since 3.3.0
   */
  void transferToHost(@NotNull InetSocketAddress address);

  /**
   * Stores a cookie with arbitrary data on the player's client.
   *
   * @param key the identifier of the cookie
   * @param data the data of the cookie
   * @throws IllegalArgumentException if the player is from a version lower than 1.20.5
   * @since 3.3.0
   * @sinceMinecraft 1.20.5
   */
  void storeCookie(Key key, byte[] data);

  /**
   * Requests a previously stored cookie from the player's client.
   * Calling this method causes the client to send the cookie to the proxy.
   * To retrieve the actual data of the requested cookie, you have to use the
   * {@link CookieReceiveEvent}.
   *
   * @param key the identifier of the cookie
   * @throws IllegalArgumentException if the player is from a version lower than 1.20.5
   * @since 3.3.0
   * @sinceMinecraft 1.20.5
   */
  void requestCookie(Key key);

  /**
   * Send the player a list of custom links to display in their client's pause menu.
   *
   * <p>Note that later packets sent by the backend server may override links sent by the proxy.
   *
   * @param links an ordered list of {@link ServerLink}s to send to the player
   * @throws IllegalArgumentException if the player is from a version lower than 1.21
   * @since 3.3.0
   * @sinceMinecraft 1.21
   */
  void setServerLinks(@NotNull List<ServerLink> links);
}
