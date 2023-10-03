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
import com.velocitypowered.proxy.connection.player.VelocityResourcePackInfo;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.ProtocolUtils.Direction;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.regex.Pattern;

public class ResourcePackRequest implements MinecraftPacket {

  private @MonotonicNonNull String url;
  private @MonotonicNonNull String hash;
  private boolean isRequired; // 1.17+
  private @Nullable Component prompt; // 1.17+

  private static final Pattern PLAUSIBLE_SHA1_HASH = Pattern.compile("^[a-z0-9]{40}$"); // 1.20.2+

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

  public @Nullable Component getPrompt() {
    return prompt;
  }

  public void setPrompt(Component prompt) {
    this.prompt = prompt;
  }

  @Override
  public void decode(ByteBuf buf, Direction direction, ProtocolVersion protocolVersion) {
    this.url = ProtocolUtils.readString(buf);
    this.hash = ProtocolUtils.readString(buf);
    if (protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_17) >= 0) {
      this.isRequired = buf.readBoolean();
      if (buf.readBoolean()) {
        this.prompt = GsonComponentSerializer.gson().deserialize(ProtocolUtils.readString(buf));
      } else {
        this.prompt = null;
      }
    }
  }

  @Override
  public void encode(ByteBuf buf, Direction direction, ProtocolVersion protocolVersion) {
    if (url == null || hash == null) {
      throw new IllegalStateException("Packet not fully filled in yet!");
    }
    ProtocolUtils.writeString(buf, url);
    ProtocolUtils.writeString(buf, hash);
    if (protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_17) >= 0) {
      buf.writeBoolean(isRequired);
      if (prompt != null) {
        buf.writeBoolean(true);
        ProtocolUtils.writeString(buf, GsonComponentSerializer.gson().serialize(prompt));
      } else {
        buf.writeBoolean(false);
      }
    }
  }

  public VelocityResourcePackInfo toServerPromptedPack() {
    ResourcePackInfo.Builder builder =
        new VelocityResourcePackInfo.BuilderImpl(Preconditions.checkNotNull(url)).setPrompt(prompt)
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
    return "ResourcePackRequest{" + "url='" + url + '\'' + ", hash='" + hash + '\'' +
        ", isRequired=" + isRequired + ", prompt='" + prompt + '\'' + '}';
  }
}
