/*
 * Copyright (C) 2022-2023 Velocity Contributors
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
import com.velocitypowered.api.util.GameProfile;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.packet.chat.ComponentHolder;
import com.velocitypowered.proxy.protocol.packet.chat.RemoteChatSession;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

public class UpsertPlayerInfoPacket implements MinecraftPacket {

  private final EnumSet<Action> actions;
  private final List<Entry> entries;

  public UpsertPlayerInfoPacket() {
    this.actions = EnumSet.noneOf(Action.class);
    this.entries = new ArrayList<>();
  }

  public UpsertPlayerInfoPacket(Action action) {
    this.actions = EnumSet.of(action);
    this.entries = new ArrayList<>();
  }

  public UpsertPlayerInfoPacket(EnumSet<Action> actions, List<Entry> entries) {
    this.actions = actions;
    this.entries = entries;
  }

  public List<Entry> getEntries() {
    return entries;
  }

  public EnumSet<Action> getActions() {
    return actions;
  }

  public boolean containsAction(Action action) {
    return this.actions.contains(action);
  }

  public void addAction(Action action) {
    this.actions.add(action);
  }

  public void addAllActions(Collection<? extends Action> actions) {
    this.actions.addAll(actions);
  }

  public void addEntry(Entry entry) {
    this.entries.add(entry);
  }

  public void addAllEntries(Collection<? extends Entry> entries) {
    this.entries.addAll(entries);
  }

  @Override
  public void decode(ByteBuf buf, ProtocolUtils.Direction direction,
      ProtocolVersion protocolVersion) {
    Action[] actions = Action.class.getEnumConstants();
    byte[] bytes = new byte[-Math.floorDiv(-actions.length, 8)];
    buf.readBytes(bytes);
    BitSet actionSet = BitSet.valueOf(bytes);

    for (int idx = 0; idx < actions.length; idx++) {
      if (actionSet.get(idx)) {
        addAction(actions[idx]);
      }
    }

    int length = ProtocolUtils.readVarInt(buf);
    for (int idx = 0; idx < length; idx++) {
      Entry entry = new Entry(ProtocolUtils.readUuid(buf));
      for (Action action : this.actions) {
        action.read.read(protocolVersion, buf, entry);
      }
      addEntry(entry);
    }
  }

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction,
      ProtocolVersion protocolVersion) {
    Action[] actions = Action.class.getEnumConstants();
    BitSet set = new BitSet(actions.length);
    for (int idx = 0; idx < actions.length; idx++) {
      set.set(idx, this.actions.contains(actions[idx]));
    }

    byte[] bytes = set.toByteArray();
    buf.writeBytes(Arrays.copyOf(bytes, -Math.floorDiv(-actions.length, 8)));

    ProtocolUtils.writeVarInt(buf, this.entries.size());
    for (Entry entry : this.entries) {
      ProtocolUtils.writeUuid(buf, entry.profileId);

      for (Action action : this.actions) {
        action.write.write(protocolVersion, buf, entry);
      }
    }
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }

  public BitSet readFixedBitSet(ByteBuf buf, int param0) {
    byte[] var0 = new byte[-Math.floorDiv(-param0, 8)];
    buf.readBytes(var0);
    return BitSet.valueOf(var0);
  }

  public enum Action {
    ADD_PLAYER((ignored, buf, info) -> { // read
      info.profile = new GameProfile(
          info.profileId,
          ProtocolUtils.readString(buf, 16),
          ProtocolUtils.readProperties(buf)
      );
    }, (ignored, buf, info) -> { // write
      ProtocolUtils.writeString(buf, info.profile.getName());
      ProtocolUtils.writeProperties(buf, info.profile.getProperties());
    }),
    INITIALIZE_CHAT((version, buf, info) -> { // read
      if (buf.readBoolean()) {
        info.chatSession = new RemoteChatSession(version, buf);
      } else {
        info.chatSession = null;
      }
    }, (ignored, buf, info) -> { // write
      buf.writeBoolean(info.chatSession != null);
      if (info.chatSession != null) {
        info.chatSession.write(buf);
      }
    }),
    UPDATE_GAME_MODE((ignored, buf, info) -> { // read
      info.gameMode = ProtocolUtils.readVarInt(buf);
    }, (ignored, buf, info) -> { // write
      ProtocolUtils.writeVarInt(buf, info.gameMode);
    }),
    UPDATE_LISTED((ignored, buf, info) -> { // read
      info.listed = buf.readBoolean();
    }, (ignored, buf, info) -> { // write
      buf.writeBoolean(info.listed);
    }),
    UPDATE_LATENCY((ignored, buf, info) -> { // read
      info.latency = ProtocolUtils.readVarInt(buf);
    }, (ignored, buf, info) -> { // write
      ProtocolUtils.writeVarInt(buf, info.latency);
    }),
    UPDATE_DISPLAY_NAME((version, buf, info) -> { // read
      if (buf.readBoolean()) {
        info.displayName = ComponentHolder.read(buf, version);
      } else {
        info.displayName = null;
      }
    }, (version, buf, info) -> { // write
      buf.writeBoolean(info.displayName != null);
      if (info.displayName != null) {
        info.displayName.write(buf);
      }
    }),
    UPDATE_LIST_ORDER((version, buf, info) -> { // read
      info.listOrder = ProtocolUtils.readVarInt(buf);
    }, (version, buf, info) -> { // write
      ProtocolUtils.writeVarInt(buf, info.listOrder);
    }),
    UPDATE_HAT((version, buf, info) -> { // read
      info.showHat = buf.readBoolean();
    }, (version, buf, info) -> { // write
      buf.writeBoolean(info.showHat);
    });

    private final Read read;
    private final Write write;

    Action(Read read, Write write) {
      this.read = read;
      this.write = write;
    }

    private interface Read {

      void read(ProtocolVersion version, ByteBuf buf, Entry info);
    }

    private interface Write {

      void write(ProtocolVersion version, ByteBuf buf, Entry info);
    }
  }

  public static class Entry {

    private final UUID profileId;
    private GameProfile profile;
    private boolean listed;
    private int latency;
    private int gameMode;
    @Nullable
    private ComponentHolder displayName;
    private boolean showHat;
    private int listOrder;
    @Nullable
    private RemoteChatSession chatSession;

    public Entry(UUID uuid) {
      this.profileId = uuid;
    }

    public UUID getProfileId() {
      return profileId;
    }

    public GameProfile getProfile() {
      return profile;
    }

    public boolean isListed() {
      return listed;
    }

    public int getLatency() {
      return latency;
    }

    public int getGameMode() {
      return gameMode;
    }

    @Nullable
    public ComponentHolder getDisplayName() {
      return displayName;
    }

    public boolean isShowHat() {
      return showHat;
    }

    public int getListOrder() {
      return listOrder;
    }

    @Nullable
    public RemoteChatSession getChatSession() {
      return chatSession;
    }

    public void setProfile(GameProfile profile) {
      this.profile = profile;
    }

    public void setListed(boolean listed) {
      this.listed = listed;
    }

    public void setLatency(int latency) {
      this.latency = latency;
    }

    public void setGameMode(int gameMode) {
      this.gameMode = gameMode;
    }

    public void setDisplayName(@Nullable ComponentHolder displayName) {
      this.displayName = displayName;
    }

    public void setShowHat(boolean showHat) {
      this.showHat = showHat;
    }

    public void setListOrder(int listOrder) {
      this.listOrder = listOrder;
    }

    public void setChatSession(@Nullable RemoteChatSession chatSession) {
      this.chatSession = chatSession;
    }

    @Override
    public String toString() {
      return "Entry{" +
          "profileId=" + profileId +
          ", profile=" + profile +
          ", listed=" + listed +
          ", latency=" + latency +
          ", gameMode=" + gameMode +
          ", displayName=" + displayName +
          ", listOrder=" + listOrder +
          ", chatSession=" + chatSession +
          '}';
    }
  }
}