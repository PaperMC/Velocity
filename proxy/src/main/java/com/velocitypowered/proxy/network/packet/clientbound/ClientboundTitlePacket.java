/*
 * Copyright (C) 2018 Velocity Contributors
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

package com.velocitypowered.proxy.network.packet.clientbound;

import com.google.common.base.MoreObjects;
import com.google.common.primitives.Ints;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.network.ProtocolUtils;
import com.velocitypowered.proxy.network.packet.Packet;
import com.velocitypowered.proxy.network.packet.PacketHandler;
import com.velocitypowered.proxy.network.packet.PacketReader;
import com.velocitypowered.proxy.network.packet.PacketWriter;
import com.velocitypowered.proxy.util.DurationUtils;
import io.netty.buffer.ByteBuf;
import java.util.Arrays;
import net.kyori.adventure.title.Title;
import org.checkerframework.checker.nullness.qual.Nullable;

public class ClientboundTitlePacket implements Packet {
  public static final PacketReader<ClientboundTitlePacket> DECODER = PacketReader.unsupported();
  public static final PacketWriter<ClientboundTitlePacket> ENCODER = PacketWriter.deprecatedEncode();

  public static ClientboundTitlePacket hide(final ProtocolVersion version) {
    return version.gte(ProtocolVersion.MINECRAFT_1_11)
      ? Instances.HIDE
      : Instances.HIDE_OLD;
  }

  public static ClientboundTitlePacket reset(final ProtocolVersion version) {
    return version.gte(ProtocolVersion.MINECRAFT_1_11)
        ? Instances.RESET
        : Instances.RESET_OLD;
  }

  public static ClientboundTitlePacket times(final ProtocolVersion version, final Title.Times times) {
    final int action = version.gte(ProtocolVersion.MINECRAFT_1_11)
        ? SET_TIMES
        : SET_TIMES_OLD;
    return new ClientboundTitlePacket(
        action,
        (int) DurationUtils.toTicks(times.fadeIn()),
        (int) DurationUtils.toTicks(times.stay()),
        (int) DurationUtils.toTicks(times.fadeOut())
    );
  }

  public static final int SET_TITLE = 0;
  public static final int SET_SUBTITLE = 1;
  public static final int SET_ACTION_BAR = 2;
  public static final int SET_TIMES = 3;
  public static final int HIDE = 4;
  public static final int RESET = 5;

  public static final int SET_TIMES_OLD = 2;
  public static final int HIDE_OLD = 3;
  public static final int RESET_OLD = 4;

  private final int action;
  private final @Nullable String component;
  private final int fadeIn;
  private final int stay;
  private final int fadeOut;

  private ClientboundTitlePacket(final int action) {
    checkAction(action, HIDE, RESET, HIDE_OLD, RESET_OLD);
    this.action = action;
    this.component = null;
    this.fadeIn = -1;
    this.stay = -1;
    this.fadeOut = -1;
  }

  public ClientboundTitlePacket(final int action, final String component) {
    checkAction(action, SET_TITLE, SET_SUBTITLE, SET_ACTION_BAR);
    this.action = action;
    this.component = component;
    this.fadeIn = -1;
    this.stay = -1;
    this.fadeOut = -1;
  }

  public ClientboundTitlePacket(final int action, final int fadeIn, final int stay, final int fadeOut) {
    checkAction(action, SET_TIMES, SET_TIMES_OLD);
    this.action = action;
    this.component = null;
    this.fadeIn = fadeIn;
    this.stay = stay;
    this.fadeOut = fadeOut;
  }

  private static void checkAction(final int action, final int... validActions) {
    if (!Ints.contains(validActions, action)) {
      throw new IllegalArgumentException("Invalid action " + action + ", expected one of: " + Arrays.toString(validActions));
    }
  }

  @Override
  public void encode(ByteBuf buf, ProtocolVersion version) {
    ProtocolUtils.writeVarInt(buf, action);
    if (version.gte(ProtocolVersion.MINECRAFT_1_11)) {
      // 1.11+ shifted the action enum by 1 to handle the action bar
      switch (action) {
        case SET_TITLE:
        case SET_SUBTITLE:
        case SET_ACTION_BAR:
          if (component == null) {
            throw new IllegalStateException("No component found for " + action);
          }
          ProtocolUtils.writeString(buf, component);
          break;
        case SET_TIMES:
          buf.writeInt(fadeIn);
          buf.writeInt(stay);
          buf.writeInt(fadeOut);
          break;
        case HIDE:
        case RESET:
          break;
        default:
          throw new UnsupportedOperationException("Unknown action " + action);
      }
    } else {
      switch (action) {
        case SET_TITLE:
        case SET_SUBTITLE:
          if (component == null) {
            throw new IllegalStateException("No component found for " + action);
          }
          ProtocolUtils.writeString(buf, component);
          break;
        case SET_TIMES_OLD:
          buf.writeInt(fadeIn);
          buf.writeInt(stay);
          buf.writeInt(fadeOut);
          break;
        case HIDE_OLD:
        case RESET_OLD:
          break;
        default:
          throw new UnsupportedOperationException("Unknown action " + action);
      }
    }
  }

  @Override
  public boolean handle(PacketHandler handler) {
    return handler.handle(this);
  }

  public int getAction() {
    return action;
  }

  public @Nullable String getComponent() {
    return component;
  }

  public int getFadeIn() {
    return fadeIn;
  }

  public int getStay() {
    return stay;
  }

  public int getFadeOut() {
    return fadeOut;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
      .add("action", this.action)
      .add("component", this.component)
      .add("fadeIn", this.fadeIn)
      .add("stay", this.stay)
      .add("fadeOut", this.fadeOut)
      .toString();
  }

  public static final class Instances {
    public static final ClientboundTitlePacket HIDE
        = new ClientboundTitlePacket(ClientboundTitlePacket.HIDE);
    public static final ClientboundTitlePacket RESET
        = new ClientboundTitlePacket(ClientboundTitlePacket.RESET);

    public static final ClientboundTitlePacket HIDE_OLD = new ClientboundTitlePacket(ClientboundTitlePacket.HIDE_OLD);
    public static final ClientboundTitlePacket RESET_OLD = new ClientboundTitlePacket(ClientboundTitlePacket.RESET_OLD);

    private Instances() {
    }
  }
}
