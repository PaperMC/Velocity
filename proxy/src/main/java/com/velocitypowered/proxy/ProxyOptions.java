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

import com.velocitypowered.api.proxy.server.ServerInfo;
import com.velocitypowered.api.proxy.server.ServerInfoForwardingMode;
import com.velocitypowered.proxy.util.AddressUtil;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import joptsimple.ValueConversionException;
import joptsimple.ValueConverter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Holds parsed command line options.
 */
public final class ProxyOptions {

  /**
   * Logger for reporting command-line parsing or help display issues.
   */
  private static final Logger LOGGER = LogManager.getLogger(ProxyOptions.class);

  /**
   * Whether the user requested help using {@code -h} or {@code --help}.
   */
  private final boolean help;

  /**
   * The optional port override specified with {@code -p} or {@code --port}.
   */
  private final @Nullable Integer port;

  /**
   * Whether the HAProxy protocol override is enabled via {@code --haproxy}.
   */
  private final @Nullable Boolean haproxy;

  /**
   * Whether to ignore all servers listed in the Velocity configuration file.
   */
  private final boolean ignoreConfigServers;

  /**
   * List of servers provided via the {@code --add-server} command-line flag.
   */
  private final List<ServerInfo> servers;

  /**
   * List of additional plugin jars specified via {@code --add-plugin} or {@code --add-extra-plugin-jar}.
   */
  private final List<String> additionalPlugins;

  ProxyOptions(final String[] args) {
    final OptionParser parser = new OptionParser();

    final OptionSpec<Void> help = parser.acceptsAll(Arrays.asList("h", "help"), "Print help")
        .forHelp();
    final OptionSpec<Integer> port = parser.acceptsAll(Arrays.asList("p", "port"),
            "Specify the bind port to be used. The configuration bind port will be ignored.")
        .withRequiredArg().ofType(Integer.class);
    final OptionSpec<Boolean> haproxy = parser.acceptsAll(
            Arrays.asList("haproxy", "haproxy-protocol"),
            "Choose whether to enable haproxy protocol. "
                    + "The configuration haproxy protocol will be ignored.")
        .withRequiredArg().ofType(Boolean.class);
    final OptionSpec<ServerInfo> servers = parser.accepts("add-server",
            "Define a server mapping. "
                    + "You must ensure that server name is not also registered in the config or use --ignore-config-servers.")
        .withRequiredArg().withValuesConvertedBy(new ServerInfoConverter());
    final OptionSpec<Void> ignoreConfigServers = parser.accepts("ignore-config-servers",
            "Skip registering servers from the config file. "
                    + "Useful in dynamic setups or with the --add-server flag.");
    final OptionSpec<String> additionalPlugins = parser.acceptsAll(Arrays.asList("add-plugin", "add-extra-plugin-jar"),
            "Specify paths to extra plugin jars to be loaded in addition to those in the plugins folder. "
                    + "This argument can be specified multiple times, once for each extra plugin jar path."
            ).withRequiredArg().ofType(String.class);
    final OptionSet set = parser.parse(args);

    this.help = set.has(help);
    this.port = port.value(set);
    this.haproxy = haproxy.value(set);
    this.servers = servers.values(set);
    this.ignoreConfigServers = set.has(ignoreConfigServers);
    this.additionalPlugins = additionalPlugins.values(set);

    if (this.help) {
      try {
        parser.printHelpOn(System.out);
      } catch (final IOException e) {
        LOGGER.error("Could not print help", e);
      }
    }
  }

  boolean isHelp() {
    return this.help;
  }

  /**
   * Gets the optional port override provided by the {@code --port} flag.
   *
   * @return the custom bind port, or {@code null} if not set
   */
  public @Nullable Integer getPort() {
    return this.port;
  }

  /**
   * Returns whether the HAProxy protocol is enabled via {@code --haproxy}.
   *
   * @return {@code true} if HAProxy is enabled, {@code false} if disabled, or {@code null} if unset
   */
  public @Nullable Boolean isHaproxy() {
    return this.haproxy;
  }

  /**
   * Returns whether to skip server definitions from the configuration file.
   *
   * @return {@code true} if configuration servers should be ignored
   */
  public boolean isIgnoreConfigServers() {
    return this.ignoreConfigServers;
  }

  /**
   * Returns the list of {@link ServerInfo} instances registered via {@code --add-server}.
   *
   * @return a list of server entries
   */
  public List<ServerInfo> getServers() {
    return this.servers;
  }

  /**
   * Returns the list of additional plugin jars specified via {@code --add-plugin}.
   *
   * @return the list of extra plugin jar paths
   */
  public List<String> getAdditionalPlugins() {
    return this.additionalPlugins;
  }

  private static final class ServerInfoConverter implements ValueConverter<ServerInfo> {

    @Override
    public ServerInfo convert(final String s) {
      String[] split = s.split(":", 2);
      if (split.length < 2) {
        throw new ValueConversionException("Invalid server format. Use <name>:<host>:[port]:[forwardingmode]:[minimumversion]");
      }

      InetSocketAddress address;
      ServerInfoForwardingMode mode = null;
      try {
        if (split.length >= 3) {
          address = AddressUtil.parseAddress(split[1] + ":" + split[2]);
        } else {
          address = AddressUtil.parseAddress(split[1]);
        }
      } catch (IllegalStateException e) {
        throw new ValueConversionException("Invalid hostname for server flag with name: " + split[0]);
      }

      if (split.length == 4) {
        try {
          mode = ServerInfoForwardingMode.valueOf(split[3].toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
          throw new ValueConversionException("Invalid forwarding mode for server flag with name: " + split[0]);
        }
      }

      return new ServerInfo(split[0], address, mode);
    }

    @Override
    public Class<? extends ServerInfo> valueType() {
      return ServerInfo.class;
    }

    @Override
    public String valuePattern() {
      return "name>:<host>:[port]:[forwardingmode]:[minimumversion]";
    }
  }
}
