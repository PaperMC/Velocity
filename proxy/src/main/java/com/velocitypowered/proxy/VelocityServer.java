package com.velocitypowered.proxy;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyReloadEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.PluginManager;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import com.velocitypowered.api.util.Favicon;
import com.velocitypowered.api.util.GameProfile;
import com.velocitypowered.api.util.ProxyVersion;
import com.velocitypowered.proxy.command.ServerCommand;
import com.velocitypowered.proxy.command.ShutdownCommand;
import com.velocitypowered.proxy.command.VelocityCommand;
import com.velocitypowered.proxy.command.VelocityCommandManager;
import com.velocitypowered.proxy.config.AnnotatedConfig;
import com.velocitypowered.proxy.config.VelocityConfiguration;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.console.VelocityConsole;
import com.velocitypowered.proxy.network.ConnectionManager;
import com.velocitypowered.proxy.network.http.NettyHttpClient;
import com.velocitypowered.proxy.plugin.VelocityEventManager;
import com.velocitypowered.proxy.plugin.VelocityPluginManager;
import com.velocitypowered.proxy.protocol.packet.Chat;
import com.velocitypowered.proxy.protocol.util.FaviconSerializer;
import com.velocitypowered.proxy.protocol.util.GameProfileSerializer;
import com.velocitypowered.proxy.scheduler.VelocityScheduler;
import com.velocitypowered.proxy.server.ServerMap;
import com.velocitypowered.proxy.util.AddressUtil;
import com.velocitypowered.proxy.util.EncryptionUtils;
import com.velocitypowered.proxy.util.VelocityChannelRegistrar;
import com.velocitypowered.proxy.util.ratelimit.Ratelimiter;
import com.velocitypowered.proxy.util.ratelimit.Ratelimiters;
import io.netty.bootstrap.Bootstrap;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.serializer.GsonComponentSerializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.RequiresNonNull;

public class VelocityServer implements ProxyServer {

  private static final Logger logger = LogManager.getLogger(VelocityServer.class);
  public static final Gson GSON = new GsonBuilder()
      .registerTypeHierarchyAdapter(Component.class, new GsonComponentSerializer())
      .registerTypeHierarchyAdapter(Favicon.class, new FaviconSerializer())
      .registerTypeHierarchyAdapter(GameProfile.class, new GameProfileSerializer())
      .create();

  private final ConnectionManager cm;
  private final ProxyOptions options;
  private @MonotonicNonNull VelocityConfiguration configuration;
  private @MonotonicNonNull NettyHttpClient httpClient;
  private @MonotonicNonNull KeyPair serverKeyPair;
  private final ServerMap servers;
  private final VelocityCommandManager commandManager = new VelocityCommandManager();
  private final AtomicBoolean shutdownInProgress = new AtomicBoolean(false);
  private boolean shutdown = false;
  private final VelocityPluginManager pluginManager;

  private final Map<UUID, ConnectedPlayer> connectionsByUuid = new ConcurrentHashMap<>();
  private final Map<String, ConnectedPlayer> connectionsByName = new ConcurrentHashMap<>();
  private final VelocityConsole console;
  private @MonotonicNonNull Ratelimiter ipAttemptLimiter;
  private final VelocityEventManager eventManager;
  private final VelocityScheduler scheduler;
  private final VelocityChannelRegistrar channelRegistrar = new VelocityChannelRegistrar();

  VelocityServer(final ProxyOptions options) {
    pluginManager = new VelocityPluginManager(this);
    eventManager = new VelocityEventManager(pluginManager);
    scheduler = new VelocityScheduler(pluginManager);
    console = new VelocityConsole(this);
    cm = new ConnectionManager(this);
    servers = new ServerMap(this);
    this.options = options;
  }

  public KeyPair getServerKeyPair() {
    if (serverKeyPair == null) {
      throw new AssertionError();
    }
    return serverKeyPair;
  }

  public VelocityConfiguration getConfiguration() {
    VelocityConfiguration cfg = this.configuration;
    if (cfg == null) {
      throw new IllegalStateException("Configuration not initialized!");
    }
    return cfg;
  }

  @Override
  public ProxyVersion getVersion() {
    Package pkg = VelocityServer.class.getPackage();
    String implName;
    String implVersion;
    String implVendor;
    if (pkg != null) {
      implName = MoreObjects.firstNonNull(pkg.getImplementationTitle(), "Velocity");
      implVersion = MoreObjects.firstNonNull(pkg.getImplementationVersion(), "<unknown>");
      implVendor = MoreObjects.firstNonNull(pkg.getImplementationVendor(), "Velocity Contributors");
    } else {
      implName = "Velocity";
      implVersion = "<unknown>";
      implVendor = "Velocity Contributors";
    }

    return new ProxyVersion(implName, implVendor, implVersion);
  }

  @Override
  public VelocityCommandManager getCommandManager() {
    return commandManager;
  }

  @EnsuresNonNull({"serverKeyPair", "servers", "pluginManager", "eventManager", "scheduler",
      "console", "cm", "configuration"})
  public void start() {
    logger.info("Booting up {} {}...", getVersion().getName(), getVersion().getVersion());

    serverKeyPair = EncryptionUtils.createRsaKeyPair(1024);

    cm.logChannelInformation();

    // Initialize commands first
    commandManager.register(new VelocityCommand(this), "velocity");
    commandManager.register(new ServerCommand(this), "server");
    commandManager.register(new ShutdownCommand(this), "shutdown", "end");

    try {
      Path configPath = Paths.get("velocity.toml");
      configuration = VelocityConfiguration.read(configPath);

      AnnotatedConfig
              .saveConfig(configuration.dumpConfig(), configPath); // Resave config to add new values

      if (!configuration.validate()) {
        logger.error(
            "Your configuration is invalid. Velocity will refuse to start up until the errors are resolved.");
        LogManager.shutdown();
        System.exit(1);
      }
    } catch (Exception e) {
      logger.error("Unable to read/load/save your velocity.toml. The server will shut down.", e);
      LogManager.shutdown();
      System.exit(1);
    }

    for (Map.Entry<String, String> entry : configuration.getServers().entrySet()) {
      servers.register(new ServerInfo(entry.getKey(), AddressUtil.parseAddress(entry.getValue())));
    }

    ipAttemptLimiter = Ratelimiters.createWithMilliseconds(configuration.getLoginRatelimit());
    httpClient = new NettyHttpClient(this);
    loadPlugins();

    // Go ahead and fire the proxy initialization event. We block since plugins should have a chance
    // to fully initialize before we accept any connections to the server.
    eventManager.fire(new ProxyInitializeEvent()).join();

    // init console permissions after plugins are loaded
    console.setupPermissions();

    final Integer port = this.options.getPort();
    if (port != null) {
      logger.debug("Overriding bind port to {} from command line option", port);
      this.cm.bind(new InetSocketAddress(configuration.getBind().getHostString(), port));
    } else {
      this.cm.bind(configuration.getBind());
    }

    if (configuration.isQueryEnabled()) {
      this.cm.queryBind(configuration.getBind().getHostString(), configuration.getQueryPort());
    }
  }

  @RequiresNonNull({"pluginManager", "eventManager"})
  private void loadPlugins() {
    logger.info("Loading plugins...");

    try {
      Path pluginPath = Paths.get("plugins");

      if (!pluginPath.toFile().exists()) {
        Files.createDirectory(pluginPath);
      } else {
        if (!pluginPath.toFile().isDirectory()) {
          logger.warn("Plugin location {} is not a directory, continuing without loading plugins",
              pluginPath);
          return;
        }

        pluginManager.loadPlugins(pluginPath);
      }
    } catch (Exception e) {
      logger.error("Couldn't load plugins", e);
    }

    // Register the plugin main classes so that we can fire the proxy initialize event
    for (PluginContainer plugin : pluginManager.getPlugins()) {
      Optional<?> instance = plugin.getInstance();
      if (instance.isPresent()) {
        try {
          eventManager.register(instance.get(), instance.get());
        } catch (Exception e) {
          logger.error("Unable to register plugin listener for {}",
              plugin.getDescription().getName(), e);
        }
      }
    }

    logger.info("Loaded {} plugins", pluginManager.getPlugins().size());
  }

  public Bootstrap initializeGenericBootstrap() {
    return this.cm.createWorker();
  }

  public boolean isShutdown() {
    return shutdown;
  }

  public boolean reloadConfiguration() throws IOException {
    Path configPath = Paths.get("velocity.toml");
    VelocityConfiguration newConfiguration = VelocityConfiguration.read(configPath);

    if (!newConfiguration.validate()) {
      return false;
    }

    // Re-register servers. If a server is being replaced, make sure to note what players need to
    // move back to a fallback server.
    Collection<ConnectedPlayer> evacuate = new ArrayList<>();
    for (Map.Entry<String, String> entry : newConfiguration.getServers().entrySet()) {
      ServerInfo newInfo =
          new ServerInfo(entry.getKey(), AddressUtil.parseAddress(entry.getValue()));
      Optional<RegisteredServer> rs = servers.getServer(entry.getKey());
      if (!rs.isPresent()) {
        servers.register(newInfo);
      } else if (!rs.get().getServerInfo().equals(newInfo)) {
        for (Player player : rs.get().getPlayersConnected()) {
          if (!(player instanceof ConnectedPlayer)) {
            throw new IllegalStateException("ConnectedPlayer not found for player " + player
                + " in server " + rs.get().getServerInfo().getName());
          }
          evacuate.add((ConnectedPlayer) player);
        }
        servers.unregister(rs.get().getServerInfo());
        servers.register(newInfo);
      }
    }

    // If we had any players to evacuate, let's move them now. Wait until they are all moved off.
    if (!evacuate.isEmpty()) {
      CountDownLatch latch = new CountDownLatch(evacuate.size());
      for (ConnectedPlayer player : evacuate) {
        Optional<RegisteredServer> next = player.getNextServerToTry();
        if (next.isPresent()) {
          player.createConnectionRequest(next.get()).connectWithIndication()
              .whenComplete((success, ex) -> {
                if (ex != null || success == null || !success) {
                  player.disconnect(TextComponent.of("Your server has been changed, but we could "
                      + "not move you to any fallback servers."));
                }
                latch.countDown();
              });
        } else {
          latch.countDown();
          player.disconnect(TextComponent.of("Your server has been changed, but we could "
              + "not move you to any fallback servers."));
        }
      }
      try {
        latch.await();
      } catch (InterruptedException e) {
        logger.error("Interrupted whilst moving players", e);
        Thread.currentThread().interrupt();
      }
    }

    // If we have a new bind address, bind to it
    if (!configuration.getBind().equals(newConfiguration.getBind())) {
      this.cm.bind(newConfiguration.getBind());
      this.cm.close(configuration.getBind());
    }

    if (configuration.isQueryEnabled() && (!newConfiguration.isQueryEnabled()
        || newConfiguration.getQueryPort() != configuration.getQueryPort())) {
      this.cm.close(new InetSocketAddress(
          configuration.getBind().getHostString(), configuration.getQueryPort()));
    }

    if (newConfiguration.isQueryEnabled()) {
      this.cm.queryBind(newConfiguration.getBind().getHostString(),
          newConfiguration.getQueryPort());
    }

    ipAttemptLimiter = Ratelimiters.createWithMilliseconds(newConfiguration.getLoginRatelimit());
    this.configuration = newConfiguration;
    eventManager.fireAndForget(new ProxyReloadEvent());
    return true;
  }

  public void shutdown(boolean explicitExit) {
    if (eventManager == null || pluginManager == null || cm == null || scheduler == null) {
      throw new AssertionError();
    }

    if (!shutdownInProgress.compareAndSet(false, true)) {
      return;
    }
    logger.info("Shutting down the proxy...");

    for (ConnectedPlayer player : ImmutableList.copyOf(connectionsByUuid.values())) {
      player.disconnect(TextComponent.of("Proxy shutting down."));
    }

    this.cm.shutdown();

    eventManager.fire(new ProxyShutdownEvent());
    try {
      if (!eventManager.shutdown() || !scheduler.shutdown()) {
        logger.error("Your plugins took over 10 seconds to shut down.");
      }
    } catch (InterruptedException e) {
      // Not much we can do about this...
      Thread.currentThread().interrupt();
    }

    shutdown = true;

    if (explicitExit) {
      System.exit(0);
    }
  }

  public NettyHttpClient getHttpClient() {
    if (httpClient == null) {
      throw new IllegalStateException("HTTP client not initialized");
    }
    return httpClient;
  }

  public Ratelimiter getIpAttemptLimiter() {
    if (ipAttemptLimiter == null) {
      throw new IllegalStateException("Ratelimiter not initialized");
    }
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
  public void broadcast(Component component) {
    Preconditions.checkNotNull(component, "component");
    Chat chat = Chat.createClientbound(component);
    for (ConnectedPlayer player : connectionsByUuid.values()) {
      player.getConnection().write(chat);
    }
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
  public Optional<RegisteredServer> getServer(String name) {
    return servers.getServer(name);
  }

  @Override
  public Collection<RegisteredServer> getAllServers() {
    return servers.getAllServers();
  }

  @Override
  public RegisteredServer registerServer(ServerInfo server) {
    return servers.register(server);
  }

  @Override
  public void unregisterServer(ServerInfo server) {
    servers.unregister(server);
  }

  @Override
  public VelocityConsole getConsoleCommandSource() {
    return console;
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

  @Override
  public InetSocketAddress getBoundAddress() {
    if (configuration == null) {
      throw new IllegalStateException(
          "No configuration"); // even though you'll never get the chance... heh, heh
    }
    return configuration.getBind();
  }
}
