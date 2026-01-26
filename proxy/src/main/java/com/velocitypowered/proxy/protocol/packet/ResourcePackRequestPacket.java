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

import com.google.common.base.Preconditions;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.player.ResourcePackInfo;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.connection.player.resourcepack.VelocityResourcePackInfo;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.ProtocolUtils.Direction;
import com.velocitypowered.proxy.protocol.packet.chat.ComponentHolder;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import java.util.UUID;
import java.util.regex.Pattern;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Represents a resource pack request packet sent by the server to prompt the client to download a resource pack.
 * The packet includes the resource pack URL, SHA1 hash, and optional prompt.
 */
public class ResourcePackRequestPacket implements MinecraftPacket {

  private @MonotonicNonNull UUID id; // 1.20.3+
  private @MonotonicNonNull String url;
  private @MonotonicNonNull String hash;
  private boolean isRequired; // 1.17+
  private @Nullable ComponentHolder prompt; // 1.17+

  private static final Pattern PLAUSIBLE_SHA1_HASH = Pattern.compile("^[a-z0-9]{40}$"); // 1.20.2+

  public @Nullable UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public @Nullable String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public boolean isRequired() {
    return isRequired;
  }

  public @Nullable String getHash() {
    return hash;
  }

  public void setHash(String hash) {
    this.hash = hash;
  }

  public void setRequired(boolean required) {
    isRequired = required;
  }

  public @Nullable ComponentHolder getPrompt() {
    return prompt;
  }

  public void setPrompt(@Nullable ComponentHolder prompt) {
    this.prompt = prompt;
  }

  @Override
  public void decode(ByteBuf buf, Direction direction, ProtocolVersion protocolVersion) {
    if (protocolVersion.noLessThan(ProtocolVersion.MINECRAFT_1_20_3)) {
      this.id = ProtocolUtils.readUuid(buf);
    }
    this.url = ProtocolUtils.readString(buf);
    this.hash = ProtocolUtils.readString(buf);
    if (protocolVersion.noLessThan(ProtocolVersion.MINECRAFT_1_17)) {
      this.isRequired = buf.readBoolean();
      if (buf.readBoolean()) {
        this.prompt = ComponentHolder.read(buf, protocolVersion);
      } else {
        this.prompt = null;
      }
    }
  }

  @Override
  public void encode(ByteBuf buf, Direction direction, ProtocolVersion protocolVersion) {
    if (protocolVersion.noLessThan(ProtocolVersion.MINECRAFT_1_20_3)) {
      if (id == null) {
        throw new IllegalStateException("Resource pack id not set yet!");
      }
      ProtocolUtils.writeUuid(buf, id);
    }
    if (url == null || hash == null) {
      throw new IllegalStateException("Packet not fully filled in yet!");
    }
    ProtocolUtils.writeString(buf, url);
    ProtocolUtils.writeString(buf, hash);
    if (protocolVersion.noLessThan(ProtocolVersion.MINECRAFT_1_17)) {
      buf.writeBoolean(isRequired);
      if (prompt != null) {
        buf.writeBoolean(true);
        prompt.write(buf);
      } else {
        buf.writeBoolean(false);
      }
    }
  }

  /**
   * Converts this packet into a {@link VelocityResourcePackInfo} object, which contains the information
   * about the resource pack being requested.
   *
   * @return a {@code VelocityResourcePackInfo} representing the resource pack information
   */
  public VelocityResourcePackInfo toServerPromptedPack() {
    final ResourcePackInfo.Builder builder =
        new VelocityResourcePackInfo.BuilderImpl(Preconditions.checkNotNull(url))
            .setId(id).setPrompt(prompt == null ? null : prompt.getComponent())
            .setShouldForce(isRequired).setOrigin(ResourcePackInfo.Origin.DOWNSTREAM_SERVER);

    if (hash != null && !hash.isEmpty()) {
      if (PLAUSIBLE_SHA1_HASH.matcher(hash).matches()) {
        builder.setHash(ByteBufUtil.decodeHexDump(hash));
      }
    }
    return (VelocityResourcePackInfo) builder.build();
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }

  @Override
  public String toString() {
    return "ResourcePackRequestPacket{"
        + "id=" + id
        + ", url='" + url + '\''
        + ", hash='" + hash + '\''
        + ", isRequired=" + isRequired
        + ", prompt=" + prompt
        + '}';
  }
}
