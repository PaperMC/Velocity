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
import com.velocitypowered.proxy.protocol.PacketCodec;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.packet.chat.ComponentHolder;
import com.velocitypowered.proxy.util.collect.Enum2IntMap;
import io.netty.buffer.ByteBuf;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.bossbar.BossBar;
import org.checkerframework.checker.nullness.qual.Nullable;

public record BossBarPacket(UUID uuid, int action, @Nullable ComponentHolder name,
                            float percent, int color, int overlay, short flags) implements MinecraftPacket {

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

  public static BossBarPacket createAddPacket(
      final UUID id,
      final BossBar bar,
      final ComponentHolder name
  ) {
    return new BossBarPacket(id, ADD, name, bar.progress(),
        COLORS_TO_PROTOCOL.get(bar.color()), OVERLAY_TO_PROTOCOL.get(bar.overlay()),
        serializeFlags(bar.flags()));
  }

  public static BossBarPacket createRemovePacket(final UUID id) {
    return new BossBarPacket(id, REMOVE, null, 0, 0, 0, (short) 0);
  }

  public static BossBarPacket createUpdateProgressPacket(final UUID id, final BossBar bar) {
    return new BossBarPacket(id, UPDATE_PERCENT, null, bar.progress(), 0, 0, (short) 0);
  }

  public static BossBarPacket createUpdateNamePacket(
      final UUID id,
      final BossBar bar,
      final ComponentHolder name
  ) {
    return new BossBarPacket(id, UPDATE_NAME, name, 0, 0, 0, (short) 0);
  }

  public static BossBarPacket createUpdateStylePacket(final UUID id, final BossBar bar) {
    return new BossBarPacket(id, UPDATE_STYLE, null, 0,
        COLORS_TO_PROTOCOL.get(bar.color()), OVERLAY_TO_PROTOCOL.get(bar.overlay()), (short) 0);
  }

  public static BossBarPacket createUpdatePropertiesPacket(final UUID id, final BossBar bar) {
    return new BossBarPacket(id, UPDATE_PROPERTIES, null, 0, 0, 0, serializeFlags(bar.flags()));
  }

  private static short serializeFlags(Set<BossBar.Flag> flags) {
    short val = 0x0;
    for (BossBar.Flag flag : flags) {
      val |= FLAG_BITS_TO_PROTOCOL.get(flag);
    }
    return val;
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }

  public static class Codec implements PacketCodec<BossBarPacket> {
    public static final Codec INSTANCE = new Codec();

    @Override
    public BossBarPacket decode(ByteBuf buf, ProtocolUtils.Direction direction,
                                 ProtocolVersion version) {
      UUID uuid = ProtocolUtils.readUuid(buf);
      int action = ProtocolUtils.readVarInt(buf);
      ComponentHolder name = null;
      float percent = 0;
      int color = 0;
      int overlay = 0;
      short flags = 0;

      switch (action) {
        case ADD:
          name = ComponentHolder.read(buf, version);
          percent = buf.readFloat();
          color = ProtocolUtils.readVarInt(buf);
          overlay = ProtocolUtils.readVarInt(buf);
          flags = buf.readUnsignedByte();
          break;
        case REMOVE:
          break;
        case UPDATE_PERCENT:
          percent = buf.readFloat();
          break;
        case UPDATE_NAME:
          name = ComponentHolder.read(buf, version);
          break;
        case UPDATE_STYLE:
          color = ProtocolUtils.readVarInt(buf);
          overlay = ProtocolUtils.readVarInt(buf);
          break;
        case UPDATE_PROPERTIES:
          flags = buf.readUnsignedByte();
          break;
        default:
          throw new UnsupportedOperationException("Unknown action " + action);
      }

      return new BossBarPacket(uuid, action, name, percent, color, overlay, flags);
    }

    @Override
    public void encode(BossBarPacket packet, ByteBuf buf, ProtocolUtils.Direction direction,
                       ProtocolVersion version) {
      ProtocolUtils.writeUuid(buf, packet.uuid);
      ProtocolUtils.writeVarInt(buf, packet.action);
      switch (packet.action) {
        case ADD:
          if (packet.name == null) {
            throw new IllegalStateException("No name specified!");
          }
          packet.name.write(buf);
          buf.writeFloat(packet.percent);
          ProtocolUtils.writeVarInt(buf, packet.color);
          ProtocolUtils.writeVarInt(buf, packet.overlay);
          buf.writeByte(packet.flags);
          break;
        case REMOVE:
          break;
        case UPDATE_PERCENT:
          buf.writeFloat(packet.percent);
          break;
        case UPDATE_NAME:
          if (packet.name == null) {
            throw new IllegalStateException("No name specified!");
          }
          packet.name.write(buf);
          break;
        case UPDATE_STYLE:
          ProtocolUtils.writeVarInt(buf, packet.color);
          ProtocolUtils.writeVarInt(buf, packet.overlay);
          break;
        case UPDATE_PROPERTIES:
          buf.writeByte(packet.flags);
          break;
        default:
          throw new UnsupportedOperationException("Unknown action " + packet.action);
      }
    }
  }
}