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

import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_13;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_8;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_9;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.PacketCodec;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class TabCompleteRequestPacket implements MinecraftPacket {

  private static final int VANILLA_MAX_TAB_COMPLETE_LEN = 2048;

  private final @Nullable String command;
  private final int transactionId;
  private final boolean assumeCommand;
  private final boolean hasPosition;
  private final long position;

  public TabCompleteRequestPacket() {
    this(null, 0, false, false, 0);
  }

  public TabCompleteRequestPacket(@Nullable String command, int transactionId,
                                   boolean assumeCommand, boolean hasPosition, long position) {
    this.command = command;
    this.transactionId = transactionId;
    this.assumeCommand = assumeCommand;
    this.hasPosition = hasPosition;
    this.position = position;
  }

  public String command() {
    if (command == null) {
      throw new IllegalStateException("Command is not specified");
    }
    return command;
  }

  public String getCommand() {
    return command();
  }

  public boolean assumeCommand() {
    return assumeCommand;
  }

  public boolean isAssumeCommand() {
    return assumeCommand();
  }

  public boolean hasPosition() {
    return hasPosition;
  }

  public long position() {
    return position;
  }

  public int transactionId() {
    return transactionId;
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }

  public static class Codec implements PacketCodec<TabCompleteRequestPacket> {
    public static final Codec INSTANCE = new Codec();

    @Override
    public TabCompleteRequestPacket decode(ByteBuf buf, ProtocolUtils.Direction direction,
                                            ProtocolVersion version) {
      String command;
      int transactionId = 0;
      boolean assumeCommand = false;
      boolean hasPosition = false;
      long position = 0;

      if (version.noLessThan(MINECRAFT_1_13)) {
        transactionId = ProtocolUtils.readVarInt(buf);
        command = ProtocolUtils.readString(buf, VANILLA_MAX_TAB_COMPLETE_LEN);
      } else {
        command = ProtocolUtils.readString(buf, VANILLA_MAX_TAB_COMPLETE_LEN);
        if (version.noLessThan(MINECRAFT_1_9)) {
          assumeCommand = buf.readBoolean();
        }
        if (version.noLessThan(MINECRAFT_1_8)) {
          hasPosition = buf.readBoolean();
          if (hasPosition) {
            position = buf.readLong();
          }
        }
      }

      return new TabCompleteRequestPacket(command, transactionId, assumeCommand, hasPosition,
          position);
    }

    @Override
    public void encode(TabCompleteRequestPacket packet, ByteBuf buf,
                       ProtocolUtils.Direction direction, ProtocolVersion version) {
      if (packet.command == null) {
        throw new IllegalStateException("Command is not specified");
      }

      if (version.noLessThan(MINECRAFT_1_13)) {
        ProtocolUtils.writeVarInt(buf, packet.transactionId);
        ProtocolUtils.writeString(buf, packet.command);
      } else {
        ProtocolUtils.writeString(buf, packet.command);
        if (version.noLessThan(MINECRAFT_1_9)) {
          buf.writeBoolean(packet.assumeCommand);
        }
        if (version.noLessThan(MINECRAFT_1_8)) {
          buf.writeBoolean(packet.hasPosition);
          if (packet.hasPosition) {
            buf.writeLong(packet.position);
          }
        }
      }
    }
  }
}
