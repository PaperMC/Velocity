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

import java.io.IOException;
import java.util.Arrays;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
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

  ProxyOptions(final String[] args) {
    final OptionParser parser = new OptionParser();

    final OptionSpec<Void> help = parser.acceptsAll(Arrays.asList("h", "help"), "Print help")
        .forHelp();
    final OptionSpec<Integer> port = parser.acceptsAll(Arrays.asList("p", "port"),
            "Specify the bind port to be used. The configuration bind port will be ignored.")
        .withRequiredArg().ofType(Integer.class);
    final OptionSet set = parser.parse(args);

    this.help = set.has(help);
    this.port = port.value(set);

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
}
