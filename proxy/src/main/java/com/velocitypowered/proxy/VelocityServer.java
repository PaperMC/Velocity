/*
 * Copyright (C) 2018 Velocity Contributors
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
import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.event.lifecycle.ProxyInitializeEventImpl;
import com.velocitypowered.api.event.lifecycle.ProxyReloadEventImpl;
import com.velocitypowered.api.event.lifecycle.ProxyShutdownEventImpl;
import com.velocitypowered.api.network.NetworkEndpoint;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.PluginManager;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.connection.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import com.velocitypowered.api.util.Favicon;
import com.velocitypowered.api.util.GameProfile;
import com.velocitypowered.api.util.ProxyVersion;
import com.velocitypowered.proxy.command.VelocityCommandManager;
import com.velocitypowered.proxy.command.builtin.GlistCommand;
import com.velocitypowered.proxy.command.builtin.ServerCommand;
import com.velocitypowered.proxy.command.builtin.ShutdownCommand;
import com.velocitypowered.proxy.command.builtin.VelocityCommand;
import com.velocitypowered.proxy.config.VelocityConfiguration;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.console.VelocityConsole;
import com.velocitypowered.proxy.event.VelocityEventManager;
import com.velocitypowered.proxy.network.ConnectionManager;
import com.velocitypowered.proxy.network.ProtocolUtils;
import com.velocitypowered.proxy.network.serialization.FaviconSerializer;
import com.velocitypowered.proxy.network.serialization.GameProfileSerializer;
import com.velocitypowered.proxy.plugin.VelocityPluginManager;
import com.velocitypowered.proxy.scheduler.VelocityScheduler;
import com.velocitypowered.proxy.server.ServerMap;
import com.velocitypowered.proxy.util.AddressUtil;
import com.velocitypowered.proxy.util.EncryptionUtils;
import com.velocitypowered.proxy.util.FileSystemUtils;
import com.velocitypowered.proxy.util.VelocityChannelRegistrar;
import com.velocitypowered.proxy.util.bossbar.AdventureBossBarManager;
import com.velocitypowered.proxy.util.ratelimit.Ratelimiter;
import com.velocitypowered.proxy.util.ratelimit.Ratelimiters;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.AccessController;
import java.security.KeyPair;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.ForwardingAudience;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.translation.TranslationRegistry;
import net.kyori.adventure.util.UTF8ResourceBundleControl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asynchttpclient.AsyncHttpClient;
import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class VelocityServer implements ProxyServer, ForwardingAudience {

  private static final Logger logger = LogManager.getLogger(VelocityServer.class);
  public static final Gson GENERAL_GSON = new GsonBuilder()
      .registerTypeHierarchyAdapter(Favicon.class, FaviconSerializer.INSTANCE)
      .registerTypeHierarchyAdapter(GameProfile.class, GameProfileSerializer.INSTANCE)
      .create();
  private static final Gson PRE_1_16_PING_SERIALIZER = ProtocolUtils
      .getJsonChatSerializer(ProtocolVersion.MINECRAFT_1_15_2)
      .serializer()
      .newBuilder()
      .registerTypeHierarchyAdapter(Favicon.class, FaviconSerializer.INSTANCE)
      .create();
  private static final Gson POST_1_16_PING_SERIALIZER = ProtocolUtils
      .getJsonChatSerializer(ProtocolVersion.MINECRAFT_1_16)
      .serializer()
      .newBuilder()
      .registerTypeHierarchyAdapter(Favicon.class, FaviconSerializer.INSTANCE)
      .create();

  private final ConnectionManager cm;
  private final ProxyOptions options;
  private @MonotonicNonNull VelocityConfiguration configuration;
  private @MonotonicNonNull KeyPair serverKeyPair;
  private final ServerMap servers;
  private final VelocityCommandManager commandManager;
  private final AtomicBoolean shutdownInProgress = new AtomicBoolean(false);
  private boolean shutdown = false;
  private final VelocityPluginManager pluginManager;
  private final AdventureBossBarManager bossBarManager;

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
    commandManager = new VelocityCommandManager(eventManager);
    scheduler = new VelocityScheduler(pluginManager);
    console = new VelocityConsole(this);
    cm = new ConnectionManager(this);
    servers = new ServerMap(this);
    this.options = options;
    this.bossBarManager = new AdventureBossBarManager();
  }

  public KeyPair getServerKeyPair() {
    return serverKeyPair;
  }

  @Override
  public VelocityConfiguration configuration() {
    return this.configuration;
  }

  @Override
  public ProxyVersion version() {
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
  public Collection<NetworkEndpoint> endpoints() {
    return this.cm.endpoints();
  }

  @Override
  public VelocityCommandManager commandManager() {
    return commandManager;
  }

  void awaitProxyShutdown() {
    cm.getBossGroup().terminationFuture().syncUninterruptibly();
  }

  @EnsuresNonNull({"serverKeyPair", "eventManager", "console", "cm", "configuration"})
  void start() {
    logger.info("Booting up {} {}...", version().name(), version().version());
    console.setupStreams();

    registerTranslations();

    serverKeyPair = EncryptionUtils.createRsaKeyPair(1024);

    cm.logChannelInformation();

    // Initialize commands first
    commandManager.register("velocity", new VelocityCommand(this));
    commandManager.register("server", new ServerCommand(this));
    commandManager.register("shutdown", new ShutdownCommand(this),"end");
    new GlistCommand(this).register();

    this.doStartupConfigLoad();

    for (Map.Entry<String, String> entry : configuration.getServers().entrySet()) {
      servers.register(new ServerInfo(entry.getKey(), AddressUtil.parseAddress(entry.getValue())));
    }

    ipAttemptLimiter = Ratelimiters.createWithMilliseconds(configuration.getLoginRatelimit());
    loadPlugins();

    // Go ahead and fire the proxy initialization event. We block since plugins should have a chance
    // to fully initialize before we accept any connections to the server.
    eventManager.fire(new ProxyInitializeEventImpl()).join();

    // init console permissions after plugins are loaded
    console.setupPermissions();

    final SocketAddress bindAddr = configuration.getBind();
    final Integer port = this.options.getPort();
    if (port != null && bindAddr instanceof InetSocketAddress) {
      logger.debug("Overriding bind port to {} from command line option", port);
      this.cm.bind(new InetSocketAddress(((InetSocketAddress) bindAddr).getHostString(), port));
    } else {
      this.cm.bind(configuration.getBind());
    }

    if (configuration.isQueryEnabled() && bindAddr instanceof InetSocketAddress) {
      this.cm.queryBind(((InetSocketAddress) bindAddr).getHostString(),
          configuration.getQueryPort());
    }

    Metrics.VelocityMetrics.startMetrics(this, configuration.getMetrics());
  }

  private void registerTranslations() {
    final TranslationRegistry translationRegistry = TranslationRegistry
        .create(Key.key("velocity", "translations"));
    translationRegistry.defaultLocale(Locale.US);
    try {
      FileSystemUtils.visitResources(VelocityServer.class, path -> {
        logger.info("Loading localizations...");

        try (Stream<Path> stream = Files.walk(path)) {
          stream.forEach(file -> {
            if (!Files.isRegularFile(file)) {
              return;
            }

            String filename = com.google.common.io.Files
                .getNameWithoutExtension(file.getFileName().toString());
            String localeName = filename.replace("messages_", "")
                .replace("messages", "")
                .replace('_', '-');
            Locale locale;
            if (localeName.isEmpty()) {
              locale = Locale.US;
            } else {
              locale = Locale.forLanguageTag(localeName);
            }

            translationRegistry.registerAll(locale,
                ResourceBundle.getBundle("com/velocitypowered/proxy/l10n/messages",
                    locale, UTF8ResourceBundleControl.get()), false);
          });
        } catch (IOException e) {
          logger.error("Encountered an I/O error whilst loading translations", e);
        }
      }, "com", "velocitypowered", "proxy", "l10n");
    } catch (IOException e) {
      logger.error("Encountered an I/O error whilst loading translations", e);
      return;
    }
    GlobalTranslator.get().addSource(translationRegistry);
  }

  @SuppressFBWarnings("DM_EXIT")
  private void doStartupConfigLoad() {
    try {
      Path configPath = Paths.get("velocity.toml");
      configuration = VelocityConfiguration.read(configPath);

      if (!configuration.validate()) {
        logger.error("Your configuration is invalid. Velocity will not start up until the errors "
            + "are resolved.");
        LogManager.shutdown();
        System.exit(1);
      }
    } catch (Exception e) {
      logger.error("Unable to read/load/save your velocity.toml. The server will shut down.", e);
      LogManager.shutdown();
      System.exit(1);
    }
  }

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
    for (PluginContainer plugin : pluginManager.plugins()) {
      Object instance = plugin.instance();
      if (instance != null) {
        try {
          eventManager.registerInternally(plugin, instance);
        } catch (Exception e) {
          logger.error("Unable to register plugin listener for {}",
              MoreObjects.firstNonNull(plugin.description().name(), plugin.description().id()), e);
        }
      }
    }

    logger.info("Loaded {} plugin(s)", pluginManager.plugins().size() - 1);
  }

  public Bootstrap createBootstrap(@Nullable EventLoopGroup group, SocketAddress target) {
    return this.cm.createWorker(group, target);
  }

  public ChannelInitializer<Channel> getBackendChannelInitializer() {
    return this.cm.backendChannelInitializer.get();
  }

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
      RegisteredServer rs = servers.getServer(entry.getKey());
      if (rs == null) {
        servers.register(newInfo);
      } else if (!rs.serverInfo().equals(newInfo)) {
        for (Player player : rs.connectedPlayers()) {
          if (!(player instanceof ConnectedPlayer)) {
            throw new IllegalStateException("ConnectedPlayer not found for player " + player
                + " in server " + rs.serverInfo().name());
          }
          evacuate.add((ConnectedPlayer) player);
        }
        servers.unregister(rs.serverInfo());
        servers.register(newInfo);
      }
    }

    // If we had any players to evacuate, let's move them now. Wait until they are all moved off.
    if (!evacuate.isEmpty()) {
      CountDownLatch latch = new CountDownLatch(evacuate.size());
      for (ConnectedPlayer player : evacuate) {
        RegisteredServer next = player.getNextServerToTry();
        if (next != null) {
          player.createConnectionRequest(next).connectWithIndication()
              .thenAccept((success) -> {
                if (success == null || !success) {
                  player.disconnect(Component.text("Your server has been changed, but we could "
                      + "not move you to any fallback servers."));
                }
                latch.countDown();
              })
              .exceptionally(throwable -> {
                player.disconnect(Component.text("Your server has been changed, but we could "
                    + "not move you to any fallback servers."));
                latch.countDown();
                return null;
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
        logger.error("Interrupted whilst moving players", e);
        Thread.currentThread().interrupt();
      }
    }

    // If we have a new bind address, bind to it
    SocketAddress oldBind = configuration.getBind();
    SocketAddress newBind = newConfiguration.getBind();
    if (!configuration.getBind().equals(newBind)) {
      this.cm.bind(newConfiguration.getBind());
      this.cm.close(configuration.getBind());
    }

    if (configuration.isQueryEnabled() && (!newConfiguration.isQueryEnabled()
        || ((newConfiguration.getQueryPort() != configuration.getQueryPort())
        && (oldBind instanceof InetSocketAddress)))) {
      this.cm.close(new InetSocketAddress(
          ((InetSocketAddress) oldBind).getHostString(), configuration.getQueryPort()));
    }

    if (newConfiguration.isQueryEnabled() && newBind instanceof InetSocketAddress) {
      this.cm.queryBind(((InetSocketAddress) newBind).getHostString(),
          newConfiguration.getQueryPort());
    }

    ipAttemptLimiter = Ratelimiters.createWithMilliseconds(newConfiguration.getLoginRatelimit());
    this.configuration = newConfiguration;
    eventManager.fireAndForget(new ProxyReloadEventImpl());
    return true;
  }

  /**
   * Shuts down the proxy, kicking players with the specified {@code reason}.
   *
   * @param explicitExit whether the user explicitly shut down the proxy
   * @param reason message to kick online players with
   */
  public void shutdown(boolean explicitExit, Component reason) {
    if (eventManager == null || pluginManager == null || cm == null || scheduler == null) {
      throw new AssertionError();
    }

    if (!shutdownInProgress.compareAndSet(false, true)) {
      return;
    }

    Runnable shutdownProcess = () -> {
      logger.info("Shutting down the proxy...");

      // Shutdown the connection manager, this should be
      // done first to refuse new connections
      cm.shutdown();

      ImmutableList<ConnectedPlayer> players = ImmutableList.copyOf(connectionsByUuid.values());
      for (ConnectedPlayer player : players) {
        player.disconnect(reason);
      }

      try {
        boolean timedOut = false;

        try {
          // Wait for the connections finish tearing down, this
          // makes sure that all the disconnect events are being fired

          CompletableFuture<Void> playersTeardownFuture = CompletableFuture.allOf(players.stream()
                  .map(ConnectedPlayer::getTeardownFuture)
                  .toArray((IntFunction<CompletableFuture<Void>[]>) CompletableFuture[]::new));

          playersTeardownFuture.get(10, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
          timedOut = true;
        } catch (ExecutionException e) {
          timedOut = true;
          logger.error("Exception while tearing down player connections", e);
        }

        try {
          eventManager.fire(new ProxyShutdownEventImpl()).get(10, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
          timedOut = true;
        } catch (ExecutionException e) {
          timedOut = true;
          logger.error("Exception while firing the shutdown event", e);
        }

        timedOut = !eventManager.shutdown() || timedOut;
        timedOut = !scheduler.shutdown() || timedOut;

        if (timedOut) {
          logger.error("Your plugins took over 10 seconds to shut down.");
        }
      } catch (InterruptedException e) {
        // Not much we can do about this...
        Thread.currentThread().interrupt();
      }

      shutdown = true;

      if (explicitExit) {
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
          @Override
          @SuppressFBWarnings("DM_EXIT")
          public Void run() {
            System.exit(0);
            return null;
          }
        });
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
   * Calls {@link #shutdown(boolean, Component)} with the default reason "Proxy shutting down."
   *
   * @param explicitExit whether the user explicitly shut down the proxy
   */
  public void shutdown(boolean explicitExit) {
    shutdown(explicitExit, Component.text("Proxy shutting down."));
  }

  @Override
  public void shutdown(Component reason) {
    shutdown(true, reason);
  }

  @Override
  public void shutdown() {
    shutdown(true);
  }

  public AsyncHttpClient getAsyncHttpClient() {
    return cm.getHttpClient();
  }

  public Ratelimiter getIpAttemptLimiter() {
    return ipAttemptLimiter;
  }

  /**
   * Checks if the {@code connection} can be registered with the proxy.
   * @param connection the connection to check
   * @return {@code true} if we can register the connection, {@code false} if not
   */
  public boolean canRegisterConnection(ConnectedPlayer connection) {
    if (configuration.isOnlineMode() && configuration.isOnlineModeKickExistingPlayers()) {
      return true;
    }
    String lowerName = connection.username().toLowerCase(Locale.US);
    return !(connectionsByName.containsKey(lowerName)
        || connectionsByUuid.containsKey(connection.id()));
  }
  
  /**
   * Attempts to register the {@code connection} with the proxy.
   * @param connection the connection to register
   * @return {@code true} if we registered the connection, {@code false} if not
   */
  public boolean registerConnection(ConnectedPlayer connection) {
    String lowerName = connection.username().toLowerCase(Locale.US);

    if (!this.configuration.isOnlineModeKickExistingPlayers()) {
      if (connectionsByName.putIfAbsent(lowerName, connection) != null) {
        return false;
      }
      if (connectionsByUuid.putIfAbsent(connection.id(), connection) != null) {
        connectionsByName.remove(lowerName, connection);
        return false;
      }
    } else {
      ConnectedPlayer existing = connectionsByUuid.get(connection.id());
      if (existing != null) {
        existing.disconnect(Component.translatable("multiplayer.disconnect.duplicate_login"));
      }

      // We can now replace the entries as needed.
      connectionsByName.put(lowerName, connection);
      connectionsByUuid.put(connection.id(), connection);
    }
    return true;
  }

  /**
   * Unregisters the given player from the proxy.
   *
   * @param connection the connection to unregister
   */
  public void unregisterConnection(ConnectedPlayer connection) {
    connectionsByName.remove(connection.username().toLowerCase(Locale.US), connection);
    connectionsByUuid.remove(connection.id(), connection);
    bossBarManager.onDisconnect(connection);
  }

  @Override
  public @Nullable Player player(String username) {
    Preconditions.checkNotNull(username, "username");
    return connectionsByName.get(username.toLowerCase(Locale.US));
  }

  @Override
  public @Nullable Player player(UUID uuid) {
    Preconditions.checkNotNull(uuid, "uuid");
    return connectionsByUuid.get(uuid);
  }

  @Override
  public Collection<Player> matchPlayer(String partialName) {
    Objects.requireNonNull(partialName);

    return connectedPlayers().stream().filter(p -> p.username()
            .regionMatches(true, 0, partialName, 0, partialName.length()))
            .collect(Collectors.toList());
  }

  @Override
  public Collection<RegisteredServer> matchServer(String partialName) {
    Objects.requireNonNull(partialName);

    return registeredServers().stream().filter(s -> s.serverInfo().name()
            .regionMatches(true, 0, partialName, 0, partialName.length()))
            .collect(Collectors.toList());
  }

  @Override
  public Collection<Player> connectedPlayers() {
    return ImmutableList.copyOf(connectionsByUuid.values());
  }

  @Override
  public int countConnectedPlayers() {
    return connectionsByUuid.size();
  }

  @Override
  public @Nullable RegisteredServer server(String name) {
    return servers.getServer(name);
  }

  @Override
  public Collection<RegisteredServer> registeredServers() {
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
  public VelocityConsole consoleCommandSource() {
    return console;
  }

  @Override
  public PluginManager pluginManager() {
    return pluginManager;
  }

  @Override
  public EventManager eventManager() {
    return eventManager;
  }

  @Override
  public VelocityScheduler scheduler() {
    return scheduler;
  }

  @Override
  public VelocityChannelRegistrar channelRegistrar() {
    return channelRegistrar;
  }

  @Override
  public @NonNull Iterable<? extends Audience> audiences() {
    Collection<Audience> audiences = new ArrayList<>(this.countConnectedPlayers() + 1);
    audiences.add(this.console);
    audiences.addAll(this.connectedPlayers());
    return audiences;
  }

  public AdventureBossBarManager getBossBarManager() {
    return bossBarManager;
  }

  public static Gson getPingGsonInstance(ProtocolVersion version) {
    return version.gte(ProtocolVersion.MINECRAFT_1_16) ? POST_1_16_PING_SERIALIZER
        : PRE_1_16_PING_SERIALIZER;
  }
}
