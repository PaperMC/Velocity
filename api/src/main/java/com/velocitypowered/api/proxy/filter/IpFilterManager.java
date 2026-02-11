/*
 * Copyright (C) 2018-2026 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.proxy.filter;

import java.net.InetAddress;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Manages IP-based filtering for the proxy, including whitelist and blacklist functionality.
 *
 * @since 3.5.0
 */
public interface IpFilterManager {

  // ==================== Blacklist Operations ====================

  /**
   * Adds an IP address to the blacklist permanently.
   *
   * @param address the IP address to blacklist
   * @param reason the reason for blacklisting
   */
  void blacklist(InetAddress address, @Nullable String reason);

  /**
   * Adds an IP address to the blacklist for a specified duration.
   *
   * @param address the IP address to blacklist
   * @param duration the duration of the ban
   * @param reason the reason for blacklisting
   */
  void blacklist(InetAddress address, Duration duration, @Nullable String reason);

  /**
   * Adds a CIDR range to the blacklist permanently.
   *
   * @param cidr the CIDR notation (e.g., "192.168.0.0/24")
   * @param reason the reason for blacklisting
   * @throws IllegalArgumentException if the CIDR notation is invalid
   */
  void blacklistCidr(String cidr, @Nullable String reason);

  /**
   * Adds a CIDR range to the blacklist for a specified duration.
   *
   * @param cidr the CIDR notation (e.g., "192.168.0.0/24")
   * @param duration the duration of the ban
   * @param reason the reason for blacklisting
   * @throws IllegalArgumentException if the CIDR notation is invalid
   */
  void blacklistCidr(String cidr, Duration duration, @Nullable String reason);

  /**
   * Removes an IP address from the blacklist.
   *
   * @param address the IP address to unblacklist
   * @return {@code true} if the address was removed, {@code false} if it wasn't blacklisted
   */
  boolean unblacklist(InetAddress address);

  /**
   * Removes a CIDR range from the blacklist.
   *
   * @param cidr the CIDR notation to remove
   * @return {@code true} if the range was removed, {@code false} if it wasn't blacklisted
   */
  boolean unblacklistCidr(String cidr);

  /**
   * Checks if an IP address is blacklisted.
   *
   * @param address the IP address to check
   * @return {@code true} if the address is blacklisted
   */
  boolean isBlacklisted(InetAddress address);

  /**
   * Gets the blacklist entry for an IP address.
   *
   * @param address the IP address to look up
   * @return the entry, or empty if not blacklisted
   */
  Optional<IpFilterEntry> getBlacklistEntry(InetAddress address);

  /**
   * Returns all blacklisted entries.
   *
   * @return an unmodifiable collection of blacklist entries
   */
  Collection<IpFilterEntry> getBlacklistEntries();

  /**
   * Clears all entries from the blacklist.
   */
  void clearBlacklist();

  // ==================== Whitelist Operations ====================

  /**
   * Adds an IP address to the whitelist.
   *
   * @param address the IP address to whitelist
   */
  void whitelist(InetAddress address);

  /**
   * Adds a CIDR range to the whitelist.
   *
   * @param cidr the CIDR notation (e.g., "192.168.0.0/24")
   * @throws IllegalArgumentException if the CIDR notation is invalid
   */
  void whitelistCidr(String cidr);

  /**
   * Removes an IP address from the whitelist.
   *
   * @param address the IP address to remove
   * @return {@code true} if the address was removed
   */
  boolean unwhitelist(InetAddress address);

  /**
   * Removes a CIDR range from the whitelist.
   *
   * @param cidr the CIDR notation to remove
   * @return {@code true} if the range was removed
   */
  boolean unwhitelistCidr(String cidr);

  /**
   * Checks if an IP address is whitelisted.
   *
   * @param address the IP address to check
   * @return {@code true} if the address is whitelisted
   */
  boolean isWhitelisted(InetAddress address);

  /**
   * Returns all whitelisted entries.
   *
   * @return an unmodifiable collection of whitelist entries
   */
  Collection<IpFilterEntry> getWhitelistEntries();

  /**
   * Clears all entries from the whitelist.
   */
  void clearWhitelist();

  // ==================== Maintenance Mode ====================

  /**
   * Enables or disables maintenance mode.
   * When enabled, only whitelisted IPs can connect.
   *
   * @param enabled whether maintenance mode should be enabled
   */
  void setMaintenanceMode(boolean enabled);

  /**
   * Checks if maintenance mode is enabled.
   *
   * @return {@code true} if maintenance mode is enabled
   */
  boolean isMaintenanceMode();

  /**
   * Sets the kick message shown to players during maintenance mode.
   *
   * @param message the message to show
   */
  void setMaintenanceMessage(Component message);

  /**
   * Gets the kick message shown during maintenance mode.
   *
   * @return the maintenance message
   */
  Component getMaintenanceMessage();

  // ==================== Connection Check ====================

  /**
   * Checks if a connection from the given IP address is allowed.
   * This considers both blacklist, whitelist, and maintenance mode.
   *
   * @param address the IP address to check
   * @return the result of the filter check
   */
  FilterResult checkConnection(InetAddress address);

  /**
   * Represents an entry in the IP filter.
   */
  interface IpFilterEntry {
    /**
     * Returns the pattern (IP or CIDR) of this entry.
     *
     * @return the pattern string
     */
    String getPattern();

    /**
     * Returns whether this is a CIDR range.
     *
     * @return {@code true} if this is a CIDR range
     */
    boolean isCidr();

    /**
     * Returns the reason for this entry, if any.
     *
     * @return the reason, or empty
     */
    Optional<String> getReason();

    /**
     * Returns when this entry expires, if it's temporary.
     *
     * @return the expiration time, or empty if permanent
     */
    Optional<Instant> getExpiration();

    /**
     * Returns when this entry was created.
     *
     * @return the creation time
     */
    Instant getCreatedAt();

    /**
     * Returns whether this entry has expired.
     *
     * @return {@code true} if expired
     */
    default boolean isExpired() {
      return getExpiration().map(exp -> Instant.now().isAfter(exp)).orElse(false);
    }
  }

  /**
   * Result of an IP filter check.
   */
  enum FilterResult {
    /**
     * The connection is allowed.
     */
    ALLOWED,

    /**
     * The connection is denied because the IP is blacklisted.
     */
    DENIED_BLACKLISTED,

    /**
     * The connection is denied because maintenance mode is active
     * and the IP is not whitelisted.
     */
    DENIED_MAINTENANCE
  }
}

