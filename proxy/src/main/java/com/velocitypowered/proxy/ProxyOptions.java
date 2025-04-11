/*
 * Copyright (C) 2018-2023 Velocity Contributors
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
import com.velocitypowered.proxy.util.AddressUtil;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;
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

  private static final Logger logger = LogManager.getLogger(ProxyOptions.class);
  private final boolean help;
  private final @Nullable Integer port;
  private final @Nullable Boolean haproxy;
  private final boolean ignoreConfigServers;
  private final List<ServerInfo> servers;

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
    final OptionSet set = parser.parse(args);

    this.help = set.has(help);
    this.port = port.value(set);
    this.haproxy = haproxy.value(set);
    this.servers = servers.values(set);
    this.ignoreConfigServers = set.has(ignoreConfigServers);

    if (this.help) {
      try {
        parser.printHelpOn(System.out);
      } catch (final IOException e) {
        logger.error("Could not print help", e);
      }
    }
  }

  boolean isHelp() {
    return this.help;
  }

  public @Nullable Integer getPort() {
    return this.port;
  }

  public @Nullable Boolean isHaproxy() {
    return this.haproxy;
  }

  public boolean isIgnoreConfigServers() {
    return this.ignoreConfigServers;
  }

  public List<ServerInfo> getServers() {
    return this.servers;
  }

  private static class ServerInfoConverter implements ValueConverter<ServerInfo> {

    @Override
    public ServerInfo convert(String s) {
      String[] split = s.split(":", 2);
      if (split.length < 2) {
        throw new ValueConversionException("Invalid server format. Use <name>:<address>");
      }
      InetSocketAddress address;
      try {
        address = AddressUtil.parseAddress(split[1]);
      } catch (IllegalStateException e) {
        throw new ValueConversionException("Invalid hostname for server flag with name: " + split[0]);
      }
      return new ServerInfo(split[0], address);
    }

    @Override
    public Class<? extends ServerInfo> valueType() {
      return ServerInfo.class;
    }

    @Override
    public String valuePattern() {
      return "name>:<address";
    }
  }
}
