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

package com.velocitypowered.proxy.protocol.packet.chat;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import java.util.Arrays;
import java.util.BitSet;

public class LastSeenMessages {

  public static final int WINDOW_SIZE = 20;
  private static final int DIV_FLOOR = -Math.floorDiv(-WINDOW_SIZE, 8);
  private final int offset;
  private final BitSet acknowledged;
  private byte checksum;

  public LastSeenMessages() {
    this(0, new BitSet(), (byte) 0);
  }

  public LastSeenMessages(int offset, BitSet acknowledged, byte checksum) {
    this.offset = offset;
    this.acknowledged = acknowledged;
    this.checksum = checksum;
  }

  public LastSeenMessages(ByteBuf buf, ProtocolVersion protocolVersion) {
    this.offset = ProtocolUtils.readVarInt(buf);

    byte[] bytes = new byte[DIV_FLOOR];
    buf.readBytes(bytes);
    this.acknowledged = BitSet.valueOf(bytes);

    if (protocolVersion.noLessThan(ProtocolVersion.MINECRAFT_1_21_5)) {
      this.checksum = buf.readByte();
    }
  }

  public void encode(ByteBuf buf, ProtocolVersion protocolVersion) {
    ProtocolUtils.writeVarInt(buf, offset);
    buf.writeBytes(Arrays.copyOf(acknowledged.toByteArray(), DIV_FLOOR));
    if (protocolVersion.noLessThan(ProtocolVersion.MINECRAFT_1_21_5)) {
      buf.writeByte(this.checksum);
    }
  }

  public int getOffset() {
    return this.offset;
  }

  public BitSet getAcknowledged() {
    return acknowledged;
  }

  public LastSeenMessages offset(final int offset) {
    return new LastSeenMessages(this.offset + offset, acknowledged, checksum);
  }

  @Override
  public String toString() {
    return "LastSeenMessages{" +
        "offset=" + offset +
        ", acknowledged=" + acknowledged +
        ", checksum=" + checksum +
        '}';
  }
}
