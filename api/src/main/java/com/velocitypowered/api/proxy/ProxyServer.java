/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.proxy;

import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.ConsoleCommandSource;
import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.network.NetworkEndpoint;
import com.velocitypowered.api.plugin.PluginManager;
import com.velocitypowered.api.proxy.config.ProxyConfig;
import com.velocitypowered.api.proxy.connection.Player;
import com.velocitypowered.api.proxy.messages.ChannelRegistrar;
import com.velocitypowered.api.proxy.player.PlayerIdentity;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import com.velocitypowered.api.scheduler.Scheduler;
import com.velocitypowered.api.util.ProxyVersion;
import java.util.Collection;
import java.util.UUID;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Provides an interface to a Minecraft server proxy.
 */
public interface ProxyServer extends Audience {

  /**
   * Shuts down the proxy, kicking players with the specified {@code reason}.
   *
   * @param reason message to kick online players with
   */
  void shutdown(Component reason);

  /**
   * Shuts down the proxy, kicking players with the default reason.
   */
  void shutdown();

  /**
   * Retrieves the player currently connected to this proxy by their Minecraft username. The search
   * is case-insensitive.
   *
   * @param username the username to search for
   * @return the player instance, if connected, else {@code null}
   */
  @Nullable Player player(String username);

  /**
   * Retrieves the player currently connected to this proxy by their Minecraft UUID.
   *
   * @param uuid the UUID
   * @return the player instance, if connected, else {@code null}
   */
  @Nullable Player player(UUID uuid);

  /**
   * Retrieves the player currently connected to this proxy by their identity.
   *
   * @param identity the identity
   * @return the player instance, if connected, else {@code null}
   */
  @Nullable default Player player(PlayerIdentity identity) {
    return player(identity.uuid());
  }

  /**
   * Retrieves all players currently connected to this proxy. This call may or may not be a snapshot
   * of all players online.
   *
   * @return the players online on this proxy
   */
  Collection<Player> connectedPlayers();

  /**
   * Returns the number of players currently connected to this proxy.
   *
   * @return the players on this proxy
   */
  int countConnectedPlayers();

  /**
   * Retrieves a registered {@link RegisteredServer} instance by its name. The search is
   * case-insensitive.
   *
   * @param name the name of the server
   * @return the registered server, which may be empty
   */
  @Nullable RegisteredServer server(String name);

  /**
   * Retrieves all {@link RegisteredServer}s registered with this proxy.
   *
   * @return the servers registered with this proxy
   */
  Collection<RegisteredServer> registeredServers();

  /**
   * Matches all {@link Player}s whose names start with the provided partial name.
   *
   * @param partialName the partial name to check for
   * @return a collection of mathed {@link Player}s
   */
  Collection<Player> matchPlayer(String partialName);

  /**
   * Matches all {@link RegisteredServer}s whose names start with the provided partial name.
   *
   * @param partialName the partial name to check for
   * @return a collection of mathed {@link RegisteredServer}s
   */
  Collection<RegisteredServer> matchServer(String partialName);

  /**
   * Registers a server with this proxy. A server with this name should not already exist.
   *
   * @param server the server to register
   * @return the newly registered server
   */
  RegisteredServer registerServer(ServerInfo server);

  /**
   * Unregisters this server from the proxy.
   *
   * @param server the server to unregister
   */
  void unregisterServer(ServerInfo server);

  /**
   * Returns an instance of {@link CommandSource} that can be used to determine if the command is
   * being invoked by the console or a console-like executor. Plugins that execute commands are
   * strongly urged to implement their own {@link CommandSource} instead of using the console
   * invoker.
   *
   * @return the console command invoker
   */
  ConsoleCommandSource consoleCommandSource();

  /**
   * Gets the {@link PluginManager} instance.
   *
   * @return the plugin manager instance
   */
  PluginManager pluginManager();

  /**
   * Gets the {@link EventManager} instance.
   *
   * @return the event manager instance
   */
  EventManager eventManager();

  /**
   * Gets the {@link CommandManager} instance.
   *
   * @return the command manager
   */
  CommandManager commandManager();

  /**
   * Gets the {@link Scheduler} instance.
   *
   * @return the scheduler instance
   */
  Scheduler scheduler();

  /**
   * Gets the {@link ChannelRegistrar} instance.
   *
   * @return the channel registrar
   */
  ChannelRegistrar channelRegistrar();

  /**
   * Gets the {@link ProxyConfig} instance.
   *
   * @return the proxy config
   */
  ProxyConfig configuration();

  /**
   * Returns the version of the proxy.
   *
   * @return the proxy version
   */
  ProxyVersion version();

  /**
   * Returns all the endpoints the proxy is listening on. This collection is immutable.
   *
   * @return all the endpoints the proxy is listening on
   */
  Collection<NetworkEndpoint> endpoints();

}
