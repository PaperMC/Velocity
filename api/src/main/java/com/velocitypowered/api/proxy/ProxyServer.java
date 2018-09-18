package com.velocitypowered.api.proxy;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.plugin.PluginManager;
import com.velocitypowered.api.proxy.messages.ChannelRegistrar;
import com.velocitypowered.api.scheduler.Scheduler;
import com.velocitypowered.api.proxy.server.ServerInfo;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * Provides an interface to a Minecraft server proxy.
 */
public interface ProxyServer {
    /**
     * Retrieves the player currently connected to this proxy by their Minecraft username. The search is case-insensitive.
     * @param username the username to search for
     * @return an {@link Optional} with the player, which may be empty
     */
    Optional<Player> getPlayer(String username);

    /**
     * Retrieves the player currently connected to this proxy by their Minecraft UUID.
     * @param uuid the UUID
     * @return an {@link Optional} with the player, which may be empty
     */
    Optional<Player> getPlayer(UUID uuid);

    /**
     * Retrieves all players currently connected to this proxy. This call may or may not be a snapshot of all players
     * online.
     * @return the players online on this proxy
     */
    Collection<Player> getAllPlayers();

    /**
     * Returns the number of players currently connected to this proxy.
     * @return the players on this proxy
     */
    int getPlayerCount();

    /**
     * Returns the maximum number of players allowed on the server
     * @return the maximum player count
     */
    int getMaxPlayerCount();

    /**
     * Returns the Message of the Day for the server.
     * @return the motd
     * @return
     */
    String getMotd();

    /**
     * Retrieves a registered {@link ServerInfo} instance by its name. The search is case-insensitive.
     * @param name the name of the server
     * @return the registered server, which may be empty
     */
    Optional<ServerInfo> getServerInfo(String name);

    /**
     * Retrieves all {@link ServerInfo}s registered with this proxy.
     * @return the servers registered with this proxy
     */
    Collection<ServerInfo> getAllServers();

    /**
     * Registers a server with this proxy. A server with this name should not already exist.
     * @param server the server to register
     */
    void registerServer(ServerInfo server);

    /**
     * Unregisters this server from the proxy.
     * @param server the server to unregister
     */
    void unregisterServer(ServerInfo server);

    /**
     * Returns an instance of {@link CommandSource} that can be used to determine if the command is being invoked by
     * the console or a console-like executor. Plugins that execute commands are strongly urged to implement their own
     * {@link CommandSource} instead of using the console invoker.
     * @return the console command invoker
     */
    CommandSource getConsoleCommandSource();

    /**
     * Gets the {@link PluginManager} instance.
     *
     * @return the plugin manager instance
     */
    PluginManager getPluginManager();

    /**
     * Gets the {@link EventManager} instance.
     *
     * @return the event manager instance
     */
    EventManager getEventManager();

    /**
     * Gets the {@link CommandManager} instance.
     * @return the command manager
     */
    CommandManager getCommandManager();

    /**
     * Gets the {@link Scheduler} instance.
     * @return the scheduler instance
     */
    Scheduler getScheduler();

    /**
     * Gets the {@link ChannelRegistrar} instance.
     * @return the channel registrar
     */
    ChannelRegistrar getChannelRegistrar();

    /**
     * Gets the address that this proxy is bound to. This does not necessarily indicate the external IP address of the
     * proxy.
     * @return the address the proxy is bound to
     */
    InetSocketAddress getBoundAddress();
}
