package com.velocitypowered.proxy;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.util.Favicon;
import com.velocitypowered.api.plugin.PluginManager;
import com.velocitypowered.api.proxy.server.ServerInfo;
import com.velocitypowered.proxy.network.ConnectionManager;
import com.velocitypowered.proxy.command.ServerCommand;
import com.velocitypowered.proxy.command.ShutdownCommand;
import com.velocitypowered.proxy.command.VelocityCommand;
import com.velocitypowered.proxy.config.VelocityConfiguration;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.connection.http.NettyHttpClient;
import com.velocitypowered.proxy.command.VelocityCommandManager;
import com.velocitypowered.proxy.config.AnnotatedConfig;
import com.velocitypowered.proxy.messages.VelocityChannelRegistrar;
import com.velocitypowered.proxy.plugin.VelocityEventManager;
import com.velocitypowered.proxy.protocol.util.FaviconSerializer;
import com.velocitypowered.proxy.plugin.VelocityPluginManager;
import com.velocitypowered.proxy.scheduler.VelocityScheduler;
import com.velocitypowered.proxy.util.AddressUtil;
import com.velocitypowered.proxy.util.EncryptionUtils;
import com.velocitypowered.proxy.util.Ratelimiter;
import com.velocitypowered.proxy.util.ServerMap;
import io.netty.bootstrap.Bootstrap;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.serializer.ComponentSerializers;
import net.kyori.text.serializer.GsonComponentSerializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

public class VelocityServer implements ProxyServer {

    private static final Logger logger = LogManager.getLogger(VelocityServer.class);
    public static final Gson GSON = new GsonBuilder()
            .registerTypeHierarchyAdapter(Component.class, new GsonComponentSerializer())
            .registerTypeHierarchyAdapter(Favicon.class, new FaviconSerializer())
            .create();

    private final ConnectionManager cm = new ConnectionManager(this);
    private VelocityConfiguration configuration;
    private NettyHttpClient httpClient;
    private KeyPair serverKeyPair;
    private final ServerMap servers = new ServerMap();
    private final VelocityCommandManager commandManager = new VelocityCommandManager();
    private final AtomicBoolean shutdownInProgress = new AtomicBoolean(false);
    private boolean shutdown = false;
    private final VelocityPluginManager pluginManager = new VelocityPluginManager(this);

    private final Map<UUID, ConnectedPlayer> connectionsByUuid = new ConcurrentHashMap<>();
    private final Map<String, ConnectedPlayer> connectionsByName = new ConcurrentHashMap<>();
    private final CommandSource consoleCommandSource = new CommandSource() {
        @Override
        public void sendMessage(Component component) {
            logger.info(ComponentSerializers.LEGACY.serialize(component));
        }

        @Override
        public boolean hasPermission(String permission) {
            return true;
        }
    };
    private Ratelimiter ipAttemptLimiter;
    private VelocityEventManager eventManager;
    private VelocityScheduler scheduler;
    private VelocityChannelRegistrar channelRegistrar;

    VelocityServer() {
        commandManager.register(new VelocityCommand(), "velocity");
        commandManager.register(new ServerCommand(this), "server");
        commandManager.register(new ShutdownCommand(this), "shutdown", "end");
    }

    public KeyPair getServerKeyPair() {
        return serverKeyPair;
    }

    public VelocityConfiguration getConfiguration() {
        return configuration;
    }

    @Override
    public VelocityCommandManager getCommandManager() {
        return commandManager;
    }

    public void start() {
        try {
            Path configPath = Paths.get("velocity.toml");
            configuration = VelocityConfiguration.read(configPath);

            if (!configuration.validate()) {
                logger.error("Your configuration is invalid. Velocity will refuse to start up until the errors are resolved.");
                LogManager.shutdown();
                System.exit(1);
            }

            AnnotatedConfig.saveConfig(configuration.dumpConfig(), configPath); //Resave config to add new values

        } catch (IOException | RuntimeException e) {
            logger.error("Unable to load your velocity.toml. The server will shut down.", e);
            LogManager.shutdown();
            System.exit(1);
        }

        for (Map.Entry<String, String> entry : configuration.getServers().entrySet()) {
            servers.register(new ServerInfo(entry.getKey(), AddressUtil.parseAddress(entry.getValue())));
        }

        serverKeyPair = EncryptionUtils.createRsaKeyPair(1024);
        ipAttemptLimiter = new Ratelimiter(configuration.getLoginRatelimit());
        httpClient = new NettyHttpClient(this);
        eventManager = new VelocityEventManager(pluginManager);
        scheduler = new VelocityScheduler(pluginManager);
        channelRegistrar = new VelocityChannelRegistrar();
        loadPlugins();

        try {
            // Go ahead and fire the proxy initialization event. We block since plugins should have a chance
            // to fully initialize before we accept any connections to the server.
            eventManager.fire(new ProxyInitializeEvent()).get();
        } catch (InterruptedException | ExecutionException e) {
            // Ignore, we don't care. InterruptedException is unlikely to happen (and if it does, you've got bigger
            // issues) and there is almost no chance ExecutionException will be thrown.
        }

        this.cm.bind(configuration.getBind());

        if (configuration.isQueryEnabled()) {
            this.cm.queryBind(configuration.getBind().getHostString(), configuration.getQueryPort());
        }
    }

    private void loadPlugins() {
        logger.info("Loading plugins...");

        try {
            Path pluginPath = Paths.get("plugins");

            if (Files.notExists(pluginPath)) {
                Files.createDirectory(pluginPath);
            } else {
                if (!Files.isDirectory(pluginPath)) {
                    logger.warn("Plugin location {} is not a directory, continuing without loading plugins", pluginPath);
                    return;
                }

                pluginManager.loadPlugins(pluginPath);
            }
        } catch (Exception e) {
            logger.error("Couldn't load plugins", e);
        }

        // Register the plugin main classes so that we may proceed with firing the proxy initialize event
        pluginManager.getPlugins().forEach(container -> {
            container.getInstance().ifPresent(plugin -> eventManager.register(plugin, plugin));
        });

        logger.info("Loaded {} plugins", pluginManager.getPlugins().size());
    }

    public ServerMap getServers() {
        return servers;
    }

    public Bootstrap initializeGenericBootstrap() {
        return this.cm.createWorker();
    }

    public boolean isShutdown() {
        return shutdown;
    }

    public void shutdown() {
        if (!shutdownInProgress.compareAndSet(false, true)) {
            return;
        }
        logger.info("Shutting down the proxy...");

        for (ConnectedPlayer player : ImmutableList.copyOf(connectionsByUuid.values())) {
            player.close(TextComponent.of("Proxy shutting down."));
        }

        this.cm.shutdown();

        eventManager.fire(new ProxyShutdownEvent());
        try {
            if (!eventManager.shutdown() || !scheduler.shutdown()) {
                logger.error("Your plugins took over 10 seconds to shut down.");
            }
        } catch (InterruptedException e) {
            // Not much we can do about this...
        }

        shutdown = true;
    }

    public NettyHttpClient getHttpClient() {
        return httpClient;
    }

    public Ratelimiter getIpAttemptLimiter() {
        return ipAttemptLimiter;
    }

    public boolean registerConnection(ConnectedPlayer connection) {
        String lowerName = connection.getUsername().toLowerCase(Locale.US);
        if (connectionsByName.putIfAbsent(lowerName, connection) != null) {
            return false;
        }
        if (connectionsByUuid.putIfAbsent(connection.getUniqueId(), connection) != null) {
            connectionsByName.remove(lowerName, connection);
            return false;
        }
        return true;
    }

    public void unregisterConnection(ConnectedPlayer connection) {
        connectionsByName.remove(connection.getUsername().toLowerCase(Locale.US), connection);
        connectionsByUuid.remove(connection.getUniqueId(), connection);
    }

    @Override
    public Optional<Player> getPlayer(String username) {
        Preconditions.checkNotNull(username, "username");
        return Optional.ofNullable(connectionsByName.get(username.toLowerCase(Locale.US)));
    }

    @Override
    public Optional<Player> getPlayer(UUID uuid) {
        Preconditions.checkNotNull(uuid, "uuid");
        return Optional.ofNullable(connectionsByUuid.get(uuid));
    }

    @Override
    public Collection<Player> getAllPlayers() {
        return ImmutableList.copyOf(connectionsByUuid.values());
    }

    @Override
    public int getPlayerCount() {
        return connectionsByUuid.size();
    }

    @Override
    public Optional<ServerInfo> getServerInfo(String name) {
        Preconditions.checkNotNull(name, "name");
        return servers.getServer(name);
    }

    @Override
    public Collection<ServerInfo> getAllServers() {
        return servers.getAllServers();
    }

    @Override
    public void registerServer(ServerInfo server) {
        servers.register(server);
    }

    @Override
    public void unregisterServer(ServerInfo server) {
        servers.unregister(server);
    }

    @Override
    public CommandSource getConsoleCommandSource() {
        return consoleCommandSource;
    }

    @Override
    public PluginManager getPluginManager() {
        return pluginManager;
    }

    @Override
    public EventManager getEventManager() {
        return eventManager;
    }

    @Override
    public VelocityScheduler getScheduler() {
        return scheduler;
    }

    @Override
    public VelocityChannelRegistrar getChannelRegistrar() {
        return channelRegistrar;
    }
}
