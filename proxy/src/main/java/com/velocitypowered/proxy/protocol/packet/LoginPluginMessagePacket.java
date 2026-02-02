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
import com.velocitypowered.proxy.protocol.ProtocolUtils.Direction;
import com.velocitypowered.proxy.protocol.util.DeferredByteBufHolder;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Represents a login plugin message packet sent during the login phase. This packet allows custom
 * plugin messages to be sent from the server to the client before login is complete.
 */
public class LoginPluginMessagePacket extends DeferredByteBufHolder implements MinecraftPacket {

  private int id;
  private @Nullable String channel;

  public LoginPluginMessagePacket() {
    super(null);
  }

  /**
   * Constructs a new {@code LoginPluginMessagePacket} with the specified ID, channel, and data buffer.
   *
   * @param id the plugin message ID
   * @param channel the channel name, or {@code null} if not specified
   * @param data the data buffer
   */
  public LoginPluginMessagePacket(int id, @Nullable String channel, ByteBuf data) {
    super(data);
    this.id = id;
    this.channel = channel;
  }

  public int getId() {
    return id;
  }

  /**
   * Gets the plugin message channel.
   *
   * @return the channel name
   * @throws IllegalStateException if the channel is not specified
   */
  public String getChannel() {
    if (channel == null) {
      throw new IllegalStateException("Channel is not specified!");
    }
    return channel;
  }

  @Override
  public String toString() {
    return "LoginPluginMessage{"
        + "id=" + id
        + ", channel='" + channel + '\''
        + ", data=" + super.toString()
        + '}';
  }

  @Override
  public void decode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
    this.id = ProtocolUtils.readVarInt(buf);
    this.channel = ProtocolUtils.readString(buf);
    if (buf.isReadable()) {
      this.replace(buf.readRetainedSlice(buf.readableBytes()));
    } else {
      this.replace(Unpooled.EMPTY_BUFFER);
    }
  }

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
    ProtocolUtils.writeVarInt(buf, id);
    if (channel == null) {
      throw new IllegalStateException("Channel is not specified!");
    }
    ProtocolUtils.writeString(buf, channel);
    buf.writeBytes(content());
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }

  @Override
  public int encodeSizeHint(Direction direction, ProtocolVersion version) {
    return content().readableBytes();
  }
}
