/*
 * Copyright (C) 2024 Velocity Contributors
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

package com.velocitypowered.proxy.config;

import static com.velocitypowered.proxy.config.VelocityConfiguration.Servers.cleanServerName;
import static com.velocitypowered.proxy.config.VelocityConfiguration.generateRandomString;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.velocitypowered.proxy.config.migration.ConfigurationMigration;
import com.velocitypowered.proxy.config.migration.ForwardingMigration;
import com.velocitypowered.proxy.config.migration.KeyAuthenticationMigration;
import com.velocitypowered.proxy.config.migration.MiniMessageTranslationsMigration;
import com.velocitypowered.proxy.config.migration.MotdMigration;
import com.velocitypowered.proxy.config.migration.PacketLimiterMigration;
import com.velocitypowered.proxy.config.migration.PingPassthroughMigration;
import com.velocitypowered.proxy.config.migration.TransferIntegrationMigration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Legacy configuration loader for Velocity.
 */
public class LegacyConfigurationLoader {
  private static final Logger logger = LogManager.getLogger(LegacyConfigurationLoader.class);

  /**
   * Reads the raw {@code forwarding-secret-file} value from a legacy TOML configuration, so a
   * custom secret location can be preserved when migrating to {@code velocity.yml}.
   *
   * @param path the legacy configuration path
   * @return the configured forwarding-secret-file, or {@code null} if unset
   */
  static @Nullable String readForwardingSecretFile(final Path path) {
    try (CommentedFileConfig config = CommentedFileConfig.builder(path).build()) {
      config.load();
      return config.get("forwarding-secret-file");
    }
  }

  /**
   * Reads the Velocity configuration from {@code path}.
   *
   * @param path the path to read from
   * @return the deserialized Velocity configuration
   * @throws IOException if we could not read from the {@code path}.
   */
  @SuppressFBWarnings(value = "RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE",
      justification = "I looked carefully and there's no way SpotBugs is right.")
  public static VelocityConfiguration read(Path path) throws IOException {
    URL defaultConfigLocation = VelocityConfiguration.class.getClassLoader()
        .getResource("default-velocity.toml");
    if (defaultConfigLocation == null) {
      throw new RuntimeException("Default configuration file does not exist.");
    }

    // Create the forwarding-secret file on first-time startup if it doesn't exist
    final Path defaultForwardingSecretPath = Path.of("forwarding.secret");
    if (Files.notExists(path) && Files.notExists(defaultForwardingSecretPath)) {
      Files.writeString(defaultForwardingSecretPath, generateRandomString(12));
    }

    try (final CommentedFileConfig config = CommentedFileConfig.builder(path)
        .defaultData(defaultConfigLocation)
        .autosave()
        .preserveInsertionOrder()
        .sync()
        .build()
    ) {
      config.load();

      final ConfigurationMigration[] migrations = {
          new ForwardingMigration(),
          new KeyAuthenticationMigration(),
          new MotdMigration(),
          new MiniMessageTranslationsMigration(),
          new TransferIntegrationMigration(),
          new PacketLimiterMigration(),
          new PingPassthroughMigration(),
      };

      for (final ConfigurationMigration migration : migrations) {
        if (migration.shouldMigrate(config)) {
          migration.migrate(config, logger);
        }
      }

      String forwardingSecretString = System.getenv().getOrDefault(
          "VELOCITY_FORWARDING_SECRET", "");
      if (forwardingSecretString.isBlank()) {
        final String forwardSecretFile = config.get("forwarding-secret-file");
        final Path secretPath = forwardSecretFile == null
            ? defaultForwardingSecretPath
            : Path.of(forwardSecretFile);
        if (Files.exists(secretPath)) {
          if (Files.isRegularFile(secretPath)) {
            forwardingSecretString = String.join("", Files.readAllLines(secretPath));
          } else {
            throw new RuntimeException(
                "The file " + forwardSecretFile + " is not a valid file or it is a directory.");
          }
        } else {
          Files.createFile(secretPath);
          Files.writeString(secretPath, forwardingSecretString = generateRandomString(12),
              StandardCharsets.UTF_8);
          logger.info("The forwarding-secret-file does not exist. A new file has been created at {}",
              forwardSecretFile);
        }
      }
      final byte[] forwardingSecret = forwardingSecretString.getBytes(StandardCharsets.UTF_8);
      final String motd = config.getOrElse("motd", "<#09add3>A Velocity Server");

      // Read the rest of the config
      final CommentedConfig serversConfig = config.get("servers");
      final CommentedConfig forcedHostsConfig = config.get("forced-hosts");
      final CommentedConfig advancedConfig = config.get("advanced");
      final CommentedConfig queryConfig = config.get("query");
      final CommentedConfig metricsConfig = config.get("metrics");
      final PlayerInfoForwarding forwardingMode = config.getEnumOrElse(
          "player-info-forwarding-mode", PlayerInfoForwarding.NONE);
      final PingPassthroughMode pingPassthrough =
          PingPassthroughMode.fromConfig(config.get("ping-passthrough"));

      final boolean samplePlayersInPing = config.getOrElse("sample-players-in-ping", false);

      final String bind = config.getOrElse("bind", "0.0.0.0:25565");
      final int maxPlayers = config.getIntOrElse("show-max-players", 500);
      final boolean onlineMode = config.getOrElse("online-mode", true);
      final boolean forceKeyAuthentication = config.getOrElse("force-key-authentication", true);
      final boolean announceForge = config.getOrElse("announce-forge", true);
      final boolean preventClientProxyConnections = config.getOrElse(
          "prevent-client-proxy-connections", false);
      final boolean kickExisting = config.getOrElse("kick-existing-players", false);
      final boolean enablePlayerAddressLogging = config.getOrElse(
          "enable-player-address-logging", true);
      final VelocityConfiguration.PacketLimiterConfig packetLimiterConfig =
          VelocityConfiguration.PacketLimiterConfig.fromConfig(config.get("packet-limiter"));

      // Throw an exception if the forwarding-secret file is empty and the proxy is using a
      // forwarding mode that requires it.
      if (forwardingSecret.length == 0
          && (forwardingMode == PlayerInfoForwarding.MODERN
          || forwardingMode == PlayerInfoForwarding.BUNGEEGUARD)) {
        throw new RuntimeException("The forwarding-secret file must not be empty.");
      }

      return new VelocityConfiguration(
          bind,
          motd,
          maxPlayers,
          onlineMode,
          preventClientProxyConnections,
          announceForge,
          forwardingMode,
          forwardingSecret,
          kickExisting,
          pingPassthrough,
          samplePlayersInPing,
          enablePlayerAddressLogging,
          readServers(serversConfig),
          readForcedHosts(forcedHostsConfig),
          readAdvanced(advancedConfig),
          readQuery(queryConfig),
          readMetrics(metricsConfig),
          forceKeyAuthentication,
          packetLimiterConfig
      );
    }
  }


  private static VelocityConfiguration.Servers readServers(CommentedConfig config) {
    if (config != null) {
      Map<String, String> servers = new HashMap<>();
      for (UnmodifiableConfig.Entry entry : config.entrySet()) {
        if (entry.getValue() instanceof String) {
          servers.put(cleanServerName(entry.getKey()), entry.getValue());
        } else {
          if (!entry.getKey().equalsIgnoreCase("try")) {
            throw new IllegalArgumentException(
                "Server entry " + entry.getKey() + " is not a string!");
          }
        }
      }
      return new VelocityConfiguration.Servers(ImmutableMap.copyOf(servers),
          config.getOrElse("try", ImmutableList.of("lobby")));
    }
    return new VelocityConfiguration.Servers();
  }

  private static VelocityConfiguration.ForcedHosts readForcedHosts(CommentedConfig config) {
    if (config != null) {
      Map<String, List<String>> forcedHosts = new HashMap<>();
      for (UnmodifiableConfig.Entry entry : config.entrySet()) {
        if (entry.getValue() instanceof String) {
          forcedHosts.put(entry.getKey().toLowerCase(Locale.ROOT),
              ImmutableList.of(entry.getValue()));
        } else if (entry.getValue() instanceof List) {
          forcedHosts.put(entry.getKey().toLowerCase(Locale.ROOT),
              ImmutableList.copyOf((List<String>) entry.getValue()));
        } else {
          throw new IllegalStateException(
              "Invalid value of type " + entry.getValue().getClass() + " in forced hosts!");
        }
      }
      return new VelocityConfiguration.ForcedHosts(forcedHosts);
    }
    return new VelocityConfiguration.ForcedHosts();
  }

  private static VelocityConfiguration.Advanced readAdvanced(CommentedConfig config) {
    if (config != null) {
      int compressionThreshold = config.getIntOrElse("compression-threshold", 256);
      int compressionLevel = config.getIntOrElse("compression-level", -1);
      int loginRatelimit = config.getIntOrElse("login-ratelimit", 3000);
      int connectionTimeout = config.getIntOrElse("connection-timeout", 5000);
      int readTimeout = config.getIntOrElse("read-timeout", 30000);
      boolean proxyProtocol = false;
      if (config.contains("haproxy-protocol")) {
        proxyProtocol = config.getOrElse("haproxy-protocol", false);
      } else {
        proxyProtocol = config.getOrElse("proxy-protocol", false);
      }
      boolean tcpFastOpen = config.getOrElse("tcp-fast-open", false);
      boolean bungeePluginMessageChannel = config.getOrElse("bungee-plugin-message-channel", true);
      boolean showPingRequests = config.getOrElse("show-ping-requests", false);
      boolean failoverOnUnexpectedServerDisconnect = config
          .getOrElse("failover-on-unexpected-server-disconnect", true);
      boolean announceProxyCommands = config.getOrElse("announce-proxy-commands", true);
      boolean logCommandExecutions = config.getOrElse("log-command-executions", false);
      boolean logPlayerConnections = config.getOrElse("log-player-connections", true);
      boolean acceptTransfers = config.getOrElse("accepts-transfers", false);
      boolean enableReusePort = config.getOrElse("enable-reuse-port", false);
      int commandRateLimit = config.getIntOrElse("command-rate-limit", 25);
      boolean forwardCommandsIfRateLimited =
          config.getOrElse("forward-commands-if-rate-limited", true);
      int kickAfterRateLimitedCommands = config.getIntOrElse("kick-after-rate-limited-commands", 0);
      int tabCompleteRateLimit = config.getIntOrElse("tab-complete-rate-limit", 10);
      int kickAfterRateLimitedTabCompletes =
          config.getIntOrElse("kick-after-rate-limited-tab-completes", 0);
      return new VelocityConfiguration.Advanced(compressionThreshold, compressionLevel,
          loginRatelimit, connectionTimeout, readTimeout, proxyProtocol, tcpFastOpen,
          bungeePluginMessageChannel, showPingRequests, failoverOnUnexpectedServerDisconnect,
          announceProxyCommands, logCommandExecutions, logPlayerConnections, acceptTransfers,
          enableReusePort, commandRateLimit, forwardCommandsIfRateLimited,
          kickAfterRateLimitedCommands, tabCompleteRateLimit, kickAfterRateLimitedTabCompletes);
    }
    return new VelocityConfiguration.Advanced();
  }

  private static VelocityConfiguration.Query readQuery(CommentedConfig config) {
    if (config != null) {
      boolean queryEnabled = config.getOrElse("enabled", false);
      int queryPort = config.getIntOrElse("port", 25565);
      String queryMap = config.getOrElse("map", "Velocity");
      boolean showPlugins = config.getOrElse("show-plugins", false);
      return new VelocityConfiguration.Query(queryEnabled, queryPort, queryMap, showPlugins);
    }
    return new VelocityConfiguration.Query();
  }

  private static VelocityConfiguration.Metrics readMetrics(CommentedConfig config) {
    if (config != null) {
      boolean enabled = config.getOrElse("enabled", true);
      return new VelocityConfiguration.Metrics(enabled);
    }
    return new VelocityConfiguration.Metrics();
  }

}
