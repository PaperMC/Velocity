/*
 * Copyright (C) 2025 Velocity Contributors
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

package com.velocitypowered.proxy.protocol.registry;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.PacketCodec;
import com.velocitypowered.proxy.protocol.ProtocolUtils.Direction;
import io.netty.util.collection.IntObjectHashMap;
import io.netty.util.collection.IntObjectMap;
import it.unimi.dsi.fastutil.Hash.Strategy;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenCustomHashMap;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A multi-version packet registry that supports different packet ID mappings across protocol
 * versions. This implementation optimizes memory usage by reusing mapping data structures across
 * versions when the mappings are identical.
 */
public class MultiVersionPacketRegistry implements ProtocolToPacketRegistry {

  private final Map<ProtocolVersion, VersionMapping> versionMappings;

  private MultiVersionPacketRegistry(
      Map<ProtocolVersion, VersionMapping> versionMappings) {
    this.versionMappings = versionMappings;
  }

  @Override
  public PacketRegistry forVersion(ProtocolVersion version) {
    VersionMapping mapping = this.versionMappings.get(version);
    if (mapping == null) {
      throw new IllegalArgumentException(String.format("Invalid version %s", version));
    }
    return mapping;
  }

  /**
   * Holds the packet mappings for a specific protocol version or a range of versions.
   * Instances are shared across versions when the mappings are identical.
   */
  private static class VersionMapping implements PacketRegistry {

    final IntObjectMap<PacketCodec<? extends MinecraftPacket>> packetIdToCodec;
    final Map<Class<? extends MinecraftPacket>, PacketCodec<? extends MinecraftPacket>>
        packetClassToCodec;
    final Object2IntMap<Class<? extends MinecraftPacket>> packetClassToId;
    final Direction direction;
    final ProtocolVersion version;

    VersionMapping(Direction direction, ProtocolVersion version) {
      this.direction = direction;
      this.version = version;
      this.packetIdToCodec = new IntObjectHashMap<>(16, 0.5f);
      this.packetClassToCodec = new HashMap<>(16, 0.5f);
      this.packetClassToId = new Object2IntOpenHashMap<>(16, 0.5f);
      this.packetClassToId.defaultReturnValue(Integer.MIN_VALUE);
    }

    void register(int id, Class<? extends MinecraftPacket> packet,
                  PacketCodec<? extends MinecraftPacket> codec, boolean encodeOnly) {
      if (!encodeOnly) {
        this.packetIdToCodec.put(id, codec);
      }
      this.packetClassToCodec.put(packet, codec);
      this.packetClassToId.put(packet, id);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      VersionMapping that = (VersionMapping) o;
      return Objects.equals(packetIdToCodec, that.packetIdToCodec)
          && Objects.equals(packetClassToCodec, that.packetClassToCodec)
          && Objects.equals(packetClassToId, that.packetClassToId)
          && Objects.equals(direction, that.direction)
          && Objects.equals(version, that.version);
    }

    public boolean equalsNonStrict(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      VersionMapping that = (VersionMapping) o;
      return Objects.equals(packetIdToCodec, that.packetIdToCodec)
          && Objects.equals(packetClassToCodec, that.packetClassToCodec)
          && Objects.equals(packetClassToId, that.packetClassToId);
    }

    @Override
    public int hashCode() {
      return Objects.hash(packetIdToCodec, packetClassToCodec, packetClassToId, direction, version);
    }

    public int hashCodeNonStrict() {
      return Objects.hash(packetIdToCodec, packetClassToCodec, packetClassToId);
    }

    @Override
    public @Nullable PacketCodec<? extends MinecraftPacket> getCodec(int id) {
      return packetIdToCodec.get(id);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends MinecraftPacket> @Nullable PacketCodec<T> getCodec(Class<T> packetClass) {
      return (PacketCodec<T>) packetClassToCodec.get(packetClass);
    }

    @Override
    public int getPacketId(MinecraftPacket packet) {
      final int id = packetClassToId.getInt(packet.getClass());
      if (id == Integer.MIN_VALUE) {
        throw new IllegalArgumentException(String.format(
            "Unable to find id for packet of type %s in %s protocol %s",
            packet.getClass().getName(), direction, version
        ));
      }
      return id;
    }

    @Override
    public boolean canDecodePacket(MinecraftPacket packet) {
      return packetClassToId.containsKey(packet.getClass());
    }
  }

  /**
   * Creates a new builder for constructing a multi-version packet registry.
   *
   * @param direction the packet direction (clientbound or serverbound)
   * @return a new builder instance
   */
  public static Builder builder(Direction direction) {
    return new Builder(direction);
  }

  /**
   * Builder for constructing a MultiVersionPacketRegistry with optimized memory usage.
   */
  public static class Builder {

    private final Direction direction;
    private final Map<ProtocolVersion, VersionMapping> workingMappings =
        new EnumMap<>(ProtocolVersion.class);

    private Builder(Direction direction) {
      this.direction = direction;
    }

    /**
     * Registers a packet for a specific protocol version with a given ID.
     *
     * @param packetClass the packet class
     * @param codec the packet codec
     * @param version the protocol version
     * @param packetId the packet ID
     * @param <T> the packet type
     * @return this builder
     */
    public <T extends MinecraftPacket> Builder register(
        Class<T> packetClass,
        PacketCodec<T> codec,
        ProtocolVersion version,
        int packetId) {
      return register(packetClass, codec, version, null, packetId, false);
    }

    /**
     * Registers a packet for a range of protocol versions with a given ID.
     *
     * @param packetClass the packet class
     * @param codec the packet codec
     * @param startVersion the starting protocol version (inclusive)
     * @param endVersion the ending protocol version (inclusive), or null for all future versions
     * @param packetId the packet ID
     * @param encodeOnly if true, the packet will only be registered for encoding, not decoding
     * @param <T> the packet type
     * @return this builder
     */
    public <T extends MinecraftPacket> Builder register(
        Class<T> packetClass,
        PacketCodec<T> codec,
        ProtocolVersion startVersion,
        @Nullable ProtocolVersion endVersion,
        int packetId,
        boolean encodeOnly) {

      ProtocolVersion end = endVersion != null ? endVersion : ProtocolVersion.MAXIMUM_VERSION;

      // Validate version range
      if (startVersion.greaterThan(end)) {
        throw new IllegalArgumentException(String.format(
            "Start version %s is greater than end version %s", startVersion, end
        ));
      }

      // Register for all versions in the range
      for (ProtocolVersion version : ProtocolVersion.values()) {
        if (version.isLegacy() || version.isUnknown()) {
          continue;
        }
        if (version.noLessThan(startVersion) && version.noGreaterThan(end)) {
          VersionMapping versionMapping = workingMappings.computeIfAbsent(version, v -> new VersionMapping(direction, version));
          if (versionMapping.packetClassToCodec.containsKey(packetClass)) {
            throw new IllegalArgumentException(String.format(
                "Packet %s already registered for version %s", packetClass.getName(), version
            ));
          }
          versionMapping.register(packetId, packetClass, codec, encodeOnly);
        }
      }

      return this;
    }

    /**
     * Registers a packet for multiple version ranges.
     *
     * @param packetClass the packet class
     * @param codec the packet codec
     * @param ranges the version ranges to register
     * @param <T> the packet type
     * @return this builder
     */
    public <T extends MinecraftPacket> Builder register(
        Class<T> packetClass,
        PacketCodec<T> codec,
        VersionRange... ranges) {
      if (ranges.length == 0) {
        throw new IllegalArgumentException("At least one version range must be provided");
      }
      for (VersionRange range : ranges) {
        register(packetClass, codec, range.start, range.end, range.packetId, range.encodeOnly);
      }
      return this;
    }

    /**
     * Builds the multi-version packet registry, optimizing memory by reusing identical mappings
     * across versions.
     *
     * @return the constructed registry
     */
    public MultiVersionPacketRegistry build() {
      // Optimize by reusing identical mappings across versions
      Map<ProtocolVersion, VersionMapping> optimized = new EnumMap<>(ProtocolVersion.class);
      Map<VersionMapping, VersionMapping> deduplicationMap = new Object2ObjectOpenCustomHashMap<>(
          new Strategy<>() {
            @Override
            public int hashCode(VersionMapping o) {
              return o.hashCodeNonStrict();
            }

            @Override
            public boolean equals(VersionMapping a, VersionMapping b) {
              return a.equalsNonStrict(b);
            }
          });

      for (Map.Entry<ProtocolVersion, VersionMapping> entry : workingMappings.entrySet()) {
        VersionMapping mapping = entry.getValue();
        VersionMapping canonical = deduplicationMap.get(mapping);
        if (canonical == null) {
          // First time seeing this mapping, use it as the canonical version
          deduplicationMap.put(mapping, mapping);
          canonical = mapping;
        }
        // Reuse the canonical mapping
        optimized.put(entry.getKey(), canonical);
      }

      return new MultiVersionPacketRegistry(Collections.unmodifiableMap(optimized));
    }
  }

  /**
   * Represents a version range for packet registration.
   */
  public static class VersionRange {

    final ProtocolVersion start;
    final @Nullable ProtocolVersion end;
    final int packetId;
    final boolean encodeOnly;

    private VersionRange(
        ProtocolVersion start,
        @Nullable ProtocolVersion end,
        int packetId,
        boolean encodeOnly) {
      this.start = start;
      this.end = end;
      this.packetId = packetId;
      this.encodeOnly = encodeOnly;
    }

    /**
     * Creates a version range starting from the specified version and continuing to all future
     * versions.
     *
     * @param start the starting version
     * @param packetId the packet ID
     * @return a new version range
     */
    public static VersionRange of(ProtocolVersion start, int packetId) {
      return new VersionRange(start, null, packetId, false);
    }

    /**
     * Creates a version range from start to end (inclusive).
     *
     * @param start the starting version
     * @param end the ending version
     * @param packetId the packet ID
     * @return a new version range
     */
    public static VersionRange of(ProtocolVersion start, ProtocolVersion end, int packetId) {
      return new VersionRange(start, end, packetId, false);
    }

    /**
     * Creates an encode-only version range starting from the specified version.
     *
     * @param start the starting version
     * @param packetId the packet ID
     * @return a new version range
     */
    public static VersionRange encodeOnly(ProtocolVersion start, int packetId) {
      return new VersionRange(start, null, packetId, true);
    }

    /**
     * Creates an encode-only version range from start to end (inclusive).
     *
     * @param start the starting version
     * @param end the ending version
     * @param packetId the packet ID
     * @return a new version range
     */
    public static VersionRange encodeOnly(
        ProtocolVersion start, ProtocolVersion end, int packetId) {
      return new VersionRange(start, end, packetId, true);
    }
  }
}