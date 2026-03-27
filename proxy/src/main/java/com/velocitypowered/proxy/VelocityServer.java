/*
 * Copyright (C) 2018-2026 Velocity Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.velocitypowered.proxy;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.velocityctd.proxy.command.builtin.AlertCommand;
import com.velocityctd.proxy.command.builtin.AlertRawCommand;
import com.velocityctd.proxy.command.builtin.FindCommand;
import com.velocityctd.proxy.command.builtin.GipCommand;
import com.velocityctd.proxy.command.builtin.GkickCommand;
import com.velocityctd.proxy.command.builtin.HubCommand;
import com.velocityctd.proxy.command.builtin.LeaveQueueCommand;
import com.velocityctd.proxy.command.builtin.PingCommand;
import com.velocityctd.proxy.command.builtin.PlistCommand;
import com.velocityctd.proxy.command.builtin.ProxyAliasCommand;
import com.velocityctd.proxy.command.builtin.QueueAdminCommand;
import com.velocityctd.proxy.command.builtin.SlashServerCommand;
import com.velocityctd.proxy.command.builtin.TransferCommand;
import com.velocityctd.proxy.connection.profile.GameProfileFetcher;
import com.velocityctd.proxy.connection.profile.cache.MemoryGameProfileCache;
import com.velocityctd.proxy.queue.RedisVelocityQueueManager;
import com.velocityctd.proxy.queue.VelocityQueueManager;
import com.velocityctd.proxy.redis.VelocityRedis;
import com.velocityctd.proxy.redis.profilecache.RedisGameProfileCache;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.Command;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyPreShutdownEvent;
import com.velocitypowered.api.event.proxy.ProxyReloadEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.network.ProtocolState;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.PluginDescription;
import com.velocitypowered.api.plugin.PluginManager;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.config.BackendServerConfig;
import com.velocitypowered.api.proxy.player.ResourcePackInfo;
import com.velocitypowered.api.proxy.server.ServerInfo;
import com.velocitypowered.api.util.Favicon;
import com.velocitypowered.api.util.GameProfile;
import com.velocitypowered.api.util.ProxyVersion;
import com.velocitypowered.api.util.ServerLink;
import com.velocitypowered.proxy.command.VelocityCommandManager;
import com.velocitypowered.proxy.command.builtin.BuiltinCommand;
import com.velocitypowered.proxy.command.builtin.CallbackCommand;
import com.velocitypowered.proxy.command.builtin.GlistCommand;
import com.velocitypowered.proxy.command.builtin.SendCommand;
import com.velocitypowered.proxy.command.builtin.ServerCommand;
import com.velocitypowered.proxy.command.builtin.ShutdownCommand;
import com.velocitypowered.proxy.command.builtin.VelocityCommand;
import com.velocitypowered.proxy.config.DynamicProxyFilterMode;
import com.velocitypowered.proxy.config.ProxyAddress;
import com.velocitypowered.proxy.config.VelocityConfiguration;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.connection.player.resourcepack.VelocityResourcePackInfo;
import com.velocitypowered.proxy.connection.util.ServerListPingHandler;
import com.velocitypowered.proxy.console.VelocityConsole;
import com.velocitypowered.proxy.crypto.EncryptionUtils;
import com.velocitypowered.proxy.event.VelocityEventManager;
import com.velocitypowered.proxy.network.ConnectionManager;
import com.velocitypowered.proxy.plugin.VelocityPluginManager;
import com.velocitypowered.proxy.plugin.loader.VelocityPluginContainer;
import com.velocitypowered.proxy.plugin.loader.VelocityPluginDescription;
import com.velocitypowered.proxy.plugin.virtual.VelocityVirtualPlugin;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.util.FaviconSerializer;
import com.velocitypowered.proxy.protocol.util.GameProfileSerializer;
import com.velocitypowered.proxy.scheduler.VelocityScheduler;
import com.velocitypowered.proxy.server.ServerMap;
import com.velocitypowered.proxy.server.VelocityRegisteredServer;
import com.velocitypowered.proxy.util.AddressUtil;
import com.velocitypowered.proxy.util.VelocityChannelRegistrar;
import com.velocitypowered.proxy.util.ratelimit.Ratelimiter;
import com.velocitypowered.proxy.util.ratelimit.Ratelimiters;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.ForwardingAudience;
import net.kyori.adventure.text.Component;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bstats.MetricsBase;
import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.NotNull;

/**
 * Implementation of {@link ProxyServer}.
 */
@SuppressWarnings({"unchecked"})
public class VelocityServer implements ProxyServer, ForwardingAudience {

  /**
   * The official Velocity GitHub URL used in branding and virtual plugin metadata.
   */
  public static final String VELOCITY_URL = "https://github.com/GemstoneGG/Velocity-CTD";

  /**
   * Shared logger used throughout proxy lifecycle events.
   */
  private static final Logger LOGGER = LogManager.getLogger(VelocityServer.class);

  /**
   * Timeout in seconds for {@link ProxyPreShutdownEvent} listeners
   * before the proxy proceeds with shutdown. Configurable via the
   * {@code velocity.pre-shutdown-timeout} system property.
   */
  private static final int PRE_SHUTDOWN_TIMEOUT = Integer.getInteger("velocity.pre-shutdown-timeout", 10);

  /**
   * The primary Gson instance used for general JSON serialization tasks.
   */
  public static final Gson GENERAL_GSON = new GsonBuilder()
      .registerTypeHierarchyAdapter(Favicon.class, FaviconSerializer.INSTANCE)
      .registerTypeHierarchyAdapter(GameProfile.class, GameProfileSerializer.INSTANCE)
      .create();

  /**
   * A {@link Gson} instance for serializing server ping responses for Minecraft versions
   * before 1.16. Uses legacy chat component formatting.
   */
  private static final Gson PRE_1_16_PING_SERIALIZER = new GsonBuilder()
      .registerTypeHierarchyAdapter(
          Component.class,
          ProtocolUtils.getJsonChatSerializer(ProtocolVersion.MINECRAFT_1_15_2)
              .serializer().getAdapter(Component.class)
      )
      .registerTypeHierarchyAdapter(Favicon.class, FaviconSerializer.INSTANCE)
      .create();

  /**
   * A {@link Gson} instance for serializing server ping responses for Minecraft versions
   * between 1.16 and 1.20.2. Uses improved component formatting.
   */
  private static final Gson PRE_1_20_3_PING_SERIALIZER = new GsonBuilder()
      .registerTypeHierarchyAdapter(
          Component.class,
          ProtocolUtils.getJsonChatSerializer(ProtocolVersion.MINECRAFT_1_20_2)
              .serializer().getAdapter(Component.class)
      )
      .registerTypeHierarchyAdapter(Favicon.class, FaviconSerializer.INSTANCE)
      .create();

  /**
   * A {@link Gson} instance for serializing server ping responses for Minecraft 1.20.3 and newer.
   * Reflects modern component structure.
   */
  private static final Gson MODERN_PING_SERIALIZER = new GsonBuilder()
      .registerTypeHierarchyAdapter(
          Component.class,
          ProtocolUtils.getJsonChatSerializer(ProtocolVersion.MINECRAFT_1_20_3)
              .serializer().getAdapter(Component.class)
      )
      .registerTypeHierarchyAdapter(Favicon.class, FaviconSerializer.INSTANCE)
      .create();

  /**
   * Manages all active network connections including listeners and backend channels.
   */
  private final ConnectionManager cm;

  /**
   * The parsed command-line options used to configure the proxy at startup.
   */
  private final ProxyOptions options;

  /**
   * The loaded proxy configuration (velocity.toml).
   * Set during startup after validation.
   */
  private @MonotonicNonNull VelocityConfiguration configuration;

  /**
   * The RSA key pair used during the Mojang login encryption handshake.
   */
  private @MonotonicNonNull KeyPair serverKeyPair;

  /**
   * A registry of all backend servers known to the proxy.
   */
  private final ServerMap servers;

  /**
   * The command manager responsible for registering and dispatching Velocity commands.
   */
  private final VelocityCommandManager commandManager;

  /**
   * Atomic flag used to indicate whether shutdown is currently in progress.
   */
  private final AtomicBoolean shutdownInProgress = new AtomicBoolean(false);

  /**
   * Whether the proxy has fully completed shutdown.
   */
  private boolean shutdown = false;

  /**
   * Whether the shutdown sequence has officially begun.
   */
  private boolean startedShutdown = false;

  /**
   * Manages loaded plugins and handles plugin lifecycle events.
   */
  private final VelocityPluginManager pluginManager;

  /**
   * Maps online players by their UUID for fast lookup.
   */
  private final Map<UUID, ConnectedPlayer> connectionsByUuid = new ConcurrentHashMap<>();

  /**
   * Maps online players by their lowercase usernames.
   */
  private final Map<String, ConnectedPlayer> connectionsByName = new ConcurrentHashMap<>();

  /**
   * Holds a set of all registered BuiltinCommand instances. Used for unregistering these commands later.
   */
  private final Set<BuiltinCommand> registeredBuiltinCommands = new HashSet<>();

  /**
   * The proxy's console interface, providing command input and logging output.
   */
  private final VelocityConsole console;

  /**
   * Rate limiter for login attempts by IP address.
   */
  private @MonotonicNonNull Ratelimiter<InetAddress> ipAttemptLimiter;

  /**
   * Rate limiter for command usage by player UUID.
   */
  private @MonotonicNonNull Ratelimiter<UUID> commandRateLimiter;

  /**
   * Rate limiter for tab completion requests by player UUID.
   */
  private @MonotonicNonNull Ratelimiter<UUID> tabCompleteRateLimiter;

  /**
   * The event manager responsible for firing and dispatching plugin events.
   */
  private final VelocityEventManager eventManager;

  /**
   * Task scheduler for asynchronous work managed by the proxy or plugins.
   */
  private final VelocityScheduler scheduler;

  /**
   * Manages plugin messaging channels and handles namespaced vs legacy support.
   */
  private final VelocityChannelRegistrar channelRegistrar = new VelocityChannelRegistrar();

  /**
   * Handles Minecraft client pings to the proxy, including response formatting and favicon.
   */
  private final ServerListPingHandler serverListPingHandler;

  /**
   * The system timestamp (in milliseconds) when the proxy started.
   */
  private final long startTime;

  /**
   * The {@link TranslationRegistryManager} instance that manages (registers and unregisters)
   * Velocity's Adventure translations.
   */
  private final TranslationRegistryManager translationRegistryManager = new TranslationRegistryManager();

  /**
   * Coordinates server queues and handles queue assignment logic.
   */
  private @Nullable VelocityQueueManager queueManager;

  /**
   * Provides access to the Redis integration used for multi-proxy features such
   * as queues, dynamic proxy discovery, and global player tracking.
   */
  private @MonotonicNonNull VelocityRedis redis;

  /**
   * The global {@link GameProfileFetcher} used by {@link com.velocitypowered.proxy.connection.client.InitialLoginSessionHandler}.
   */
  private @MonotonicNonNull GameProfileFetcher gameProfileFetcher;

  VelocityServer(final ProxyOptions options) {
    pluginManager = new VelocityPluginManager(this);
    eventManager = new VelocityEventManager(pluginManager);
    commandManager = new VelocityCommandManager(eventManager, pluginManager);
    scheduler = new VelocityScheduler(pluginManager);
    console = new VelocityConsole(this);
    cm = new ConnectionManager(this);
    servers = new ServerMap(this);
    startTime = System.currentTimeMillis();
    serverListPingHandler = new ServerListPingHandler(this);
    this.options = options;
  }

  /**
   * Returns the RSA key pair used for player encryption handshakes.
   *
   * @return the proxy's {@link KeyPair}, or {@code null} before startup
   */
  public KeyPair getServerKeyPair() {
    return serverKeyPair;
  }

  /**
   * Returns the queue manager currently in use.
   * Will throw when the queue is not enabled. Please check this beforehand with {@link #isQueueEnabled()}.
   *
   * @return the {@link VelocityQueueManager}, or throws {@link IllegalStateException} if not initialized
   */
  @Override
  public VelocityQueueManager getQueueManager() {
    if (queueManager == null) {
      throw new IllegalStateException("Queue is not enabled.");
    }

    return queueManager;
  }

  /**
   * Returns the {@link VelocityRedis} instance for interacting with Redis features.
   *
   * @return the {@link VelocityRedis} instance
   */
  public VelocityRedis getRedis() {
    return redis;
  }

  public @MonotonicNonNull GameProfileFetcher getGameProfileFetcher() {
    return gameProfileFetcher;
  }

  @Override
  public final VelocityConfiguration getConfiguration() {
    return this.configuration;
  }

  /**
   * Gets the system timestamp (in milliseconds) when the proxy started.
   *
   * @return the proxy startup time
   */
  public long getStartTime() {
    return startTime;
  }

  /**
   * Returns the {@link ProxyVersion} instance representing the proxy's implementation metadata.
   *
   * <p>This includes the proxy's name, vendor, and version string as defined by the package metadata.
   * If package metadata is unavailable (e.g., in a dev environment), default values will be used.</p>
   *
   * @return a {@link ProxyVersion} describing the proxy's implementation
   */
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

  /**
   * Indicates whether the shutdown sequence has begun.
   *
   * @return {@code true} if shutdown has started, otherwise {@code false}
   */
  public boolean isStartedShutdown() {
    return this.startedShutdown;
  }

  private VelocityPluginContainer createVirtualPlugin() {
    ProxyVersion version = getVersion();
    PluginDescription description = new VelocityPluginDescription(
        "velocity", version.getName(), version.getVersion(), "The Velocity proxy",
            version.getName().equals("Velocity") ? VELOCITY_URL : null,
            ImmutableList.of(version.getVendor()), Collections.emptyList(), null);
    VelocityPluginContainer container = new VelocityPluginContainer(description);
    container.setInstance(VelocityVirtualPlugin.INSTANCE);
    return container;
  }

  /**
   * Returns the {@link VelocityCommandManager} responsible for handling Velocity's command framework.
   *
   * <p>This includes command registration, execution, Brigadier support, and alias management.</p>
   *
   * @return the {@link VelocityCommandManager} instance
   */
  @Override
  public VelocityCommandManager getCommandManager() {
    return commandManager;
  }

  /**
   * Blocks the current thread until the proxy has completed its shutdown process.
   *
   * <p>This method is typically invoked to wait on termination of the Netty event loop group
   * managing the server listeners.</p>
   */
  void awaitProxyShutdown() {
    cm.getBossGroup().terminationFuture().syncUninterruptibly();
  }

  /**
   * Starts the Velocity proxy, initializing all required systems including networking,
   * plugin management, configuration loading, and event dispatching.
   *
   * <p>This method should be called exactly once during proxy bootstrap. It prepares the proxy
   * to begin accepting player connections and enables plugin interactions.</p>
   *
   * <p>This method ensures that all critical fields (such as {@code serverKeyPair}, {@code scheduler},
   * {@code cm}, and {@code configuration}) are initialized before completing.</p>
   *
   * @throws RuntimeException if startup configuration is invalid or plugin loading fails
   */
  @EnsuresNonNull({"serverKeyPair", "servers", "pluginManager", "eventManager", "scheduler",
      "console", "cm", "configuration"})
  void start() {
    LOGGER.info("Booting up {} {}...", getVersion().getName(), getVersion().getVersion());
    console.setupStreams();
    pluginManager.registerPlugin(this.createVirtualPlugin());

    // Yes, you're reading that correctly. We're generating a 1024-bit RSA keypair. Sounds
    // dangerous, right? We're well within the realm of factoring such a key...
    //
    // You can blame Mojang. For the record, we also don't consider the Minecraft protocol
    // encryption scheme to be secure, and it has reached the point where any serious cryptographic
    // protocol needs a refresh. There are multiple obvious weaknesses, and this is far from the
    // most serious.
    //
    // If you are using Minecraft in a security-sensitive application, *I don't know what to say.*
    serverKeyPair = EncryptionUtils.createRsaKeyPair(1024);

    cm.logChannelInformation();

    this.doStartupConfigLoad();

    if (getConfiguration().getRedis().getProxyId() == null && getConfiguration().getRedis().isEnabled()) {
      throw new IllegalArgumentException("'proxy-id' cannot be null when redis is enabled!");
    }

    if ((getConfiguration().getQueue().getMasterProxyIds() == null
        || getConfiguration().getQueue().getMasterProxyIds().isEmpty()) && getConfiguration().getQueue().isEnabled()) {
      throw new IllegalArgumentException("'master-proxy-ids' cannot be empty when queues is enabled!");
    }

    for (ServerInfo cliServer : options.getServers()) {
      servers.register(cliServer);
    }

    if (!options.isIgnoreConfigServers()) {
      for (Map.Entry<String, BackendServerConfig> entry : configuration.getBackendServers().entrySet()) {
        servers.register(new ServerInfo(entry.getKey(), AddressUtil.parseAddress(entry.getValue().address()), entry.getValue().forwardingMode()));
      }
    }

    gameProfileFetcher = new GameProfileFetcher(this);
    if (configuration.isCachePlayerProfileResultEnabled() && configuration.isOnlineMode()) {
      LOGGER.debug("Registering memory profile cache");
      gameProfileFetcher.getCacheLayers().addFirst(new MemoryGameProfileCache(
          Duration.ofMinutes(configuration.getProfileCacheExpiryMinutes()),
          1_000
      ));
    }

    if (configuration.getRedis().isEnabled()) {
      redis = new VelocityRedis(this);

      if (configuration.isCachePlayerProfileResultEnabled() && configuration.isOnlineMode()) {
        LOGGER.debug("Registering Redis profile cache");
        gameProfileFetcher.getCacheLayers().addLast(new RedisGameProfileCache(
            redis.getProvider(),
            Duration.ofMinutes(configuration.getProfileCacheExpiryMinutes())
        ));
      }
    }

    if (configuration.getQueue().isEnabled()) {
      queueManager = isRedisEnabled()
          ? new RedisVelocityQueueManager(this)
          : new VelocityQueueManager(this);
    }

    registerCommands();

    LOGGER.info("Loading localizations...");
    translationRegistryManager.registerTranslations();

    ipAttemptLimiter = Ratelimiters.createWithMilliseconds(configuration.getLoginRatelimit());
    commandRateLimiter = Ratelimiters.createWithMilliseconds(configuration.getCommandRatelimit());
    tabCompleteRateLimiter = Ratelimiters.createWithMilliseconds(configuration.getTabCompleteRatelimit());
    loadPlugins();

    // Go ahead and fire the proxy initialization event. We block since plugins should have a chance
    // to fully initialize before we accept any connections to the server.
    eventManager.fire(new ProxyInitializeEvent()).join();

    // init console permissions after plugins are loaded
    console.setupPermissions();

    final Integer port = this.options.getPort();
    if (port != null) {
      LOGGER.debug("Overriding bind port to {} from command line option", port);
      this.cm.bind(new InetSocketAddress(configuration.getBind().getHostString(), port));
    } else {
      this.cm.bind(configuration.getBind());
    }

    final Boolean haproxy = this.options.isHaproxy();
    if (haproxy != null) {
      LOGGER.debug("Overriding HAProxy protocol to {} from command line option", haproxy);
      configuration.setProxyProtocol(haproxy);
    }

    if (configuration.isQueryEnabled()) {
      this.cm.queryBind(configuration.getBind().getHostString(), configuration.getQueryPort());
    }

    final String defaultPackage = new String(new byte[] {'o', 'r', 'g', '.', 'b', 's', 't', 'a', 't', 's' });
    if (!MetricsBase.class.getPackage().getName().startsWith(defaultPackage)) {
      Metrics.VelocityMetrics.startMetrics(this, configuration.getMetrics());
    } else {
      LOGGER.warn("debug environment, metrics is disabled!");
    }
  }

  @SuppressFBWarnings("DM_EXIT")
  private void doStartupConfigLoad() {
    try {
      Path configPath = Path.of("velocity.toml");
      configuration = VelocityConfiguration.read(configPath);

      if (!configuration.validate()) {
        LOGGER.error("Your configuration is invalid. Velocity will not start up until the errors "
            + "are resolved.");
        LogManager.shutdown();
        System.exit(1);
      }

      commandManager.setAnnounceProxyCommands(configuration.isAnnounceProxyCommands());
    } catch (Exception e) {
      LOGGER.error("Unable to read/load/save your velocity.toml. The server will shut down.", e);
      LogManager.shutdown();
      System.exit(1);
    }
  }

  private void loadPlugins() {
    LOGGER.info("Loading plugins...");

    try {
      Path pluginPath = Path.of("plugins");
      ArrayList<Path> additionalPlugins = new ArrayList<>();

      if (!pluginPath.toFile().exists()) {
        Files.createDirectory(pluginPath);
      } else {
        if (!pluginPath.toFile().isDirectory()) {
          LOGGER.warn("Plugin location {} is not a directory, continuing without loading plugins",
              pluginPath);
          return;
        }
      }

      for (String additionalPluginPath : options.getAdditionalPlugins()) {
        Path path = Path.of(additionalPluginPath);
        if (!Files.exists(path)) {
          LOGGER.warn("Unable to find plugin file by path {}", additionalPluginPath);
          continue;
        }

        if (!path.toFile().isFile()) {
          LOGGER.warn("Plugin {} is not a file", additionalPluginPath);
          continue;
        }

        additionalPlugins.add(path);
      }

      pluginManager.loadPlugins(pluginPath, additionalPlugins);
    } catch (Exception e) {
      LOGGER.error("Couldn't load plugins", e);
    }

    // Register the plugin main classes so that we can fire the proxy initialize event
    for (PluginContainer plugin : pluginManager.getPlugins()) {
      Optional<?> instance = plugin.getInstance();
      if (instance.isPresent()) {
        try {
          eventManager.registerInternally(plugin, instance.get());
        } catch (Exception e) {
          LOGGER.error("Unable to register plugin listener for {}",
              plugin.getDescription().getName().orElse(plugin.getDescription().getId()), e);
        }
      }
    }

    LOGGER.info("Loaded {} plugins", pluginManager.getPlugins().size());
  }

  /**
   * Creates a Netty {@link Bootstrap} instance for establishing new backend or client connections.
   *
   * @param group the Netty {@link EventLoopGroup} to use, or {@code null} to use the default
   * @return a configured {@link Bootstrap} for initiating connections
   */
  public Bootstrap createBootstrap(final @Nullable EventLoopGroup group) {
    return this.cm.createWorker(group);
  }

  /**
   * Returns the {@link ChannelInitializer} used for backend server connections.
   *
   * @return the backend {@link ChannelInitializer}
   */
  public ChannelInitializer<Channel> getBackendChannelInitializer() {
    return this.cm.backendChannelInitializer.get();
  }

  /**
   * Gets the {@link ServerListPingHandler} responsible for processing client pings to the proxy.
   *
   * @return the ping handler instance
   */
  public ServerListPingHandler getServerListPingHandler() {
    return serverListPingHandler;
  }

  /**
   * Returns whether the proxy has completed shutdown.
   *
   * @return {@code true} if shutdown is complete, otherwise {@code false}
   */
  public boolean isShutdown() {
    return shutdown;
  }

  /**
   * Reloads the proxy's configuration.
   *
   * @return {@code true} if successful, {@code false} if we can't read the configuration
   * @throws IOException if we can't read {@code velocity.toml}
   */
  public boolean reloadConfiguration() throws IOException {
    Path configPath = Path.of("velocity.toml");
    VelocityConfiguration newConfiguration = VelocityConfiguration.read(configPath);

    if (!newConfiguration.validate()) {
      return false;
    }

    unregisterCommands();

    this.configuration = newConfiguration;

    reloadServerList();

    registerCommands();

    translationRegistryManager.unregisterTranslations();

    translationRegistryManager.registerTranslations();

    // Re-register servers. If a server is being replaced, make sure to note what players need to
    // move back to a fallback server.
    Collection<ConnectedPlayer> evacuate = new ArrayList<>();
    for (Map.Entry<String, BackendServerConfig> entry : newConfiguration.getBackendServers().entrySet()) {
      ServerInfo newInfo = new ServerInfo(entry.getKey(), AddressUtil.parseAddress(entry.getValue().address()), entry.getValue().forwardingMode());
      Optional<VelocityRegisteredServer> rs = servers.getServer(entry.getKey());
      if (rs.isEmpty()) {
        servers.register(newInfo);
      } else if (!rs.get().getServerInfo().equals(newInfo)) {
        evacuate.addAll(rs.get().getPlayersConnected());

        servers.unregister(rs.get().getServerInfo());
        servers.register(newInfo);
      }
    }

    // If we had any players to evacuate, let's move them now. Wait until they are all moved off.
    if (!evacuate.isEmpty()) {
      CountDownLatch latch = new CountDownLatch(evacuate.size());
      for (ConnectedPlayer player : evacuate) {
        Optional<VelocityRegisteredServer> next = player.currentServerRetrySession().getNextServerToTry();
        if (next.isPresent()) {
          player.createConnectionRequest(next.get()).connectWithIndication()
              .whenComplete((success, ex) -> {
                if (ex != null || success == null || !success) {
                  player.disconnect(Component.text("Your server has been changed, but we could "
                      + "not move you to any fallback servers."));
                }
                latch.countDown();
              });
        } else {
          latch.countDown();
          player.disconnect(Component.text("Your server has been changed, but we could "
              + "not move you to any fallback servers."));
        }
      }
      try {
        latch.await();
      } catch (InterruptedException e) {
        LOGGER.error("Interrupted whilst moving players", e);
        Thread.currentThread().interrupt();
      }
    }

    // If we have a new bind address, bind to it
    if (!configuration.getBind().equals(newConfiguration.getBind())) {
      this.cm.bind(newConfiguration.getBind());
      this.cm.close(configuration.getBind());
    }

    boolean queryPortChanged = newConfiguration.getQueryPort() != configuration.getQueryPort();
    boolean queryAlreadyEnabled = configuration.isQueryEnabled();
    boolean queryEnabled = newConfiguration.isQueryEnabled();
    if (queryAlreadyEnabled && (!queryEnabled || queryPortChanged)) {
      this.cm.close(new InetSocketAddress(
          configuration.getBind().getHostString(), configuration.getQueryPort()));
    }
    if (queryEnabled && (!queryAlreadyEnabled || queryPortChanged)) {
      this.cm.queryBind(newConfiguration.getBind().getHostString(),
          newConfiguration.getQueryPort());
    }

    commandManager.setAnnounceProxyCommands(newConfiguration.isAnnounceProxyCommands());
    ipAttemptLimiter = Ratelimiters.createWithMilliseconds(newConfiguration.getLoginRatelimit());
    this.configuration = newConfiguration;
    eventManager.fireAndForget(new ProxyReloadEvent());

    if (queueManager != null) {
      queueManager.reload();
    }

    if (!this.getConfiguration().getServerLinks().isEmpty()) {
      for (ConnectedPlayer player : this.getAllPlayers()) {
        if (player.getProtocolVersion().noLessThan(ProtocolVersion.MINECRAFT_1_21)) {
          try {
            if (player.getProtocolState() == ProtocolState.CONFIGURATION || player.getProtocolState() == ProtocolState.PLAY) {
              String serverName = player.getCurrentServer().map(s -> s.getServerInfo().getName()).orElse("");
              List<ServerLink> scopedLinks = getConfiguration().getServerLinksFor(serverName);
              player.setServerLinks(scopedLinks);
            }
          } catch (IllegalStateException ignored) {
            // Ignore illegal state to ensure each viable reload is successful.
          }
        }
      }
    }

    return true;
  }

  private void unregisterCommands() {
    for (BuiltinCommand command : registeredBuiltinCommands) {
      unregisterCommand(command.label());
    }
    registeredBuiltinCommands.clear();

    for (String alias : configuration.getProxyCommandAliases().keySet()) {
      unregisterCommand(alias);
    }
  }

  private void unregisterCommand(final String command) {
    if (commandManager.getCommandMeta(command) != null
        && Objects.requireNonNull(commandManager.getCommandMeta(command)).getPlugin() instanceof VelocityCommandManager) {
      commandManager.unregister(command);
    }
  }

  private void registerCommands() {
    registerCommand(VelocityCommand::new);
    registerCommand(CallbackCommand::new);
    registerCommand(ShutdownCommand::new);
    registerCommand(configuration.isAlertEnabled(), AlertCommand::new);
    registerCommand(configuration.isAlertRawEnabled(), AlertRawCommand::new);
    registerCommand(configuration.isFindEnabled(), FindCommand::new);
    registerCommand(configuration.isGkickEnabled(), GkickCommand::new);
    registerCommand(configuration.isGipEnabled(), GipCommand::new);
    registerCommand(configuration.isTransferEnabled(), TransferCommand::new);
    registerCommand(configuration.isGlistEnabled(), GlistCommand::new);
    registerCommand(configuration.isPlistEnabled(), PlistCommand::new);
    registerCommand(configuration.isPingEnabled(), PingCommand::new);
    registerCommand(configuration.isSendEnabled(), SendCommand::new);
    registerCommand(configuration.isHubEnabled(), HubCommand::new);
    registerCommand(configuration.getQueue().isEnabled(), QueueAdminCommand::new);
    registerCommand(configuration.getQueue().isEnabled(), LeaveQueueCommand::new);
    registerCommand(configuration.isServerEnabled(), ServerCommand::new);

    // /<server_name> commands
    for (Map.Entry<String, List<String>> entry : configuration.getSlashServers().entrySet()) {
      String serverName = entry.getKey();
      List<String> commandLabels = entry.getValue();

      for (String commandLabel : commandLabels) {
        registerCommand(SlashServerCommand.factory(serverName, commandLabel));
      }
    }

    // Proxy command aliases
    for (Map.Entry<String, List<String>> entry : configuration.getProxyCommandAliases().entrySet()) {
      String alias = entry.getKey();
      List<String> commands = entry.getValue();

      if (commandManager.hasCommand(alias)) {
        LOGGER.warn("Proxy command alias '{}' conflicts with existing command, skipping", alias);
        continue;
      }

      ProxyAliasCommand proxyAliasCommand = new ProxyAliasCommand(this, alias, commands);
      commandManager.register(
          commandManager.metaBuilder(alias)
              .plugin(VelocityVirtualPlugin.INSTANCE)
              .build(),
          proxyAliasCommand
      );
    }
  }

  private void registerCommand(Function<VelocityServer, ? extends BuiltinCommand> commandConstructor) {
    BuiltinCommand command = commandConstructor.apply(this);
    if (commandManager.hasCommand(command.label())) {
      LOGGER.debug("Not registering built-in command /{}, command already exists.", command.label());
      return;
    }

    BrigadierCommand brigadierCommand = command.build();
    if (brigadierCommand == null) {
      LOGGER.debug("Not registering built-in command /{}, returned null.", command.label());
      return;
    }

    if (!brigadierCommand.getNode().getName().equals(command.label())) {
      throw new IllegalStateException("BuiltinCommand#label and BrigadierCommand node name mismatch.");
    }

    String[] aliases = findAliases(command);

    commandManager.register(
            commandManager.metaBuilder(brigadierCommand)
                    .aliases(aliases)
                    .plugin(VelocityVirtualPlugin.INSTANCE)
                    .build(),
            brigadierCommand
    );

    registeredBuiltinCommands.add(command);

    LOGGER.debug("Registered built-in command /{}", command.label());
  }

  private void registerCommand(boolean condition, Function<VelocityServer, ? extends BuiltinCommand> commandConstructor) {
    if (condition) {
      registerCommand(commandConstructor);
    }
  }

  private String[] findAliases(BuiltinCommand command) {
    List<String> aliases = new ArrayList<>(command.aliases());

    List<String> configuredAliases = configuration.getCommandAliases().get(command.label());
    if (configuredAliases != null) {
      aliases.addAll(configuredAliases);
    }

    return aliases.toArray(String[]::new);
  }

  /**
   * Reloads the list of servers based on the updated configuration.
   *
   * <p>This is exclusively implemented within VelocityServer as it
   * is not a function necessary and present for generic purposes
   * within ServerCommand and is exclusive to reload's functionality.</p>
   */
  public void reloadServerList() {
    VelocityConfiguration config = getConfiguration();
    List<ServerInfo> newConfigServers = loadServersFromNewList(config);

    getAllServers().forEach(server -> {
      if (!newConfigServers.contains(server.getServerInfo())) {
        unregisterServer(server.getServerInfo());
      }
    });

    newConfigServers.forEach(serverInfo -> {
      if (getServer(serverInfo.getName()).isEmpty()) {
        registerServer(serverInfo);
      }
    });
  }

  /**
   * Loads servers from the [servers] section of the configuration.
   *
   * @param config the Velocity configuration
   * @return list of configured ServerInfo objects
   */
  private static List<ServerInfo> loadServersFromNewList(final VelocityConfiguration config) {
    List<ServerInfo> serverList = new ArrayList<>();

    config.getServers().forEach((serverName, address) -> {
      InetSocketAddress socketAddress = AddressUtil.parseAddress(address);
      serverList.add(new ServerInfo(serverName, socketAddress));
    });

    return serverList;
  }

  /**
   * Shuts down the proxy, kicking players with the specified reason.
   *
   * @param explicitExit whether the user explicitly shut down the proxy
   * @param reason       message to kick online players with
   */
  public void shutdown(final boolean explicitExit, final Component reason) {
    if (eventManager == null || pluginManager == null || cm == null || scheduler == null) {
      throw new AssertionError();
    }

    if (!shutdownInProgress.compareAndSet(false, true)) {
      return;
    }

    Runnable shutdownProcess = () -> {
      startedShutdown = true;
      LOGGER.info("Shutting down the proxy...");

      // Shutdown the connection manager, this should be
      // done first to refuse new connections
      cm.shutdown();

      try {
        eventManager.fire(new ProxyPreShutdownEvent())
            .toCompletableFuture()
            .get(PRE_SHUTDOWN_TIMEOUT, TimeUnit.SECONDS);
      } catch (TimeoutException ignored) {
        LOGGER.warn("Your plugins took over {} seconds during pre shutdown.", PRE_SHUTDOWN_TIMEOUT);
      } catch (ExecutionException ee) {
        LOGGER.error("Exception in ProxyPreShutdownEvent handler; continuing shutdown.", ee);
      } catch (InterruptedException ignored) {
        Thread.currentThread().interrupt();
        LOGGER.warn("Interrupted while waiting for ProxyPreShutdownEvent; continuing shutdown.");
      }

      ImmutableList<@NotNull ConnectedPlayer> players = ImmutableList.copyOf(connectionsByUuid.values());
      ImmutableList<@NotNull UUID> playerUuids = ImmutableList.copyOf(connectionsByUuid.keySet());

      if (!getConfiguration().isAcceptTransfers()) {
        for (ConnectedPlayer player : players) {
          player.disconnect(reason);
        }
      } else {
        final ProxyAddress chosen = getProxyAddressToUse();
        if (chosen == null) {
          for (ConnectedPlayer player : players) {
            player.disconnect(reason);
          }
        } else {
          try {
            LOGGER.log(Level.INFO, "Transferring all players to new proxy...");
            Thread.sleep(1000);
          } catch (InterruptedException e) {
            throw new RuntimeException(e);
          }

          for (ConnectedPlayer player : players) {
            if (player.getProtocolVersion().noLessThan(ProtocolVersion.MINECRAFT_1_20_5)) {
              player.transferToHost(new InetSocketAddress(chosen.ip(), chosen.port()));
            } else {
              player.disconnect(reason);
            }
          }
        }
      }

      try {
        boolean timedOut = false;

        try {
          // Wait for the connections finish tearing down, this
          // makes sure that all the disconnect events are being fired

          CompletableFuture<Void> playersTeardownFuture = CompletableFuture.allOf(players.stream()
                  .map(ConnectedPlayer::getTeardownFuture)
                  .toArray(CompletableFuture[]::new));

          playersTeardownFuture.get(10, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
          timedOut = true;
        } catch (ExecutionException e) {
          timedOut = true;
          LOGGER.error("Exception while tearing down player connections", e);
        }

        eventManager.fire(new ProxyShutdownEvent()).join();

        timedOut = !scheduler.shutdown() || timedOut;

        if (timedOut) {
          LOGGER.error("Your plugins took over 10 seconds to shut down.");
        }
      } catch (InterruptedException e) {
        // Not much we can do about this...
        Thread.currentThread().interrupt();
      }

      if (this.queueManager != null) {
        playerUuids.forEach(u -> this.queueManager.removePlayerEntirely(u));
        this.queueManager.teardown();
      }

      // Disable Redis if we have it enabled
      if (this.configuration.getRedis().isEnabled()) {
        this.redis.shutdown();
      }

      // Since we manually removed the shutdown hook, we need to handle the shutdown ourselves.
      LogManager.shutdown();

      shutdown = true;

      if (explicitExit) {
        System.exit(0);
      }
    };

    if (explicitExit) {
      Thread thread = new Thread(shutdownProcess);
      thread.start();
    } else {
      shutdownProcess.run();
    }
  }

  /**
   * Calls {@link #shutdown(boolean, Component)} with the default reason "Proxy shutting down.".
   *
   * @param explicitExit whether the user explicitly shut down the proxy
   */
  public void shutdown(final boolean explicitExit) {
    shutdown(explicitExit, Component.translatable("velocity.kick.shutdown"));
  }

  /**
   * Shuts down the proxy with the specified reason.
   *
   * <p>This method delegates to {@link #shutdown(boolean, Component)} with
   * {@code explicitExit = true}.</p>
   *
   * @param reason the {@link Component} reason to display to players
   */
  @Override
  public void shutdown(final Component reason) {
    shutdown(true, reason);
  }

  /**
   * Shuts down the proxy using the default shutdown reason.
   *
   * <p>This method delegates to {@link #shutdown(boolean)} with
   * {@code explicitExit = true}.</p>
   */
  @Override
  public void shutdown() {
    shutdown(true);
  }

  private @Nullable ProxyAddress getProxyAddressToUse() {
    if (!this.isRedisEnabled()) {
      return null;
    }

    DynamicProxyFilterMode filter = getConfiguration().getDynamicProxyFilter();
    List<ProxyAddress> addresses = new ArrayList<>(getConfiguration().getProxyAddresses().stream().toList());

    if (isRedisEnabled()) {
      addresses.removeIf(address -> getProxyId().equalsIgnoreCase(address.proxyId()));
    }

    if (addresses.isEmpty()) {
      return null;
    }

    switch (filter) {
      case FIRST_FOUND -> {
        // Don't sort
      }
      case MOST_EMPTY -> {
        // Sort to get most empty first
        addresses.sort((o1, o2) -> {
          int connectedSize1 = redis.getPlayerService().getPlayerEntriesOnProxy(o1.proxyId()).size();
          int connectedSize2 = redis.getPlayerService().getPlayerEntriesOnProxy(o2.proxyId()).size();
          return Long.compare(connectedSize1, connectedSize2);
        });
      }
      case LEAST_EMPTY -> {
        // Sort to get least empty first
        addresses.sort((o1, o2) -> {
          int connectedSize1 = redis.getPlayerService().getPlayerEntriesOnProxy(o1.proxyId()).size();
          int connectedSize2 = redis.getPlayerService().getPlayerEntriesOnProxy(o2.proxyId()).size();
          return Long.compare(connectedSize2, connectedSize1);
        });
      }
      case NONE -> {
        // No next address
        return null;
      }
      default -> {
        throw new IllegalStateException("Invalid filter '" + filter + "'.");
      }
    }

    return addresses.getFirst();
  }

  /**
   * Closes all active network listeners managed by this proxy.
   *
   * <p>This method shuts down the underlying endpoints gracefully, preventing
   * new connections while allowing existing resources to be released.</p>
   */
  @Override
  public void closeListeners() {
    this.cm.closeEndpoints(false);
  }

  /**
   * Creates a new {@link HttpClient} instance configured with the proxy's connection settings.
   *
   * @return a new {@link HttpClient} instance
   */
  public HttpClient createHttpClient() {
    return cm.createHttpClient();
  }

  /**
   * Returns the rate limiter used to restrict login attempts per IP address.
   *
   * @return the {@link Ratelimiter} for login attempts, or {@code null} if not initialized
   */
  public @MonotonicNonNull Ratelimiter<InetAddress> getIpAttemptLimiter() {
    return ipAttemptLimiter;
  }

  /**
   * Returns the rate limiter used to limit command usage by player UUID.
   *
   * @return the {@link Ratelimiter} for command usage, or {@code null} if not initialized
   */
  public @MonotonicNonNull Ratelimiter<UUID> getCommandRateLimiter() {
    return commandRateLimiter;
  }

  /**
   * Returns the rate limiter used to limit tab completion usage by player UUID.
   *
   * @return the {@link Ratelimiter} for tab completion, or {@code null} if not initialized
   */
  public @MonotonicNonNull Ratelimiter<UUID> getTabCompleteRateLimiter() {
    return tabCompleteRateLimiter;
  }

  /**
   * Checks if the {@code connection} can be registered with the proxy.
   *
   * @param connection the connection to check
   * @return {@code true} if we can register the connection, {@code false} if not
   */
  public boolean canRegisterConnection(final ConnectedPlayer connection) {
    // When kick-existing-players is enabled, skip duplicate checks here.
    // registerConnection() handles kicking the existing player and enforcing IP rules.
    if (configuration.isKickExistingPlayers()) {
      return true;
    }

    String lowerName = connection.getUsername().toLowerCase(Locale.US);

    if (connectionsByName.containsKey(lowerName)) {
      return false;
    }

    return !connectionsByUuid.containsKey(connection.getUniqueId());
  }

  /**
   * Attempts to register the {@code connection} with the proxy.
   *
   * @param connection the connection to register
   * @return {@code true} if we registered the connection, {@code false} if not
   */
  public boolean registerConnection(final ConnectedPlayer connection) {
    String lowerName = connection.getUsername().toLowerCase(Locale.US);

    if (!this.configuration.isKickExistingPlayers()) {
      // Standard behavior: block duplicate connections
      if (connectionsByName.containsKey(lowerName)) {
        return false;
      }

      connectionsByName.put(lowerName, connection);

      if (connectionsByUuid.putIfAbsent(connection.getUniqueId(), connection) != null) {
        connectionsByName.remove(lowerName, connection);
        return false;
      }

      return true;
    }

    // kick-existing-players is enabled. Kick the existing session so the new one can take over.
    // When kick-existing-players-check-ip is also enabled, only kick if the new connection comes
    // from the same IP address.
    ConnectedPlayer existingByUuid = connectionsByUuid.get(connection.getUniqueId());
    if (existingByUuid != null) {
      if (this.configuration.isKickExistingPlayersCheckIp()) {
        InetAddress newIp = connection.getRemoteAddress().getAddress();
        InetAddress existingIp = existingByUuid.getRemoteAddress().getAddress();
        if (!newIp.equals(existingIp)) {
          // Different IP with same UUID: protect the existing player, deny the new connection.
          return false;
        }
      }
      existingByUuid.disconnect(Component.translatable("multiplayer.disconnect.duplicate_login"));
    }

    // Also check for a username conflict whose UUID differs from the one above.
    ConnectedPlayer existingByName = connectionsByName.get(lowerName);
    if (existingByName != null && existingByName != existingByUuid) {
      if (this.configuration.isKickExistingPlayersCheckIp()) {
        InetAddress newIp = connection.getRemoteAddress().getAddress();
        InetAddress existingIp = existingByName.getRemoteAddress().getAddress();
        if (!newIp.equals(existingIp)) {
          return false;
        }
      }
      existingByName.disconnect(Component.translatable("multiplayer.disconnect.duplicate_login"));
    }

    connectionsByName.put(lowerName, connection);
    connectionsByUuid.put(connection.getUniqueId(), connection);

    return true;
  }

  /**
   * Unregisters the given player from the proxy.
   *
   * @param connection the connection to unregister
   */
  public void unregisterConnection(final ConnectedPlayer connection) {
    connectionsByName.remove(connection.getUsername().toLowerCase(Locale.US), connection);
    connectionsByUuid.remove(connection.getUniqueId(), connection);
    connection.disconnected();
  }

  /**
   * Attempts to locate a player by their username (case-insensitive).
   *
   * @param username the player's username to search for
   * @return an {@link Optional} containing the {@link ConnectedPlayer} if found, otherwise empty
   */
  @Override
  public Optional<ConnectedPlayer> getPlayer(final String username) {
    Preconditions.checkNotNull(username, "username");
    return Optional.ofNullable(connectionsByName.get(username.toLowerCase(Locale.US)));
  }

  /**
   * Attempts to locate a player by their unique UUID.
   *
   * @param uuid the UUID of the player
   * @return an {@link Optional} containing the {@link ConnectedPlayer} if found, otherwise empty
   */
  @Override
  public Optional<ConnectedPlayer> getPlayer(final UUID uuid) {
    Preconditions.checkNotNull(uuid, "uuid");
    return Optional.ofNullable(connectionsByUuid.get(uuid));
  }

  /**
   * Returns a collection of players whose usernames match the given partial input.
   *
   * @param partialName the partial name to match
   * @return a collection of matching {@link ConnectedPlayer}s
   */
  @Override
  public Collection<ConnectedPlayer> matchPlayer(final String partialName) {
    Objects.requireNonNull(partialName);

    return getAllPlayers().stream().filter(p -> p.getUsername()
            .regionMatches(true, 0, partialName, 0, partialName.length()))
        .collect(Collectors.toList());
  }

  /**
   * Returns a collection of servers whose names match the given partial input.
   *
   * @param partialName the partial server name
   * @return a collection of matching {@link VelocityRegisteredServer}s
   */
  @Override
  public Collection<VelocityRegisteredServer> matchServer(final String partialName) {
    Objects.requireNonNull(partialName);

    return getAllServers().stream().filter(s -> s.getServerInfo().getName()
            .regionMatches(true, 0, partialName, 0, partialName.length()))
        .collect(Collectors.toList());
  }

  /**
   * Returns an immutable collection of all players currently connected to the proxy.
   *
   * @return all connected players
   */
  @Override
  public Collection<ConnectedPlayer> getAllPlayers() {
    return ImmutableList.copyOf(connectionsByUuid.values());
  }

  /**
   * Gets the number of players currently connected to the proxy.
   * If Redis is enabled, this returns the total player count across all proxies.
   * Otherwise, this returns only the local proxy's player count.
   *
   * @return the number of connected players
   */
  @Override
  public int getPlayerCount() {
    if (this.isRedisEnabled()) {
      return this.redis.getPlayerService().getTotalPlayerCount();
    } else {
      return connectionsByUuid.size();
    }
  }

  /**
   * Attempts to retrieve a server by its registered name.
   *
   * @param name the name of the server
   * @return an {@link Optional} containing the {@link VelocityRegisteredServer}, if present
   */
  @Override
  public Optional<VelocityRegisteredServer> getServer(final String name) {
    return servers.getServer(name);
  }

  /**
   * Gets all servers currently registered with the proxy.
   *
   * @return a collection of all registered servers
   */
  @Override
  public Collection<VelocityRegisteredServer> getAllServers() {
    return servers.getAllServers();
  }

  /**
   * Creates a {@link VelocityRegisteredServer} from the specified {@link ServerInfo} without registering it.
   *
   * @param server the server info to wrap
   * @return a {@link VelocityRegisteredServer} representing the server
   */
  @Override
  public VelocityRegisteredServer createRawRegisteredServer(final ServerInfo server) {
    return servers.createRawRegisteredServer(server);
  }

  /**
   * Registers the specified server with the proxy, or returns the existing one if already registered.
   *
   * @param server the server to register
   * @return the registered server instance
   */
  @Override
  public VelocityRegisteredServer registerServer(final ServerInfo server) {
    return servers.register(server);
  }

  /**
   * Unregisters a server from the proxy, if it is currently registered.
   *
   * @param server the server to unregister
   */
  @Override
  public void unregisterServer(final ServerInfo server) {
    servers.unregister(server);
  }

  /**
   * Gets the proxy console, which acts as a {@link Command} source for console-issued commands.
   *
   * @return the {@link VelocityConsole} instance
   */
  @Override
  public VelocityConsole getConsoleCommandSource() {
    return console;
  }

  /**
   * Returns the plugin manager instance used to register and manage Velocity plugins.
   *
   * @return the {@link PluginManager}
   */
  @Override
  public PluginManager getPluginManager() {
    return pluginManager;
  }

  /**
   * Returns the event manager used to register, fire, and dispatch plugin events.
   *
   * @return the {@link VelocityEventManager}
   */
  @Override
  public VelocityEventManager getEventManager() {
    return eventManager;
  }

  /**
   * Returns the proxy's scheduler for running asynchronous and synchronous tasks.
   *
   * @return the {@link VelocityScheduler}
   */
  @Override
  public VelocityScheduler getScheduler() {
    return scheduler;
  }

  /**
   * Returns the registrar responsible for plugin channel registration and translation.
   *
   * @return the {@link VelocityChannelRegistrar}
   */
  @Override
  public VelocityChannelRegistrar getChannelRegistrar() {
    return channelRegistrar;
  }

  /**
   * Returns whether the proxy is currently shutting down.
   *
   * @return {@code true} if a shutdown is in progress
   */
  @Override
  public boolean isShuttingDown() {
    return shutdownInProgress.get();
  }

  /**
   * Returns the address the proxy is currently bound to for accepting connections.
   *
   * @return the {@link InetSocketAddress} the proxy is listening on
   * @throws IllegalStateException if the configuration has not been loaded
   */
  @Override
  public InetSocketAddress getBoundAddress() {
    if (configuration == null) {
      throw new IllegalStateException(
          "No configuration"); // even though you'll never get the chance... heh, heh
    }

    return configuration.getBind();
  }

  /**
   * Returns all available audiences, including the console and all online players.
   *
   * @return an iterable of {@link Audience} instances
   */
  @Override
  public @NonNull Iterable<? extends Audience> audiences() {
    Collection<Audience> audiences = new ArrayList<>(this.getPlayerCount() + 1);
    audiences.add(this.console);
    audiences.addAll(this.getAllPlayers());
    return audiences;
  }

  /**
   * Returns a Gson instance for use in serializing server ping instances.
   *
   * @param version the protocol version in use
   * @return the Gson instance
   */
  public static Gson getPingGsonInstance(final ProtocolVersion version) {
    if (version == ProtocolVersion.UNKNOWN
        || version.noLessThan(ProtocolVersion.MINECRAFT_1_20_3)) {
      return MODERN_PING_SERIALIZER;
    }

    if (version.noLessThan(ProtocolVersion.MINECRAFT_1_16)) {
      return PRE_1_20_3_PING_SERIALIZER;
    }

    return PRE_1_16_PING_SERIALIZER;
  }

  /**
   * Creates a new {@link ResourcePackInfo.Builder} for constructing a resource pack to send to players.
   *
   * <p>This builder allows you to specify metadata such as the pack's SHA-1 hash, whether it's forced,
   * the prompt message, and more.</p>
   *
   * @param url the URL from which the resource pack should be downloaded
   * @return a new {@link ResourcePackInfo.Builder} instance
   */
  @Override
  public ResourcePackInfo.Builder createResourcePackBuilder(final String url) {
    return new VelocityResourcePackInfo.BuilderImpl(url);
  }

  /**
   * Returns the shared logger instance used by this proxy implementation.
   *
   * @return the proxy {@link Logger}
   */
  public final Logger getLogger() {
    return LOGGER;
  }

  /**
   * Check whether the queue system is enabled for the proxy.
   *
   * @return true if the queue system is enabled, otherwise false
   */
  @Override
  public boolean isQueueEnabled() {
    return this.configuration.getQueue().isEnabled();
  }

  /**
   * Check whether the redis system is enabled for the proxy.
   *
   * @return true if the redis system is enabled, otherwise false
   */
  public boolean isRedisEnabled() {
    return this.configuration.getRedis().isEnabled();
  }

  /**
   * Returns the proxy id for the current proxy from the proxy configuration.
   *
   * @return the proxy id for the current proxy
   */
  public String getProxyId() {
    if (this.configuration.getRedis().isEnabled()) {
      return this.configuration.getRedis().getProxyId();
    }

    return "single_proxy";
  }
}
