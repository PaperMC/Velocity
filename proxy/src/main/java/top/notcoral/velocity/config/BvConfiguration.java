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
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
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

  /**
   * The legacy serializer used to render MiniMessage-parsed brand text into the section-symbol
   * ({@code §}) form the Minecraft client expects inside the {@code minecraft:brand} payload.
   * Hex colors are emitted so modern clients render them correctly.
   */
  private static final LegacyComponentSerializer BRAND_LEGACY_SERIALIZER =
      LegacyComponentSerializer.builder().hexColors().build();

  @Expose
  private final Compression compression;

  @Expose
  private final Optimization optimization;

  @Expose
  private final Brand brand;

  private BvConfiguration(Compression compression, Optimization optimization, Brand brand) {
    this.compression = compression;
    this.optimization = optimization;
    this.brand = brand;
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
      final CommentedConfig brandConfig = config.get("brand");
      return new BvConfiguration(
          new Compression(compressionConfig),
          new Optimization(optimizationConfig),
          new Brand(brandConfig)
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

    if (optimization.eventLoopThreads < 0) {
      logger.error("event-loop-threads must be >= 0 (0 = Netty default), got {}",
          optimization.eventLoopThreads);
      valid = false;
    }

    if (optimization.bossThreads < 1) {
      logger.error("boss-threads must be >= 1, got {}", optimization.bossThreads);
      valid = false;
    }

    if (optimization.dnsResolverThreads < 1) {
      logger.error("dns-resolver-threads must be >= 1, got {}", optimization.dnsResolverThreads);
      valid = false;
    }

    if (optimization.dnsCacheTtlSeconds < 0) {
      logger.error("dns-cache-ttl-seconds must be >= 0, got {}",
          optimization.dnsCacheTtlSeconds);
      valid = false;
    }

    if (optimization.dnsNegativeCacheTtlSeconds < 0) {
      logger.error("dns-negative-cache-ttl-seconds must be >= 0, got {}",
          optimization.dnsNegativeCacheTtlSeconds);
      valid = false;
    }

    if (brand.mode == BrandMode.CUSTOM && brand.customBrand.isBlank()) {
      logger.error("brand-mode is 'custom' but custom-brand is empty in bvelocity.toml; "
          + "set a value or switch brand-mode back to 'default'");
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

  /**
   * Returns the F3 server-brand settings.
   *
   * @return the brand settings
   */
  public Brand getBrand() {
    return brand;
  }

  @Override
  public String toString() {
    return "BvConfiguration{"
        + "compression=" + compression
        + ", optimization=" + optimization
        + ", brand=" + brand
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
    @Expose
    private int eventLoopThreads;
    @Expose
    private int bossThreads;
    @Expose
    private int dnsResolverThreads;
    @Expose
    private int dnsCacheTtlSeconds;
    @Expose
    private int dnsNegativeCacheTtlSeconds;

    private Optimization() {
    }

    private Optimization(CommentedConfig config) {
      if (config != null) {
        this.flushConsolidationEnabled = config.getOrElse("flush-consolidation-enabled", true);
        this.flushConsolidationThreshold = config.getIntOrElse(
            "flush-consolidation-threshold", 256);
        this.compressBoundHeadroom = config.getIntOrElse("compress-bound-headroom", 16);
        this.compressionStatsEnabled = config.getOrElse("compression-stats-enabled", true);
        this.eventLoopThreads = config.getIntOrElse("event-loop-threads", 0);
        this.bossThreads = config.getIntOrElse("boss-threads", 1);
        this.dnsResolverThreads = config.getIntOrElse("dns-resolver-threads", 2);
        this.dnsCacheTtlSeconds = config.getIntOrElse("dns-cache-ttl-seconds", 30);
        this.dnsNegativeCacheTtlSeconds =
            config.getIntOrElse("dns-negative-cache-ttl-seconds", 3);
      } else {
        this.flushConsolidationEnabled = true;
        this.flushConsolidationThreshold = 256;
        this.compressBoundHeadroom = 16;
        this.compressionStatsEnabled = true;
        this.eventLoopThreads = 0;
        this.bossThreads = 1;
        this.dnsResolverThreads = 2;
        this.dnsCacheTtlSeconds = 30;
        this.dnsNegativeCacheTtlSeconds = 3;
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

    /**
     * Returns the number of Netty worker (event-loop) threads, or {@code 0} for Netty's default of
     * {@code 2 * availableProcessors()}.
     *
     * @return the worker thread count, or {@code 0} for the Netty default
     */
    public int getEventLoopThreads() {
      return eventLoopThreads;
    }

    /**
     * Returns the number of Netty boss (acceptor) threads. Ignored when SO_REUSEPORT is enabled
     * (each worker binds its own socket then).
     *
     * @return the boss thread count
     */
    public int getBossThreads() {
      return bossThreads;
    }

    /**
     * Returns the number of threads in the DNS resolver pool.
     *
     * @return the DNS resolver thread count
     */
    public int getDnsResolverThreads() {
      return dnsResolverThreads;
    }

    /**
     * Returns the positive DNS cache TTL, in seconds.
     *
     * @return the positive DNS cache TTL, in seconds
     */
    public int getDnsCacheTtlSeconds() {
      return dnsCacheTtlSeconds;
    }

    /**
     * Returns the negative DNS cache TTL, in seconds. {@code 0} disables negative caching.
     *
     * @return the negative DNS cache TTL, in seconds
     */
    public int getDnsNegativeCacheTtlSeconds() {
      return dnsNegativeCacheTtlSeconds;
    }

    @Override
    public String toString() {
      return "Optimization{"
          + "flushConsolidationEnabled=" + flushConsolidationEnabled
          + ", flushConsolidationThreshold=" + flushConsolidationThreshold
          + ", compressBoundHeadroom=" + compressBoundHeadroom
          + ", compressionStatsEnabled=" + compressionStatsEnabled
          + ", eventLoopThreads=" + eventLoopThreads
          + ", bossThreads=" + bossThreads
          + ", dnsResolverThreads=" + dnsResolverThreads
          + ", dnsCacheTtlSeconds=" + dnsCacheTtlSeconds
          + ", dnsNegativeCacheTtlSeconds=" + dnsNegativeCacheTtlSeconds
          + '}';
    }
  }

  /**
   * Controls the F3 server-brand text shown in the upper-right of the client debug screen.
   *
   * <p>In {@link BrandMode#CUSTOM} the {@code custom-brand} string is parsed through MiniMessage
   * ({@link MiniMessage#miniMessage()}) and serialized to the section-symbol form the client
   * expects. The serialized result is cached for the lifetime of the configuration object, so a
   * reload picks up new text via a fresh {@link BvConfiguration} instance.
   */
  public static final class Brand {

    @Expose
    private BrandMode mode;

    @Expose
    private String customBrand;

    private transient String renderedCustomBrand;

    private Brand() {
    }

    private Brand(CommentedConfig config) {
      if (config != null) {
        this.mode = config.getEnumOrElse("mode", BrandMode.DEFAULT);
        this.customBrand = config.getOrElse("custom-brand", "<gradient:#ff7eb3:#ff758f>Custom Velocity</gradient>");
      } else {
        this.mode = BrandMode.DEFAULT;
        this.customBrand = "<gradient:#ff7eb3:#ff758f>Custom Velocity</gradient>";
      }
      this.renderedCustomBrand = renderCustomBrand();
    }

    /**
     * Returns the brand rendering mode.
     *
     * @return the brand rendering mode
     */
    public BrandMode getMode() {
      return mode;
    }

    /**
     * Returns the raw MiniMessage {@code custom-brand} string, as written in {@code bvelocity.toml}.
     *
     * @return the raw custom-brand string
     */
    public String getCustomBrand() {
      return customBrand;
    }

    /**
     * Returns the {@code custom-brand} parsed through MiniMessage and serialized to the
     * section-symbol ({@code §}) form the Minecraft client renders inside the F3 brand field.
     *
     * @return the serialized custom brand, or an empty string when the configured text is blank
     */
    public String getRenderedCustomBrand() {
      if (renderedCustomBrand == null) {
        renderedCustomBrand = renderCustomBrand();
      }
      return renderedCustomBrand;
    }

    private String renderCustomBrand() {
      if (customBrand == null || customBrand.isBlank()) {
        return "";
      }
      try {
        return BRAND_LEGACY_SERIALIZER.serialize(MiniMessage.miniMessage().deserialize(customBrand));
      } catch (Exception e) {
        logger.warn("Failed to parse custom-brand MiniMessage '{}'; using the raw text", customBrand, e);
        return customBrand;
      }
    }

    @Override
    public String toString() {
      return "Brand{"
          + "mode=" + mode
          + ", customBrand='" + customBrand + '\''
          + '}';
    }
  }
}
