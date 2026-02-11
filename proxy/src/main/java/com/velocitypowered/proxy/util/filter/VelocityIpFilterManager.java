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

package com.velocitypowered.proxy.util.filter;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.proxy.filter.IpFilterManager;
import java.net.InetAddress;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Implementation of {@link IpFilterManager}.
 */
public class VelocityIpFilterManager implements IpFilterManager {

  private static final Logger logger = LogManager.getLogger(VelocityIpFilterManager.class);

  private final ConcurrentMap<String, IpFilterEntryImpl> blacklist = new ConcurrentHashMap<>();
  private final ConcurrentMap<String, IpFilterEntryImpl> whitelist = new ConcurrentHashMap<>();
  private volatile boolean maintenanceMode = false;
  private volatile Component maintenanceMessage = Component.translatable("velocity.filter.maintenance");

  // ==================== Blacklist Operations ====================

  @Override
  public void blacklist(InetAddress address, @Nullable String reason) {
    blacklist(address, null, reason);
  }

  @Override
  public void blacklist(InetAddress address, Duration duration, @Nullable String reason) {
    Preconditions.checkNotNull(address, "address");
    String key = address.getHostAddress();
    Instant expiration = duration != null ? Instant.now().plus(duration) : null;
    blacklist.put(key, new IpFilterEntryImpl(key, false, reason, expiration));
    logger.info("Blacklisted IP: {} (reason: {}, expires: {})", key, reason, expiration);
  }

  @Override
  public void blacklistCidr(String cidr, @Nullable String reason) {
    blacklistCidr(cidr, null, reason);
  }

  @Override
  public void blacklistCidr(String cidr, Duration duration, @Nullable String reason) {
    Preconditions.checkNotNull(cidr, "cidr");
    validateCidr(cidr);
    Instant expiration = duration != null ? Instant.now().plus(duration) : null;
    blacklist.put(cidr, new IpFilterEntryImpl(cidr, true, reason, expiration));
    logger.info("Blacklisted CIDR: {} (reason: {}, expires: {})", cidr, reason, expiration);
  }

  @Override
  public boolean unblacklist(InetAddress address) {
    Preconditions.checkNotNull(address, "address");
    String key = address.getHostAddress();
    IpFilterEntry removed = blacklist.remove(key);
    if (removed != null) {
      logger.info("Removed IP from blacklist: {}", key);
      return true;
    }
    return false;
  }

  @Override
  public boolean unblacklistCidr(String cidr) {
    Preconditions.checkNotNull(cidr, "cidr");
    IpFilterEntry removed = blacklist.remove(cidr);
    if (removed != null) {
      logger.info("Removed CIDR from blacklist: {}", cidr);
      return true;
    }
    return false;
  }

  @Override
  public boolean isBlacklisted(InetAddress address) {
    return getBlacklistEntry(address).isPresent();
  }

  @Override
  public Optional<IpFilterEntry> getBlacklistEntry(InetAddress address) {
    Preconditions.checkNotNull(address, "address");
    cleanupExpired(blacklist);

    // Check direct IP match
    String ip = address.getHostAddress();
    IpFilterEntryImpl directMatch = blacklist.get(ip);
    if (directMatch != null && !directMatch.isExpired()) {
      return Optional.of(directMatch);
    }

    // Check CIDR matches
    for (IpFilterEntryImpl entry : blacklist.values()) {
      if (entry.isCidr() && !entry.isExpired() && matchesCidr(address, entry.getPattern())) {
        return Optional.of(entry);
      }
    }

    return Optional.empty();
  }

  @Override
  public Collection<IpFilterEntry> getBlacklistEntries() {
    cleanupExpired(blacklist);
    return Collections.unmodifiableCollection(
        blacklist.values().stream()
            .filter(e -> !e.isExpired())
            .collect(Collectors.toList()));
  }

  @Override
  public void clearBlacklist() {
    blacklist.clear();
    logger.info("Cleared blacklist");
  }

  // ==================== Whitelist Operations ====================

  @Override
  public void whitelist(InetAddress address) {
    Preconditions.checkNotNull(address, "address");
    String key = address.getHostAddress();
    whitelist.put(key, new IpFilterEntryImpl(key, false, null, null));
    logger.info("Whitelisted IP: {}", key);
  }

  @Override
  public void whitelistCidr(String cidr) {
    Preconditions.checkNotNull(cidr, "cidr");
    validateCidr(cidr);
    whitelist.put(cidr, new IpFilterEntryImpl(cidr, true, null, null));
    logger.info("Whitelisted CIDR: {}", cidr);
  }

  @Override
  public boolean unwhitelist(InetAddress address) {
    Preconditions.checkNotNull(address, "address");
    String key = address.getHostAddress();
    IpFilterEntry removed = whitelist.remove(key);
    if (removed != null) {
      logger.info("Removed IP from whitelist: {}", key);
      return true;
    }
    return false;
  }

  @Override
  public boolean unwhitelistCidr(String cidr) {
    Preconditions.checkNotNull(cidr, "cidr");
    IpFilterEntry removed = whitelist.remove(cidr);
    if (removed != null) {
      logger.info("Removed CIDR from whitelist: {}", cidr);
      return true;
    }
    return false;
  }

  @Override
  public boolean isWhitelisted(InetAddress address) {
    Preconditions.checkNotNull(address, "address");

    // Check direct IP match
    String ip = address.getHostAddress();
    if (whitelist.containsKey(ip)) {
      return true;
    }

    // Check CIDR matches
    for (IpFilterEntryImpl entry : whitelist.values()) {
      if (entry.isCidr() && matchesCidr(address, entry.getPattern())) {
        return true;
      }
    }

    return false;
  }

  @Override
  public Collection<IpFilterEntry> getWhitelistEntries() {
    return Collections.unmodifiableCollection(whitelist.values());
  }

  @Override
  public void clearWhitelist() {
    whitelist.clear();
    logger.info("Cleared whitelist");
  }

  // ==================== Maintenance Mode ====================

  @Override
  public void setMaintenanceMode(boolean enabled) {
    this.maintenanceMode = enabled;
    logger.info("Maintenance mode {}", enabled ? "enabled" : "disabled");
  }

  @Override
  public boolean isMaintenanceMode() {
    return maintenanceMode;
  }

  @Override
  public void setMaintenanceMessage(Component message) {
    Preconditions.checkNotNull(message, "message");
    this.maintenanceMessage = message;
  }

  @Override
  public Component getMaintenanceMessage() {
    return maintenanceMessage;
  }

  // ==================== Connection Check ====================

  @Override
  public FilterResult checkConnection(InetAddress address) {
    Preconditions.checkNotNull(address, "address");

    // Check blacklist first
    if (isBlacklisted(address)) {
      return FilterResult.DENIED_BLACKLISTED;
    }

    // Check maintenance mode
    if (maintenanceMode && !isWhitelisted(address)) {
      return FilterResult.DENIED_MAINTENANCE;
    }

    return FilterResult.ALLOWED;
  }

  // ==================== Helper Methods ====================

  private void validateCidr(String cidr) {
    if (!cidr.contains("/")) {
      throw new IllegalArgumentException("Invalid CIDR notation: " + cidr);
    }
    String[] parts = cidr.split("/");
    if (parts.length != 2) {
      throw new IllegalArgumentException("Invalid CIDR notation: " + cidr);
    }
    try {
      int prefix = Integer.parseInt(parts[1]);
      if (prefix < 0 || prefix > 128) {
        throw new IllegalArgumentException("Invalid CIDR prefix length: " + prefix);
      }
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Invalid CIDR prefix: " + parts[1]);
    }
  }

  private boolean matchesCidr(InetAddress address, String cidr) {
    try {
      String[] parts = cidr.split("/");
      InetAddress cidrAddress = InetAddress.getByName(parts[0]);
      int prefixLength = Integer.parseInt(parts[1]);

      byte[] addressBytes = address.getAddress();
      byte[] cidrBytes = cidrAddress.getAddress();

      if (addressBytes.length != cidrBytes.length) {
        return false;
      }

      int fullBytes = prefixLength / 8;
      int remainingBits = prefixLength % 8;

      for (int i = 0; i < fullBytes; i++) {
        if (addressBytes[i] != cidrBytes[i]) {
          return false;
        }
      }

      if (remainingBits > 0 && fullBytes < addressBytes.length) {
        int mask = 0xFF << (8 - remainingBits);
        if ((addressBytes[fullBytes] & mask) != (cidrBytes[fullBytes] & mask)) {
          return false;
        }
      }

      return true;
    } catch (Exception e) {
      logger.warn("Failed to match CIDR {}: {}", cidr, e.getMessage());
      return false;
    }
  }

  private void cleanupExpired(ConcurrentMap<String, IpFilterEntryImpl> map) {
    map.entrySet().removeIf(entry -> entry.getValue().isExpired());
  }

  /**
   * Implementation of {@link IpFilterEntry}.
   */
  private static class IpFilterEntryImpl implements IpFilterEntry {
    private final String pattern;
    private final boolean isCidr;
    private final @Nullable String reason;
    private final @Nullable Instant expiration;
    private final Instant createdAt;

    IpFilterEntryImpl(String pattern, boolean isCidr, @Nullable String reason,
                      @Nullable Instant expiration) {
      this.pattern = pattern;
      this.isCidr = isCidr;
      this.reason = reason;
      this.expiration = expiration;
      this.createdAt = Instant.now();
    }

    @Override
    public String getPattern() {
      return pattern;
    }

    @Override
    public boolean isCidr() {
      return isCidr;
    }

    @Override
    public Optional<String> getReason() {
      return Optional.ofNullable(reason);
    }

    @Override
    public Optional<Instant> getExpiration() {
      return Optional.ofNullable(expiration);
    }

    @Override
    public Instant getCreatedAt() {
      return createdAt;
    }

    @Override
    public String toString() {
      return "IpFilterEntry{"
          + "pattern='" + pattern + '\''
          + ", isCidr=" + isCidr
          + ", reason='" + reason + '\''
          + ", expiration=" + expiration
          + '}';
    }
  }
}

