package com.velocitypowered.proxy;

import java.io.IOException;
import java.util.Arrays;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.Nullable;

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
