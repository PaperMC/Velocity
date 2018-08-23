package com.velocitypowered.api.proxy;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.plugin.PluginManager;
import com.velocitypowered.api.proxy.messages.ChannelRegistrar;
import com.velocitypowered.api.scheduler.Scheduler;
import com.velocitypowered.api.server.ServerInfo;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * Represents a Minecraft proxy server that is compatible with the Velocity API.
 */
public interface ProxyServer {
    /**
     * Retrieves the player currently connected to this proxy by their Minecraft username.
     * @param username the username
     * @return an {@link Optional} with the player
     */
    Optional<Player> getPlayer(String username);

    /**
     * Retrieves the player currently connected to this proxy by their Minecraft UUID.
     * @param uuid the UUID
     * @return an {@link Optional} with the player
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
     * Retrieves a registered {@link ServerInfo} instance by its name.
     * @param name the name of the server
     * @return the server
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
}
