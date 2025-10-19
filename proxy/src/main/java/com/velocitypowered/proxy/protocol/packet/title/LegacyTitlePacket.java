/*
 * Copyright (C) 2018-2023 Velocity Contributors
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

package com.velocitypowered.proxy.protocol.packet.title;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.PacketCodec;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.packet.chat.ComponentHolder;
import io.netty.buffer.ByteBuf;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class LegacyTitlePacket extends GenericTitlePacket {

  private final @Nullable ComponentHolder component;
  private final int fadeIn;
  private final int stay;
  private final int fadeOut;

  public LegacyTitlePacket(ActionType action, @Nullable ComponentHolder component,
                           int fadeIn, int stay, int fadeOut) {
    super(action);
    this.component = component;
    this.fadeIn = fadeIn;
    this.stay = stay;
    this.fadeOut = fadeOut;
  }

  @Override
  public @Nullable ComponentHolder getComponent() {
    return component;
  }

  @Override
  public int getFadeIn() {
    return fadeIn;
  }

  @Override
  public int getStay() {
    return stay;
  }

  @Override
  public int getFadeOut() {
    return fadeOut;
  }

  @Override
  public String toString() {
    return "LegacyTitlePacket{"
        + "action=" + getAction()
        + ", component='" + component + '\''
        + ", fadeIn=" + fadeIn
        + ", stay=" + stay
        + ", fadeOut=" + fadeOut
        + '}';
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }

  public static class Codec implements PacketCodec<LegacyTitlePacket> {
    public static final Codec INSTANCE = new Codec();

    @Override
    public LegacyTitlePacket decode(ByteBuf buf, ProtocolUtils.Direction direction,
        ProtocolVersion protocolVersion) {
      throw new UnsupportedOperationException(); // encode only
    }

    @Override
    public void encode(LegacyTitlePacket packet, ByteBuf buf, ProtocolUtils.Direction direction,
        ProtocolVersion protocolVersion) {
      if (protocolVersion.lessThan(ProtocolVersion.MINECRAFT_1_11)
          && packet.getAction() == ActionType.SET_ACTION_BAR) {
        throw new IllegalStateException("Action bars are only supported on 1.11 and newer");
      }
      ProtocolUtils.writeVarInt(buf, packet.getAction().getAction(protocolVersion));

      switch (packet.getAction()) {
        case SET_TITLE:
        case SET_SUBTITLE:
        case SET_ACTION_BAR:
          if (packet.component == null) {
            throw new IllegalStateException("No component found for " + packet.getAction());
          }
          packet.component.write(buf);
          break;
        case SET_TIMES:
          buf.writeInt(packet.fadeIn);
          buf.writeInt(packet.stay);
          buf.writeInt(packet.fadeOut);
          break;
        case HIDE:
        case RESET:
          break;
        default:
          throw new UnsupportedOperationException("Unknown action " + packet.getAction());
      }
    }
  }
}
