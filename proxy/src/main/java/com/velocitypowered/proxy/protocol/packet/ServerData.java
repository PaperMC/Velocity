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

package com.velocitypowered.proxy.protocol.packet;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.util.Favicon;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

public class ServerData implements MinecraftPacket {

  private @Nullable Component description;
  private @Nullable Favicon favicon;
  private boolean secureChatEnforced; // Added in 1.19.1

  public ServerData() {
  }

  public ServerData(@Nullable Component description, @Nullable Favicon favicon,
      boolean secureChatEnforced) {
    this.description = description;
    this.favicon = favicon;
    this.secureChatEnforced = secureChatEnforced;
  }

  @Override
  public void decode(ByteBuf buf, ProtocolUtils.Direction direction,
      ProtocolVersion protocolVersion) {
    if (buf.readBoolean()) {
      this.description = ProtocolUtils.getJsonChatSerializer(protocolVersion)
          .deserialize(ProtocolUtils.readString(buf));
    }
    if (buf.readBoolean()) {
      this.favicon = new Favicon(ProtocolUtils.readString(buf));
    }
    if (protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_19_3) < 0) {
      buf.readBoolean();
    }
    if (protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_19_1) >= 0) {
      this.secureChatEnforced = buf.readBoolean();
    }
  }

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
    boolean hasDescription = this.description != null;
    buf.writeBoolean(hasDescription);
    if (hasDescription) {
      ProtocolUtils.writeString(
          buf,
          ProtocolUtils.getJsonChatSerializer(protocolVersion).serialize(this.description)
      );
    }

    boolean hasFavicon = this.favicon != null;
    buf.writeBoolean(hasFavicon);
    if (hasFavicon) {
      ProtocolUtils.writeString(buf, favicon.getBase64Url());
    }

    if (protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_19_3) < 0) {
      buf.writeBoolean(false);
    }
    if (protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_19_1) >= 0) {
      buf.writeBoolean(this.secureChatEnforced);
    }
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }

  public @Nullable Component getDescription() {
    return description;
  }

  public @Nullable Favicon getFavicon() {
    return favicon;
  }

  public boolean isSecureChatEnforced() {
    return secureChatEnforced;
  }
}
