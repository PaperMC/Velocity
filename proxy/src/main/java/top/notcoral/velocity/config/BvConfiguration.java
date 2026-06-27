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

package top.notcoral.velocity.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.google.gson.annotations.Expose;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * bVelocity-specific configuration, read from {@code bvelocity.toml}.
 *
 * <p>This file is the single home for every setting bVelocity adds or repurposes relative to
 * upstream Velocity, so that {@code velocity.toml} can stay in lock-step with upstream. Today that
 * means the full compression subsystem: the protocol-level {@code compression-threshold} and
 * {@code compression-level} (moved out of {@code velocity.toml}'s {@code [advanced]} block) plus
 * the bVelocity optimization knobs (flush consolidation, output-buffer headroom, compression
 * statistics collection).
 *
 * <p>The file is optional: if {@code bvelocity.toml} is absent on disk, the bundled
 * {@code default-bvelocity.toml} resource seeds it and the documented defaults apply.
 */
public final class BvConfiguration {

  private static final Logger logger = LogManager.getLogger(BvConfiguration.class);

  @Expose
  private final Compression compression;

  @Expose
  private final Optimization optimization;

  private BvConfiguration(Compression compression, Optimization optimization) {
    this.compression = compression;
    this.optimization = optimization;
  }

  /**
   * Reads the bVelocity configuration from {@code bvelocity.toml} next to the proxy jar, seeding it
   * from the bundled {@code default-bvelocity.toml} resource on first run.
   *
   * @param path the path to {@code bvelocity.toml}
   * @return the loaded configuration
   * @throws IOException if the configuration could not be read or written
   */
  public static BvConfiguration read(Path path) throws IOException {
    final URL defaultConfigLocation = BvConfiguration.class.getClassLoader()
        .getResource("default-bvelocity.toml");
    if (defaultConfigLocation == null) {
      throw new RuntimeException("Default bVelocity configuration file does not exist.");
    }

    try (final CommentedFileConfig config = CommentedFileConfig.builder(path)
        .defaultData(defaultConfigLocation)
        .autosave()
        .preserveInsertionOrder()
        .sync()
        .build()) {
      config.load();

      final CommentedConfig compressionConfig = config.get("compression");
      final CommentedConfig optimizationConfig = config.get("optimization");
      return new BvConfiguration(
          new Compression(compressionConfig),
          new Optimization(optimizationConfig)
      );
    }
  }

  /**
   * Validates the configuration, logging any problems and returning whether it is sound.
   *
   * @return {@code true} if the configuration is valid
   */
  public boolean validate() {
    boolean valid = true;

    if (compression.compressionLevel < -1 || compression.compressionLevel > 12) {
      logger.error("Invalid compression level {} in bvelocity.toml", compression.compressionLevel);
      valid = false;
    } else if (compression.compressionLevel == 0) {
      logger.warn("ALL packets going through the proxy will be uncompressed. This will increase "
          + "bandwidth usage.");
    }

    if (compression.compressionThreshold < -1) {
      logger.error("Invalid compression threshold {} in bvelocity.toml",
          compression.compressionThreshold);
      valid = false;
    } else if (compression.compressionThreshold == 0) {
      logger.warn("ALL packets going through the proxy will be compressed. This will compromise "
          + "throughput and increase CPU usage!");
    }

    if (optimization.flushConsolidationThreshold < 1) {
      logger.error("flush-consolidation-threshold must be >= 1, got {}",
          optimization.flushConsolidationThreshold);
      valid = false;
    }

    if (optimization.compressBoundHeadroom < 0) {
      logger.error("compress-bound-headroom must be >= 0, got {}",
          optimization.compressBoundHeadroom);
      valid = false;
    }

    return valid;
  }

  /**
   * Returns the compression settings.
   *
   * @return the compression settings
   */
  public Compression getCompression() {
    return compression;
  }

  /**
   * Returns the optimization settings.
   *
   * @return the optimization settings
   */
  public Optimization getOptimization() {
    return optimization;
  }

  @Override
  public String toString() {
    return "BvConfiguration{"
        + "compression=" + compression
        + ", optimization=" + optimization
        + '}';
  }

  /**
   * Protocol-level compression settings, relocated from {@code velocity.toml}'s
   * {@code [advanced]} block.
   */
  public static final class Compression {

    @Expose
    private int compressionThreshold;
    @Expose
    private int compressionLevel;

    private Compression() {
    }

    private Compression(CommentedConfig config) {
      if (config != null) {
        this.compressionThreshold = config.getIntOrElse("compression-threshold", 128);
        this.compressionLevel = config.getIntOrElse("compression-level", -1);
      } else {
        this.compressionThreshold = 128;
        this.compressionLevel = -1;
      }
    }

    /**
     * Returns the minimum packet size, in bytes, before compression is applied.
     *
     * @return the minimum packet size, in bytes, before compression is applied
     */
    public int getCompressionThreshold() {
      return compressionThreshold;
    }

    /**
     * Returns the configured compression level, or {@code -1} for the bVelocity auto default.
     *
     * @return the configured compression level, or {@code -1} for the bVelocity auto default
     */
    public int getCompressionLevel() {
      return compressionLevel;
    }

    @Override
    public String toString() {
      return "Compression{"
          + "compressionThreshold=" + compressionThreshold
          + ", compressionLevel=" + compressionLevel
          + '}';
    }
  }

  /**
   * bVelocity network and compression-path optimizations.
   */
  public static final class Optimization {

    @Expose
    private boolean flushConsolidationEnabled;
    @Expose
    private int flushConsolidationThreshold;
    @Expose
    private int compressBoundHeadroom;
    @Expose
    private boolean compressionStatsEnabled;

    private Optimization() {
    }

    private Optimization(CommentedConfig config) {
      if (config != null) {
        this.flushConsolidationEnabled = config.getOrElse("flush-consolidation-enabled", true);
        this.flushConsolidationThreshold = config.getIntOrElse(
            "flush-consolidation-threshold", 256);
        this.compressBoundHeadroom = config.getIntOrElse("compress-bound-headroom", 16);
        this.compressionStatsEnabled = config.getOrElse("compression-stats-enabled", true);
      } else {
        this.flushConsolidationEnabled = true;
        this.flushConsolidationThreshold = 256;
        this.compressBoundHeadroom = 16;
        this.compressionStatsEnabled = true;
      }
    }

    /**
     * Returns whether outbound writes are consolidated into fewer flushes.
     *
     * @return whether outbound writes are consolidated into fewer flushes
     */
    public boolean isFlushConsolidationEnabled() {
      return flushConsolidationEnabled;
    }

    /**
     * Returns the maximum number of pending writes buffered before a flush is issued.
     *
     * @return the maximum number of pending writes buffered before a flush is issued
     */
    public int getFlushConsolidationThreshold() {
      return flushConsolidationThreshold;
    }

    /**
     * Returns the headroom, in bytes, added above the uncompressed size when pre-allocating the
     * compressed-output buffer.
     *
     * @return the headroom, in bytes, added above the uncompressed size when pre-allocating the
     *     compressed-output buffer
     */
    public int getCompressBoundHeadroom() {
      return compressBoundHeadroom;
    }

    /**
     * Returns whether per-packet compression statistics are collected for {@code /bv compression}.
     *
     * @return whether per-packet compression statistics are collected for {@code /bv compression}
     */
    public boolean isCompressionStatsEnabled() {
      return compressionStatsEnabled;
    }

    @Override
    public String toString() {
      return "Optimization{"
          + "flushConsolidationEnabled=" + flushConsolidationEnabled
          + ", flushConsolidationThreshold=" + flushConsolidationThreshold
          + ", compressBoundHeadroom=" + compressBoundHeadroom
          + ", compressionStatsEnabled=" + compressionStatsEnabled
          + '}';
    }
  }
}
