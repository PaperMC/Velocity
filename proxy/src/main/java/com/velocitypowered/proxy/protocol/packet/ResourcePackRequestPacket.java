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
import com.velocitypowered.proxy.protocol.PacketCodec;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.ProtocolUtils.Direction;
import com.velocitypowered.proxy.protocol.packet.chat.ComponentHolder;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import java.util.UUID;
import java.util.regex.Pattern;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class ResourcePackRequestPacket implements MinecraftPacket {

  private final @Nullable UUID id; // 1.20.3+
  private final String url;
  private final String hash;
  private final boolean isRequired; // 1.17+
  private final @Nullable ComponentHolder prompt; // 1.17+

  private static final Pattern PLAUSIBLE_SHA1_HASH = Pattern.compile("^[a-z0-9]{40}$"); // 1.20.2+

  public ResourcePackRequestPacket(@Nullable UUID id, String url, String hash,
                                   boolean isRequired, @Nullable ComponentHolder prompt) {
    this.id = id;
    this.url = url;
    this.hash = hash;
    this.isRequired = isRequired;
    this.prompt = prompt;
  }

  public @Nullable UUID getId() {
    return id;
  }

  public String getUrl() {
    return url;
  }

  public boolean isRequired() {
    return isRequired;
  }

  public String getHash() {
    return hash;
  }

  public @Nullable ComponentHolder getPrompt() {
    return prompt;
  }

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
    return "ResourcePackRequestPacket{" +
            "id=" + id +
            ", url='" + url + '\'' +
            ", hash='" + hash + '\'' +
            ", isRequired=" + isRequired +
            ", prompt=" + prompt +
            '}';
  }

  public static class Codec implements PacketCodec<ResourcePackRequestPacket> {
    public static final Codec INSTANCE = new Codec();

    @Override
    public ResourcePackRequestPacket decode(ByteBuf buf, Direction direction,
        ProtocolVersion protocolVersion) {
      UUID id = null;
      if (protocolVersion.noLessThan(ProtocolVersion.MINECRAFT_1_20_3)) {
        id = ProtocolUtils.readUuid(buf);
      }
      String url = ProtocolUtils.readString(buf);
      String hash = ProtocolUtils.readString(buf);
      boolean isRequired = false;
      ComponentHolder prompt = null;
      if (protocolVersion.noLessThan(ProtocolVersion.MINECRAFT_1_17)) {
        isRequired = buf.readBoolean();
        if (buf.readBoolean()) {
          prompt = ComponentHolder.read(buf, protocolVersion);
        }
      }
      return new ResourcePackRequestPacket(id, url, hash, isRequired, prompt);
    }

    @Override
    public void encode(ResourcePackRequestPacket packet, ByteBuf buf, Direction direction,
        ProtocolVersion protocolVersion) {
      if (protocolVersion.noLessThan(ProtocolVersion.MINECRAFT_1_20_3)) {
        if (packet.id == null) {
          throw new IllegalStateException("Resource pack id not set yet!");
        }
        ProtocolUtils.writeUuid(buf, packet.id);
      }
      if (packet.url == null || packet.hash == null) {
        throw new IllegalStateException("Packet not fully filled in yet!");
      }
      ProtocolUtils.writeString(buf, packet.url);
      ProtocolUtils.writeString(buf, packet.hash);
      if (protocolVersion.noLessThan(ProtocolVersion.MINECRAFT_1_17)) {
        buf.writeBoolean(packet.isRequired);
        if (packet.prompt != null) {
          buf.writeBoolean(true);
          packet.prompt.write(buf);
        } else {
          buf.writeBoolean(false);
        }
      }
    }
  }
}
