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

import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import java.util.Arrays;
import java.util.BitSet;

public class LastSeenMessages {

  private static final int DIV_FLOOR = -Math.floorDiv(-20, 8);
  private int offset;
  private BitSet acknowledged;

  public LastSeenMessages() {
    this.offset = 0;
    this.acknowledged = new BitSet();
  }

  public LastSeenMessages(ByteBuf buf) {
    this.offset = ProtocolUtils.readVarInt(buf);

    byte[] bytes = new byte[DIV_FLOOR];
    buf.readBytes(bytes);
    this.acknowledged = BitSet.valueOf(bytes);
  }

  public void encode(ByteBuf buf) {
    ProtocolUtils.writeVarInt(buf, offset);
    buf.writeBytes(Arrays.copyOf(acknowledged.toByteArray(), DIV_FLOOR));
  }

  public boolean isEmpty() {
    return acknowledged.isEmpty();
  }

  public int getOffset() {
    return this.offset;
  }

  @Override
  public String toString() {
    return "LastSeenMessages{" +
            "offset=" + offset +
            ", acknowledged=" + acknowledged +
            '}';
  }
}
