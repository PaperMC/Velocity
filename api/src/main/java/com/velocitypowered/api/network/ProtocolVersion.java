/*
 * Copyright (C) 2018-2022 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.network;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Represents each Minecraft protocol version.
 */
public class ProtocolVersion {

  @Deprecated
  private static final AtomicInteger REGISTER_COUNTER = new AtomicInteger();
  private static final Map<Integer, ProtocolVersion> protocols = new HashMap<>(); // TODO: Move internally and FU?

  public static ProtocolVersion UNKNOWN = register(new ProtocolVersion(-1, "Unknown"));
  public static ProtocolVersion LEGACY = register(new ProtocolVersion(-2, "Legacy"));
  public static ProtocolVersion MINECRAFT_1_7_2 = register(new ProtocolVersion(4, "1.7.2", "1.7.3", "1.7.4", "1.7.5"));
  public static ProtocolVersion MINECRAFT_1_7_6 = register(new ProtocolVersion(5, "1.7.6", "1.7.7", "1.7.8", "1.7.9", "1.7.10"));
  public static ProtocolVersion MINECRAFT_1_8 = register(new ProtocolVersion(47, "1.8", "1.8.1", "1.8.2", "1.8.3", "1.8.4", "1.8.5", "1.8.6", "1.8.7", "1.8.8", "1.8.9"));
  public static ProtocolVersion MINECRAFT_1_9 = register(new ProtocolVersion(107, "1.9"));
  public static ProtocolVersion MINECRAFT_1_9_1 = register(new ProtocolVersion(108, "1.9.1"));
  public static ProtocolVersion MINECRAFT_1_9_2 = register(new ProtocolVersion(109, "1.9.2"));
  public static ProtocolVersion MINECRAFT_1_9_4 = register(new ProtocolVersion(110, "1.9.3", "1.9.4"));
  public static ProtocolVersion MINECRAFT_1_10 = register(new ProtocolVersion(210, "1.10", "1.10.1", "1.10.2"));
  public static ProtocolVersion MINECRAFT_1_11 = register(new ProtocolVersion(315, "1.11"));
  public static ProtocolVersion MINECRAFT_1_11_1 = register(new ProtocolVersion(316, "1.11.1", "1.11.2"));
  public static ProtocolVersion MINECRAFT_1_12 = register(new ProtocolVersion(335, "1.12"));
  public static ProtocolVersion MINECRAFT_1_12_1 = register(new ProtocolVersion(338, "1.12.1"));
  public static ProtocolVersion MINECRAFT_1_12_2 = register(new ProtocolVersion(340, "1.12.2"));
  public static ProtocolVersion MINECRAFT_1_13 = register(new ProtocolVersion(393, "1.13"));
  public static ProtocolVersion MINECRAFT_1_13_1 = register(new ProtocolVersion(401, "1.13.1"));
  public static ProtocolVersion MINECRAFT_1_13_2 = register(new ProtocolVersion(404, "1.13.2"));
  public static ProtocolVersion MINECRAFT_1_14 = register(new ProtocolVersion(477, "1.14"));
  public static ProtocolVersion MINECRAFT_1_14_1 = register(new ProtocolVersion(480, "1.14.1"));
  public static ProtocolVersion MINECRAFT_1_14_2 = register(new ProtocolVersion(485, "1.14.2"));
  public static ProtocolVersion MINECRAFT_1_14_3 = register(new ProtocolVersion(490, "1.14.3"));
  public static ProtocolVersion MINECRAFT_1_14_4 = register(new ProtocolVersion(498, "1.14.4"));
  public static ProtocolVersion MINECRAFT_1_15 = register(new ProtocolVersion(573, "1.15"));
  public static ProtocolVersion MINECRAFT_1_15_1 = register(new ProtocolVersion(575, "1.15.1"));
  public static ProtocolVersion MINECRAFT_1_15_2 = register(new ProtocolVersion(578, "1.15.2"));
  public static ProtocolVersion MINECRAFT_1_16 = register(new ProtocolVersion(735, "1.16"));
  public static ProtocolVersion MINECRAFT_1_16_1 = register(new ProtocolVersion(736, "1.16.1"));
  public static ProtocolVersion MINECRAFT_1_16_2 = register(new ProtocolVersion(751, "1.16.2"));
  public static ProtocolVersion MINECRAFT_1_16_3 = register(new ProtocolVersion(753, "1.16.3"));
  public static ProtocolVersion MINECRAFT_1_16_4 = register(new ProtocolVersion(754, "1.16.4", "1.16.5"));
  public static ProtocolVersion MINECRAFT_1_17 = register(new ProtocolVersion(755, "1.17"));
  public static ProtocolVersion MINECRAFT_1_17_1 = register(new ProtocolVersion(756, "1.17.1"));
  public static ProtocolVersion MINECRAFT_1_18 = register(new ProtocolVersion(757, "1.18", "1.18.1"));
  public static ProtocolVersion MINECRAFT_1_18_2 = register(new ProtocolVersion(758, "1.18.2"));
  public static ProtocolVersion MINECRAFT_1_19 = register(new ProtocolVersion(759, "1.19"));
  public static ProtocolVersion MINECRAFT_1_19_1 = register(new ProtocolVersion(760, "1.19.1", "1.19.2"));
  public static ProtocolVersion MINECRAFT_1_19_3 = register(new ProtocolVersion(761, "1.19.3"));
  public static ProtocolVersion MINECRAFT_1_19_4 = register(new ProtocolVersion(762, "1.19.4"));
  public static ProtocolVersion MINECRAFT_1_20 = register(new ProtocolVersion(763, "1.20", "1.20.1"));

  private static ProtocolVersion register(ProtocolVersion protocolversion) {
    protocols.put(protocolversion.protocol != -1 ? protocolversion.protocol : protocolversion.snapshotProtocol, protocolversion);
    return protocolversion;
  }

  private static final int SNAPSHOT_BIT = 30;

  private final int protocol;
  private final int snapshotProtocol;
  private final String[] names;
  // Because we need a replacement for ordinals...
  @Deprecated
  private final int registered;

  /**
   * Represents the lowest supported version.
   */
  public static final ProtocolVersion MINIMUM_VERSION = MINECRAFT_1_7_2;
  /**
   * Represents the highest supported version.
   */
  public static final ProtocolVersion MAXIMUM_VERSION = values()[values().length - 1];

  /**
   * The user-friendly representation of the lowest and highest supported versions.
   */
  public static final String SUPPORTED_VERSION_STRING = String
      .format("%s-%s", MINIMUM_VERSION.getVersionIntroducedIn(),
          MAXIMUM_VERSION.getMostRecentSupportedVersion());

  /**
   * A map linking the protocol version number to its {@link ProtocolVersion} representation.
   */
  public static final ImmutableMap<Integer, ProtocolVersion> ID_TO_PROTOCOL_CONSTANT;

  static {
    Map<Integer, ProtocolVersion> versions = new HashMap<>();
    for (ProtocolVersion version : values()) {
      // For versions where the snapshot is compatible with the prior release version, Mojang will
      // default to that. Follow that behavior since there is precedent (all the Minecraft 1.8
      // minor releases use the same protocol version).
      versions.putIfAbsent(version.protocol, version);
      if (version.snapshotProtocol != -1) {
        versions.put(version.snapshotProtocol, version);
      }
    }

    ID_TO_PROTOCOL_CONSTANT = ImmutableMap.copyOf(versions);
  }

  /**
   * A set containing all the protocols that the proxy actually supports, excluding special-purpose
   * "versions" like {@link #LEGACY} and {@link #UNKNOWN}.
   */
  public static final Set<ProtocolVersion> SUPPORTED_VERSIONS;

  static {
    Set<ProtocolVersion> versions = new HashSet<>();
    for (ProtocolVersion value : values()) {
      if (!value.isUnknown() && !value.isLegacy()) {
        versions.add(value);
      }
    }

    SUPPORTED_VERSIONS = Collections.unmodifiableSet(versions);
  }

  public static ProtocolVersion[] values() {
    return protocols.values().toArray(new ProtocolVersion[0]);
  }

  ProtocolVersion(int protocol, String... names) {
    this(protocol, -1, names);
  }

  ProtocolVersion(int protocol, int snapshotProtocol, String... names) {
    if (snapshotProtocol != -1) {
      this.snapshotProtocol = (1 << SNAPSHOT_BIT) | snapshotProtocol;
    } else {
      this.snapshotProtocol = -1;
    }

    this.protocol = protocol;
    this.names = names;
    this.registered = REGISTER_COUNTER.getAndIncrement();
  }

  /**
   * Returns the protocol as an int.
   *
   * @return the protocol version
   */
  public int getProtocol() {
    return protocol == -1 ? snapshotProtocol : protocol;
  }

  /**
   * Returns the user-friendly name for this protocol.
   *
   * @return the protocol name
   * @deprecated A protocol may be shared by multiple versions. Use @link{#getVersionIntroducedIn()}
   *     or @link{#getVersionsSupportedBy()} to get more accurate version names.
   */
  @Deprecated
  public String getName() {
    return getVersionIntroducedIn();
  }

  /**
   * Returns the user-friendly name of the version
   * this protocol was introduced in.
   *
   * @return the version name
   */
  public String getVersionIntroducedIn() {
    return names[0];
  }

  /**
   * Returns the user-friendly name of the last
   * version this protocol is valid for.
   *
   * @return the version name
   */
  public String getMostRecentSupportedVersion() {
    return names[names.length - 1];
  }

  /**
   * Returns all versions this protocol is valid for.
   *
   * @return the version names
   */
  public List<String> getVersionsSupportedBy() {
    return ImmutableList.copyOf(names);
  }

  /**
   * Gets the {@link ProtocolVersion} for the given protocol.
   *
   * @param protocol the protocol as an int
   * @return the protocol version
   */
  public static ProtocolVersion getProtocolVersion(int protocol) {
    return ID_TO_PROTOCOL_CONSTANT.getOrDefault(protocol, UNKNOWN);
  }

  /**
   * Returns whether the protocol is supported.
   *
   * @param protocol the protocol as an int
   * @return if the protocol supported
   */
  public static boolean isSupported(int protocol) {
    ProtocolVersion version = ID_TO_PROTOCOL_CONSTANT.get(protocol);

    return version != null && !version.isUnknown();
  }

  /**
   * Returns whether the {@link ProtocolVersion} is supported.
   *
   * @param version the protocol version
   * @return if the protocol supported
   */
  public static boolean isSupported(ProtocolVersion version) {
    return version != null && !version.isUnknown();
  }

  /**
   * Returns whether this {@link ProtocolVersion} is unknown to the proxy.
   *
   * @return if the protocol is unknown
   */
  public boolean isUnknown() {
    return this == UNKNOWN;
  }

  /**
   * Returns whether this {@link ProtocolVersion} is a legacy protocol.
   *
   * @return if the protocol is legacy
   */
  public boolean isLegacy() {
    return this == LEGACY;
  }

  @Override
  public String toString() {
    return getVersionIntroducedIn();
  }

  @Deprecated
  public int compareTo(ProtocolVersion protocolVersion) {
    return this.registered - protocolVersion.registered;
  }
}
