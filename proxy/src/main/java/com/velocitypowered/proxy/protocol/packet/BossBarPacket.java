/*
 * Copyright (C) 2018-2021 Velocity Contributors
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

package com.velocitypowered.proxy.protocol.packet;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.packet.chat.ComponentHolder;
import com.velocitypowered.proxy.util.collect.Enum2IntMap;
import io.netty.buffer.ByteBuf;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.bossbar.BossBar;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Represents a packet used to manage boss bars.
 * This packet can add, remove, or update a boss bar.
 */
public class BossBarPacket implements MinecraftPacket {

  private static final Enum2IntMap<BossBar.Color> COLORS_TO_PROTOCOL =
      new Enum2IntMap.Builder<>(BossBar.Color.class)
          .put(BossBar.Color.PINK, 0)
          .put(BossBar.Color.BLUE, 1)
          .put(BossBar.Color.RED, 2)
          .put(BossBar.Color.GREEN, 3)
          .put(BossBar.Color.YELLOW, 4)
          .put(BossBar.Color.PURPLE, 5)
          .put(BossBar.Color.WHITE, 6)
          .build();
  private static final Enum2IntMap<BossBar.Overlay> OVERLAY_TO_PROTOCOL =
      new Enum2IntMap.Builder<>(BossBar.Overlay.class)
          .put(BossBar.Overlay.PROGRESS, 0)
          .put(BossBar.Overlay.NOTCHED_6, 1)
          .put(BossBar.Overlay.NOTCHED_10, 2)
          .put(BossBar.Overlay.NOTCHED_12, 3)
          .put(BossBar.Overlay.NOTCHED_20, 4)
          .build();
  private static final Enum2IntMap<BossBar.Flag> FLAG_BITS_TO_PROTOCOL =
      new Enum2IntMap.Builder<>(BossBar.Flag.class)
          .put(BossBar.Flag.DARKEN_SCREEN, 0x1)
          .put(BossBar.Flag.PLAY_BOSS_MUSIC, 0x2)
          .put(BossBar.Flag.CREATE_WORLD_FOG, 0x4)
          .build();

  public static final int ADD = 0;
  public static final int REMOVE = 1;
  public static final int UPDATE_PERCENT = 2;
  public static final int UPDATE_NAME = 3;
  public static final int UPDATE_STYLE = 4;
  public static final int UPDATE_PROPERTIES = 5;
  private @Nullable UUID uuid;
  private int action;
  private @Nullable ComponentHolder name;
  private float percent;
  private int color;
  private int overlay;
  private short flags;

  /**
   * Creates a packet to add a new boss bar.
   *
   * @param id the UUID of the boss bar
   * @param bar the {@link BossBar} instance
   * @param name the {@link ComponentHolder} containing the boss bar's name
   * @return a {@link BossBarPacket} to add a boss bar
   */
  public static BossBarPacket createAddPacket(
      final UUID id,
      final BossBar bar,
      final ComponentHolder name
  ) {
    final BossBarPacket packet = new BossBarPacket();
    packet.setUuid(id);
    packet.setAction(BossBarPacket.ADD);
    packet.setName(name);
    packet.setColor(COLORS_TO_PROTOCOL.get(bar.color()));
    packet.setOverlay(OVERLAY_TO_PROTOCOL.get(bar.overlay()));
    packet.setPercent(bar.progress());
    packet.setFlags(serializeFlags(bar.flags()));
    return packet;
  }

  /**
   * Creates a packet to remove an existing boss bar.
   *
   * @param id the UUID of the boss bar to remove
   * @param bar the {@link BossBar} instance
   * @return a {@link BossBarPacket} to remove a boss bar
   */
  public static BossBarPacket createRemovePacket(final UUID id, final BossBar bar) {
    final BossBarPacket packet = new BossBarPacket();
    packet.setUuid(id);
    packet.setAction(REMOVE);
    return packet;
  }

  /**
   * Creates a packet to update the progress (percentage) of the boss bar.
   *
   * @param id the UUID of the boss bar
   * @param bar the {@link BossBar} instance
   * @return a {@link BossBarPacket} to update the boss bar's progress
   */
  public static BossBarPacket createUpdateProgressPacket(final UUID id, final BossBar bar) {
    final BossBarPacket packet = new BossBarPacket();
    packet.setUuid(id);
    packet.setAction(UPDATE_PERCENT);
    packet.setPercent(bar.progress());
    return packet;
  }

  /**
   * Creates a packet to update the name of the boss bar.
   *
   * @param id the UUID of the boss bar
   * @param bar the {@link BossBar} instance
   * @param name the {@link ComponentHolder} containing the boss bar's new name
   * @return a {@link BossBarPacket} to update the boss bar's name
   */
  public static BossBarPacket createUpdateNamePacket(
      final UUID id,
      final BossBar bar,
      final ComponentHolder name
  ) {
    final BossBarPacket packet = new BossBarPacket();
    packet.setUuid(id);
    packet.setAction(UPDATE_NAME);
    packet.setName(name);
    return packet;
  }

  /**
   * Creates a packet to update the style (color and overlay) of the boss bar.
   *
   * @param id the UUID of the boss bar
   * @param bar the {@link BossBar} instance
   * @return a {@link BossBarPacket} to update the boss bar's style
   */
  public static BossBarPacket createUpdateStylePacket(final UUID id, final BossBar bar) {
    final BossBarPacket packet = new BossBarPacket();
    packet.setUuid(id);
    packet.setAction(UPDATE_STYLE);
    packet.setColor(COLORS_TO_PROTOCOL.get(bar.color()));
    packet.setOverlay(OVERLAY_TO_PROTOCOL.get(bar.overlay()));
    return packet;
  }

  /**
   * Creates a packet to update the properties of the boss bar.
   *
   * @param id the UUID of the boss bar
   * @param bar the {@link BossBar} instance
   * @return a {@link BossBarPacket} to update the boss bar's properties
   */
  public static BossBarPacket createUpdatePropertiesPacket(final UUID id, final BossBar bar) {
    final BossBarPacket packet = new BossBarPacket();
    packet.setUuid(id);
    packet.setAction(UPDATE_PROPERTIES);
    packet.setFlags(serializeFlags(bar.flags()));
    return packet;
  }

  /**
   * Retrieves the UUID of the boss bar.
   *
   * @return the UUID of the boss bar
   * @throws IllegalStateException if the UUID has not been set
   */
  public UUID getUuid() {
    if (uuid == null) {
      throw new IllegalStateException("No boss bar UUID specified");
    }
    return uuid;
  }

  public void setUuid(UUID uuid) {
    this.uuid = uuid;
  }

  public int getAction() {
    return action;
  }

  public void setAction(int action) {
    this.action = action;
  }

  public @Nullable ComponentHolder getName() {
    return name;
  }

  public void setName(ComponentHolder name) {
    this.name = name;
  }

  public float getPercent() {
    return percent;
  }

  public void setPercent(float percent) {
    this.percent = percent;
  }

  public int getColor() {
    return color;
  }

  public void setColor(int color) {
    this.color = color;
  }

  public int getOverlay() {
    return overlay;
  }

  public void setOverlay(int overlay) {
    this.overlay = overlay;
  }

  public short getFlags() {
    return flags;
  }

  public void setFlags(short flags) {
    this.flags = flags;
  }

  @Override
  public String toString() {
    return "BossBar{"
        + "uuid=" + uuid
        + ", action=" + action
        + ", name='" + name + '\''
        + ", percent=" + percent
        + ", color=" + color
        + ", overlay=" + overlay
        + ", flags=" + flags
        + '}';
  }

  @Override
  public void decode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
    this.uuid = ProtocolUtils.readUuid(buf);
    this.action = ProtocolUtils.readVarInt(buf);
    switch (action) {
      case ADD -> {
        this.name = ComponentHolder.read(buf, version);
        this.percent = buf.readFloat();
        this.color = ProtocolUtils.readVarInt(buf);
        this.overlay = ProtocolUtils.readVarInt(buf);
        this.flags = buf.readUnsignedByte();
      }
      case REMOVE -> {
      }
      case UPDATE_PERCENT -> this.percent = buf.readFloat();
      case UPDATE_NAME -> this.name = ComponentHolder.read(buf, version);
      case UPDATE_STYLE -> {
        this.color = ProtocolUtils.readVarInt(buf);
        this.overlay = ProtocolUtils.readVarInt(buf);
      }
      case UPDATE_PROPERTIES -> this.flags = buf.readUnsignedByte();
      default -> throw new UnsupportedOperationException("Unknown action " + action);
    }
  }

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
    if (uuid == null) {
      throw new IllegalStateException("No boss bar UUID specified");
    }
    ProtocolUtils.writeUuid(buf, uuid);
    ProtocolUtils.writeVarInt(buf, action);
    switch (action) {
      case ADD -> {
        if (name == null) {
          throw new IllegalStateException("No name specified!");
        }
        name.write(buf);
        buf.writeFloat(percent);
        ProtocolUtils.writeVarInt(buf, color);
        ProtocolUtils.writeVarInt(buf, overlay);
        buf.writeByte(flags);
      }
      case REMOVE -> {
      }
      case UPDATE_PERCENT -> buf.writeFloat(percent);
      case UPDATE_NAME -> {
        if (name == null) {
          throw new IllegalStateException("No name specified!");
        }
        name.write(buf);
      }
      case UPDATE_STYLE -> {
        ProtocolUtils.writeVarInt(buf, color);
        ProtocolUtils.writeVarInt(buf, overlay);
      }
      case UPDATE_PROPERTIES -> buf.writeByte(flags);
      default -> throw new UnsupportedOperationException("Unknown action " + action);
    }
  }

  private static byte serializeFlags(Set<BossBar.Flag> flags) {
    byte val = 0x0;
    for (BossBar.Flag flag : flags) {
      val |= (byte) FLAG_BITS_TO_PROTOCOL.get(flag);
    }
    return val;
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }
}
