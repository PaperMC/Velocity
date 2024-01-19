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

package com.velocitypowered.proxy.protocol.packet;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.util.Favicon;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.packet.chat.ComponentHolder;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class ServerDataPacket implements MinecraftPacket {

  private @Nullable ComponentHolder description;
  private @Nullable Favicon favicon;
  private boolean secureChatEnforced; // Added in 1.19.1

  public ServerDataPacket() {
  }

  public ServerDataPacket(@Nullable ComponentHolder description, @Nullable Favicon favicon,
                          boolean secureChatEnforced) {
    this.description = description;
    this.favicon = favicon;
    this.secureChatEnforced = secureChatEnforced;
  }

  @Override
  public void decode(ByteBuf buf, ProtocolUtils.Direction direction,
      ProtocolVersion protocolVersion) {
    if (protocolVersion.noLessThan(ProtocolVersion.MINECRAFT_1_19_4) || buf.readBoolean()) {
      this.description = ComponentHolder.read(buf, protocolVersion);
    }
    if (buf.readBoolean()) {
      String iconBase64;
      if (protocolVersion.noLessThan(ProtocolVersion.MINECRAFT_1_19_4)) {
        byte[] iconBytes = ProtocolUtils.readByteArray(buf);
        iconBase64 = "data:image/png;base64," + new String(Base64.getEncoder().encode(iconBytes), StandardCharsets.UTF_8);
      } else {
        iconBase64 = ProtocolUtils.readString(buf);
      }
      this.favicon = new Favicon(iconBase64);
    }
    if (protocolVersion.lessThan(ProtocolVersion.MINECRAFT_1_19_3)) {
      buf.readBoolean();
    }
    if (protocolVersion.noLessThan(ProtocolVersion.MINECRAFT_1_19_1)) {
      this.secureChatEnforced = buf.readBoolean();
    }
  }

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction,
      ProtocolVersion protocolVersion) {
    boolean hasDescription = this.description != null;
    if (protocolVersion.lessThan(ProtocolVersion.MINECRAFT_1_19_4)) {
      buf.writeBoolean(hasDescription);
    }
    if (protocolVersion.noLessThan(ProtocolVersion.MINECRAFT_1_19_4) || hasDescription) {
      this.description.write(buf);
    }

    boolean hasFavicon = this.favicon != null;
    buf.writeBoolean(hasFavicon);
    if (hasFavicon) {
      if (protocolVersion.noLessThan(ProtocolVersion.MINECRAFT_1_19_4)) {
        String cutIconBase64 = favicon.getBase64Url().substring("data:image/png;base64,".length());
        byte[] iconBytes = Base64.getDecoder().decode(cutIconBase64.getBytes(StandardCharsets.UTF_8));
        ProtocolUtils.writeByteArray(buf, iconBytes);
      } else {
        ProtocolUtils.writeString(buf, favicon.getBase64Url());
      }
    }

    if (protocolVersion.lessThan(ProtocolVersion.MINECRAFT_1_19_3)) {
      buf.writeBoolean(false);
    }
    if (protocolVersion.noLessThan(ProtocolVersion.MINECRAFT_1_19_1)) {
      buf.writeBoolean(this.secureChatEnforced);
    }
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }

  public @Nullable ComponentHolder getDescription() {
    return description;
  }

  public @Nullable Favicon getFavicon() {
    return favicon;
  }

  public boolean isSecureChatEnforced() {
    return secureChatEnforced;
  }
}