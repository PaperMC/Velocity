package com.velocitypowered.proxy;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.velocitypowered.api.plugin.PluginManager;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.server.ServerInfo;
import com.velocitypowered.natives.util.Natives;
import com.velocitypowered.network.ConnectionManager;
import com.velocitypowered.proxy.config.VelocityConfiguration;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.connection.http.NettyHttpClient;
import com.velocitypowered.proxy.plugin.VelocityPluginManager;
import com.velocitypowered.proxy.util.AddressUtil;
import com.velocitypowered.proxy.util.EncryptionUtils;
import com.velocitypowered.proxy.util.ServerMap;
import io.netty.bootstrap.Bootstrap;
import net.kyori.text.Component;
import net.kyori.text.serializer.GsonComponentSerializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class VelocityServer implements ProxyServer {
    private static final Logger logger = LogManager.getLogger(VelocityServer.class);
    private static final VelocityServer INSTANCE = new VelocityServer();
    public static final Gson GSON = new GsonBuilder()
            .registerTypeHierarchyAdapter(Component.class, new GsonComponentSerializer())
            .create();

    private final ConnectionManager cm = new ConnectionManager();
    private VelocityConfiguration configuration;
    private NettyHttpClient httpClient;
    private KeyPair serverKeyPair;
    private final ServerMap servers = new ServerMap();
    private final VelocityPluginManager pluginManager = new VelocityPluginManager(this);

    private final Map<UUID, ConnectedPlayer> connectionsByUuid = new ConcurrentHashMap<>();
    private final Map<String, ConnectedPlayer> connectionsByName = new ConcurrentHashMap<>();

    private VelocityServer() {
    }

    public static VelocityServer getServer() {
        return INSTANCE;
    }

    public KeyPair getServerKeyPair() {
        return serverKeyPair;
    }

    public VelocityConfiguration getConfiguration() {
        return configuration;
    }

    public void start() {
        logger.info("Using {}", Natives.compressor.getLoadedVariant());
        logger.info("Using {}", Natives.cipher.getLoadedVariant());

        // Create a key pair
        logger.info("Booting up Velocity...");
        try {
            Path configPath = Paths.get("velocity.toml");
            try {
                configuration = VelocityConfiguration.read(configPath);
            } catch (NoSuchFileException e) {
                logger.info("No velocity.toml found, creating one for you...");
                Files.copy(VelocityServer.class.getResourceAsStream("/velocity.toml"), configPath);
                configuration = VelocityConfiguration.read(configPath);
            }

            if (!configuration.validate()) {
                logger.error("Your configuration is invalid. Velocity will refuse to start up until the errors are resolved.");
                System.exit(1);
            }
        } catch (IOException e) {
            logger.error("Unable to load your velocity.toml. The server will shut down.", e);
            System.exit(1);
        }

        for (Map.Entry<String, String> entry : configuration.getServers().entrySet()) {
            servers.register(new ServerInfo(entry.getKey(), AddressUtil.parseAddress(entry.getValue())));
        }

        serverKeyPair = EncryptionUtils.createRsaKeyPair(1024);

        httpClient = new NettyHttpClient(this);

        loadPlugins();

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
                    logger.info("Plugin location {} is not a directory, continuing without loading plugins", pluginPath);
                    return;
                }

                pluginManager.loadPlugins(pluginPath);
            }
        } catch (Exception e) {
            logger.error("Couldn't load plugins", e);
        }

        logger.info("Loaded {} plugins", pluginManager.getPlugins().size());
    }

    public ServerMap getServers() {
        return servers;
    }

    public Bootstrap initializeGenericBootstrap() {
        return this.cm.createWorker();
    }

    public void shutdown() {
        this.cm.shutdown();
    }

    public NettyHttpClient getHttpClient() {
        return httpClient;
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
    public Optional<Player> getPlayer(@Nonnull String username) {
        Preconditions.checkNotNull(username, "username");
        return Optional.ofNullable(connectionsByName.get(username.toLowerCase(Locale.US)));
    }

    @Override
    public Optional<Player> getPlayer(@Nonnull UUID uuid) {
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
    public Optional<ServerInfo> getServerInfo(@Nonnull String name) {
        Preconditions.checkNotNull(name, "name");
        return servers.getServer(name);
    }

    @Override
    public Collection<ServerInfo> getAllServers() {
        return servers.getAllServers();
    }

    @Override
    public void registerServer(@Nonnull ServerInfo server) {
        servers.register(server);
    }

    @Override
    public void unregisterServer(@Nonnull ServerInfo server) {
        servers.unregister(server);
    }

    @Override
    public PluginManager getPluginManager() {
        return pluginManager;
    }
}
