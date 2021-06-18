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

package com.velocitypowered.proxy.network.java.packet.clientbound;

import com.google.common.base.MoreObjects;
import com.google.common.base.VerifyException;
import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.player.java.TabListEntry;
import com.velocitypowered.api.proxy.player.java.JavaPlayerIdentity;
import com.velocitypowered.proxy.network.ProtocolUtils;
import com.velocitypowered.proxy.network.java.packet.JavaPacketHandler;
import com.velocitypowered.proxy.network.packet.Packet;
import com.velocitypowered.proxy.network.packet.PacketReader;
import com.velocitypowered.proxy.network.packet.PacketWriter;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.checkerframework.checker.nullness.qual.Nullable;

public class ClientboundPlayerListItemPacket implements Packet {
  public static final PacketReader<ClientboundPlayerListItemPacket> DECODER = PacketReader.method(ClientboundPlayerListItemPacket::new);
  public static final PacketWriter<ClientboundPlayerListItemPacket> ENCODER = PacketWriter.deprecatedEncode();

  public static final int ADD_PLAYER = 0;
  public static final int UPDATE_GAMEMODE = 1;
  public static final int UPDATE_LATENCY = 2;
  public static final int UPDATE_DISPLAY_NAME = 3;
  public static final int REMOVE_PLAYER = 4;
  private int action;
  private final List<Item> items = new ArrayList<>();

  public ClientboundPlayerListItemPacket(int action, List<Item> items) {
    this.action = action;
    this.items.addAll(items);
  }

  public ClientboundPlayerListItemPacket() {
  }

  public int getAction() {
    return action;
  }

  public List<Item> getItems() {
    return items;
  }

  @Override
  public void decode(ByteBuf buf, ProtocolVersion version) {
    if (version.gte(ProtocolVersion.MINECRAFT_1_8)) {
      action = ProtocolUtils.readVarInt(buf);
      int length = ProtocolUtils.readVarInt(buf);

      for (int i = 0; i < length; i++) {
        Item item = new Item(ProtocolUtils.readUuid(buf));
        items.add(item);
        switch (action) {
          case ADD_PLAYER:
            item.setName(ProtocolUtils.readString(buf));
            item.setProperties(ProtocolUtils.readProperties(buf));
            item.setGameMode(ProtocolUtils.readVarInt(buf));
            item.setLatency(ProtocolUtils.readVarInt(buf));
            item.setDisplayName(readOptionalComponent(buf, version));
            break;
          case UPDATE_GAMEMODE:
            item.setGameMode(ProtocolUtils.readVarInt(buf));
            break;
          case UPDATE_LATENCY:
            item.setLatency(ProtocolUtils.readVarInt(buf));
            break;
          case UPDATE_DISPLAY_NAME:
            item.setDisplayName(readOptionalComponent(buf, version));
            break;
          case REMOVE_PLAYER:
            //Do nothing, all that is needed is the uuid
            break;
          default:
            throw new UnsupportedOperationException("Unknown action " + action);
        }
      }
    } else {
      Item item = new Item();
      item.setName(ProtocolUtils.readString(buf));
      action = buf.readBoolean() ? ADD_PLAYER : REMOVE_PLAYER;
      item.setLatency(buf.readShort());
      items.add(item);
    }
  }

  private static @Nullable Component readOptionalComponent(ByteBuf buf, ProtocolVersion version) {
    if (buf.readBoolean()) {
      return ProtocolUtils.getJsonChatSerializer(version)
          .deserialize(ProtocolUtils.readString(buf));
    }
    return null;
  }

  @Override
  public void encode(ByteBuf buf, ProtocolVersion version) {
    if (version.gte(ProtocolVersion.MINECRAFT_1_8)) {
      ProtocolUtils.writeVarInt(buf, action);
      ProtocolUtils.writeVarInt(buf, items.size());
      for (Item item : items) {
        UUID uuid = item.getUuid();
        if (uuid == null) {
          throw new VerifyException("UUID-less entry serialization attempt - 1.7 component!");
        }

        ProtocolUtils.writeUuid(buf, uuid);
        switch (action) {
          case ADD_PLAYER:
            ProtocolUtils.writeString(buf, item.getName());
            ProtocolUtils.writeProperties(buf, item.getProperties());
            ProtocolUtils.writeVarInt(buf, item.getGameMode());
            ProtocolUtils.writeVarInt(buf, item.getLatency());

            writeDisplayName(buf, item.getDisplayName(), version);
            break;
          case UPDATE_GAMEMODE:
            ProtocolUtils.writeVarInt(buf, item.getGameMode());
            break;
          case UPDATE_LATENCY:
            ProtocolUtils.writeVarInt(buf, item.getLatency());
            break;
          case UPDATE_DISPLAY_NAME:
            writeDisplayName(buf, item.getDisplayName(), version);
            break;
          case REMOVE_PLAYER:
            // Do nothing, all that is needed is the uuid
            break;
          default:
            throw new UnsupportedOperationException("Unknown action " + action);
        }
      }
    } else {
      Item item = items.get(0);
      Component displayNameComponent = item.getDisplayName();
      if (displayNameComponent != null) {
        String displayName = LegacyComponentSerializer.legacySection()
            .serialize(displayNameComponent);
        ProtocolUtils.writeString(buf,
            displayName.length() > 16 ? displayName.substring(0, 16) : displayName);
      } else {
        ProtocolUtils.writeString(buf, item.getName());
      }
      buf.writeBoolean(action != REMOVE_PLAYER);
      buf.writeShort(item.getLatency());
    }
  }

  @Override
  public boolean handle(JavaPacketHandler handler) {
    return handler.handle(this);
  }

  private void writeDisplayName(ByteBuf buf, @Nullable Component displayName,
      ProtocolVersion version) {
    buf.writeBoolean(displayName != null);
    if (displayName != null) {
      ProtocolUtils.writeString(buf, ProtocolUtils.getJsonChatSerializer(version)
          .serialize(displayName));
    }
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
      .add("action", this.action)
      .add("items", this.items)
      .toString();
  }

  public static class Item {

    private final @Nullable UUID uuid;
    private String name = "";
    private List<JavaPlayerIdentity.Property> properties = ImmutableList.of();
    private int gameMode;
    private int latency;
    private @Nullable Component displayName;

    public Item() {
      uuid = null;
    }

    public Item(@Nullable UUID uuid) {
      this.uuid = uuid;
    }

    public static Item from(TabListEntry entry) {
      return new Item(entry.gameProfile().uuid())
          .setName(entry.gameProfile().name())
          .setProperties(entry.gameProfile().properties())
          .setLatency(entry.ping())
          .setGameMode(entry.gameMode())
          .setDisplayName(entry.displayName());
    }

    public @Nullable UUID getUuid() {
      return uuid;
    }

    public String getName() {
      return name;
    }

    public Item setName(String name) {
      this.name = name;
      return this;
    }

    public List<JavaPlayerIdentity.Property> getProperties() {
      return properties;
    }

    public Item setProperties(List<JavaPlayerIdentity.Property> properties) {
      this.properties = properties;
      return this;
    }

    public int getGameMode() {
      return gameMode;
    }

    public Item setGameMode(int gameMode) {
      this.gameMode = gameMode;
      return this;
    }

    public int getLatency() {
      return latency;
    }

    public Item setLatency(int latency) {
      this.latency = latency;
      return this;
    }

    public @Nullable Component getDisplayName() {
      return displayName;
    }

    public Item setDisplayName(@Nullable Component displayName) {
      this.displayName = displayName;
      return this;
    }
  }
}
