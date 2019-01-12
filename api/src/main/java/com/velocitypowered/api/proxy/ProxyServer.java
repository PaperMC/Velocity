package com.velocitypowered.api.proxy;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.plugin.PluginManager;
import com.velocitypowered.api.proxy.config.ProxyConfig;
import com.velocitypowered.api.proxy.messages.ChannelRegistrar;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import com.velocitypowered.api.scheduler.Scheduler;
import com.velocitypowered.api.util.ProxyVersion;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import net.kyori.text.Component;

/**
 * Provides an interface to a Minecraft server proxy.
 */
public abstract class ProxyServer {

  private static ProxyServer instance;

  public static void setInstance(ProxyServer instance) {
    Preconditions.checkState(ProxyServer.instance == null, "Cannot redefine singleton ProxyServer");
    Preconditions.checkNotNull(instance, "instance");
    ProxyServer.instance = instance;
  }

  /**
   * Returns an instance of running {@link ProxyServer}
   */
  public static ProxyServer getInstance() {
    return instance;
  }

  /**
   * Retrieves the player currently connected to this proxy by their Minecraft username. The search
   * is case-insensitive.
   *
   * @param username the username to search for
   * @return an {@link Optional} with the player, which may be empty
   */
  public abstract Optional<Player> getPlayer(String username);

  /**
   * Retrieves the player currently connected to this proxy by their Minecraft UUID.
   *
   * @param uuid the UUID
   * @return an {@link Optional} with the player, which may be empty
   */
  public abstract Optional<Player> getPlayer(UUID uuid);

  /**
   * Broadcasts a message to all players currently online.
   *
   * @param component the message to send
   */
  public abstract void broadcast(Component component);

  /**
   * Retrieves all players currently connected to this proxy. This call may or may not be a snapshot
   * of all players online.
   *
   * @return the players online on this proxy
   */
  public abstract Collection<Player> getAllPlayers();

  /**
   * Returns the number of players currently connected to this proxy.
   *
   * @return the players on this proxy
   */
  public abstract int getPlayerCount();

  /**
   * Retrieves a registered {@link RegisteredServer} instance by its name. The search is
   * case-insensitive.
   *
   * @param name the name of the server
   * @return the registered server, which may be empty
   */
  public abstract Optional<RegisteredServer> getServer(String name);

  /**
   * Retrieves all {@link RegisteredServer}s registered with this proxy.
   *
   * @return the servers registered with this proxy
   */
  public abstract Collection<RegisteredServer> getAllServers();

  /**
   * Registers a server with this proxy. A server with this name should not already exist.
   *
   * @param server the server to register
   * @return the newly registered server
   */
  public abstract RegisteredServer registerServer(ServerInfo server);

  /**
   * Unregisters this server from the proxy.
   *
   * @param server the server to unregister
   */
  public abstract void unregisterServer(ServerInfo server);

  /**
   * Returns an instance of {@link CommandSource} that can be used to determine if the command is
   * being invoked by the console or a console-like executor. Plugins that execute commands are
   * strongly urged to implement their own {@link CommandSource} instead of using the console
   * invoker.
   *
   * @return the console command invoker
   */
  public abstract ConsoleCommandSource getConsoleCommandSource();

  /**
   * Gets the {@link PluginManager} instance.
   *
   * @return the plugin manager instance
   */
  public abstract PluginManager getPluginManager();

  /**
   * Gets the {@link EventManager} instance.
   *
   * @return the event manager instance
   */
  public abstract EventManager getEventManager();

  /**
   * Gets the {@link CommandManager} instance.
   *
   * @return the command manager
   */
  public abstract CommandManager getCommandManager();

  /**
   * Gets the {@link Scheduler} instance.
   *
   * @return the scheduler instance
   */
  public abstract Scheduler getScheduler();

  /**
   * Gets the {@link ChannelRegistrar} instance.
   *
   * @return the channel registrar
   */
  public abstract ChannelRegistrar getChannelRegistrar();

  /**
   * Gets the address that this proxy is bound to. This does not necessarily indicate the external
   * IP address of the proxy.
   *
   * @return the address the proxy is bound to
   */
  public abstract InetSocketAddress getBoundAddress();

  /**
   * Gets the {@link ProxyConfig} instance.
   *
   * @return the proxy config
   */
  public abstract ProxyConfig getConfiguration();

  /**
   * Returns the version of the proxy.
   *
   * @return the proxy version
   */
  public abstract ProxyVersion getVersion();
}
