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

/**
 * Represents a collection of the last seen messages by a player or client.
 * This class tracks the recent chat messages that the player has viewed.
 */
public class LastSeenMessages {

  public static final int WINDOW_SIZE = 20;
  private static final int DIV_FLOOR = -Math.floorDiv(-WINDOW_SIZE, 8);
  private final int offset;
  private final BitSet acknowledged;
  private byte checksum;

  public LastSeenMessages() {
    this(0, new BitSet(), (byte) 0);
  }

  /**
   * Creates a new {@link LastSeenMessages} instance with the specified offset, acknowledged messages, and checksum.
   *
   * @param offset the starting index of the message window
   * @param acknowledged a BitSet representing which messages have been acknowledged
   * @param checksum the checksum for the message window data
   */
  public LastSeenMessages(int offset, BitSet acknowledged, byte checksum) {
    this.offset = offset;
    this.acknowledged = acknowledged;
    this.checksum = checksum;
  }

  /**
   * Constructs a new {@link LastSeenMessages} instance by decoding data from the provided
   * {@link ByteBuf}.
   *
   * @param buf the buffer containing the serialized last seen messages data
   * @param protocolVersion the protocol version (determines if checksum is written)
   */
  public LastSeenMessages(ByteBuf buf, ProtocolVersion protocolVersion) {
    this.offset = ProtocolUtils.readVarInt(buf);

    byte[] bytes = new byte[DIV_FLOOR];
    buf.readBytes(bytes);
    this.acknowledged = BitSet.valueOf(bytes);

    if (protocolVersion.noLessThan(ProtocolVersion.MINECRAFT_1_21_5)) {
      this.checksum = buf.readByte();
    }
  }

  /**
   * Encodes this {@link LastSeenMessages} instance into the provided {@link ByteBuf}.
   *
   * @param buf the buffer to write the data to
   * @param protocolVersion the protocol version used for encoding
   */
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
    return "LastSeenMessages{"
        + "offset=" + offset
        + ", acknowledged=" + acknowledged
        + ", checksum=" + checksum
        + '}';
  }
}
