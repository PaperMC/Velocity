package com.velocitypowered.proxy.config.migration;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import org.apache.logging.log4j.Logger;

/**
 * Creation of the configuration option "accepts-transfers".
 */
public final class TransferIntegrationMigration implements ConfigurationMigration {
  @Override
  public boolean shouldMigrate(final CommentedFileConfig config) {
    return configVersion(config) < 2.7;
  }

  @Override
  public void migrate(final CommentedFileConfig config, final Logger logger) {
    config.set("accepts-transfers", false);
    config.setComment("accepts-transfers", """
            Allows players transferred from other hosts via the
            Transfer packet (Minecraft 1.20.5) to be received.""");
    config.set("config-version", "2.7");
  }
}
