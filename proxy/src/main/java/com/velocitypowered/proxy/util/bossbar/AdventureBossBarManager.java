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

package com.velocitypowered.proxy.util.bossbar;

import com.google.common.collect.MapMaker;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.network.ProtocolUtils;
import com.velocitypowered.proxy.network.packet.clientbound.ClientboundBossBarPacket;
import com.velocitypowered.proxy.util.collect.Enum2IntMap;
import com.velocitypowered.proxy.util.concurrent.Once;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.bossbar.BossBar.Color;
import net.kyori.adventure.bossbar.BossBar.Flag;
import net.kyori.adventure.bossbar.BossBar.Overlay;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Manages all boss bars known to the proxy.
 */
public class AdventureBossBarManager implements BossBar.Listener {
  private static final Enum2IntMap<Color> COLORS_TO_PROTOCOL =
      new Enum2IntMap.Builder<>(Color.class)
          .put(Color.PINK, 0)
          .put(Color.BLUE, 1)
          .put(Color.RED, 2)
          .put(Color.GREEN, 3)
          .put(Color.YELLOW, 4)
          .put(Color.PURPLE, 5)
          .put(Color.WHITE, 6)
          .build();
  private static final Enum2IntMap<Overlay> OVERLAY_TO_PROTOCOL =
      new Enum2IntMap.Builder<>(Overlay.class)
          .put(Overlay.PROGRESS, 0)
          .put(Overlay.NOTCHED_6, 1)
          .put(Overlay.NOTCHED_10, 2)
          .put(Overlay.NOTCHED_12, 3)
          .put(Overlay.NOTCHED_20, 4)
          .build();
  private static final Enum2IntMap<Flag> FLAG_BITS_TO_PROTOCOL =
      new Enum2IntMap.Builder<>(Flag.class)
          .put(Flag.DARKEN_SCREEN, 0x1)
          .put(Flag.PLAY_BOSS_MUSIC, 0x2)
          .put(Flag.CREATE_WORLD_FOG, 0x4)
          .build();
  private final Map<BossBar, BossBarHolder> bars;

  public AdventureBossBarManager() {
    this.bars = new MapMaker().weakKeys().makeMap();
  }

  private @Nullable BossBarHolder getHandler(BossBar bar) {
    return this.bars.get(bar);
  }

  private BossBarHolder getOrCreateHandler(BossBar bar) {
    BossBarHolder holder = this.bars.computeIfAbsent(bar, k -> new BossBarHolder(bar));
    holder.register();
    return holder;
  }

  /**
   * Called when a player disconnects from the proxy. Removes the player from any boss bar
   * subscriptions.
   *
   * @param player the player to remove
   */
  public void onDisconnect(ConnectedPlayer player) {
    for (BossBarHolder holder : bars.values()) {
      holder.subscribers.remove(player);
    }
  }

  /**
   * Adds the specified player to the boss bar's viewers and spawns the boss bar, registering the
   * boss bar if needed.
   * @param player the intended viewer
   * @param bar the boss bar to show
   */
  public void addBossBar(ConnectedPlayer player, BossBar bar) {
    BossBarHolder holder = this.getOrCreateHandler(bar);
    if (holder.subscribers.add(player)) {
      player.getConnection().write(holder.createAddPacket(player));
    }
  }

  /**
   * Removes the specified player to the boss bar's viewers and despawns the boss bar.
   * @param player the intended viewer
   * @param bar the boss bar to hide
   */
  public void removeBossBar(ConnectedPlayer player, BossBar bar) {
    BossBarHolder holder = this.getHandler(bar);
    if (holder != null && holder.subscribers.remove(player)) {
      player.getConnection().write(holder.createRemovePacket());
    }
  }

  @Override
  public void bossBarNameChanged(@NonNull BossBar bar, @NonNull Component oldName,
      @NonNull Component newName) {
    BossBarHolder holder = this.getHandler(bar);
    if (holder == null) {
      return;
    }
    for (ConnectedPlayer player : holder.subscribers) {
      Component translated = player.translateMessage(newName);

      if (player.protocolVersion().gte(ProtocolVersion.MINECRAFT_1_16)) {
        ClientboundBossBarPacket rgbPacket = holder.createTitleUpdate(
            translated, ProtocolVersion.MINECRAFT_1_16);
        player.getConnection().write(rgbPacket);
      } else {
        ClientboundBossBarPacket pre116Packet = holder.createTitleUpdate(
            translated, ProtocolVersion.MINECRAFT_1_15_2);
        player.getConnection().write(pre116Packet);
      }
    }
  }

  @Override
  public void bossBarProgressChanged(@NonNull BossBar bar, float oldProgress, float newProgress) {
    BossBarHolder holder = this.getHandler(bar);
    if (holder == null) {
      return;
    }
    ClientboundBossBarPacket packet = holder.createPercentUpdate(newProgress);
    for (ConnectedPlayer player : holder.subscribers) {
      player.getConnection().write(packet);
    }
  }

  @Override
  public void bossBarColorChanged(@NonNull BossBar bar, @NonNull Color oldColor,
      @NonNull Color newColor) {
    BossBarHolder holder = this.getHandler(bar);
    if (holder == null) {
      return;
    }
    ClientboundBossBarPacket packet = holder.createColorUpdate(newColor);
    for (ConnectedPlayer player : holder.subscribers) {
      player.getConnection().write(packet);
    }
  }

  @Override
  public void bossBarOverlayChanged(@NonNull BossBar bar, @NonNull Overlay oldOverlay,
      @NonNull Overlay newOverlay) {
    BossBarHolder holder = this.getHandler(bar);
    if (holder == null) {
      return;
    }
    ClientboundBossBarPacket packet = holder.createOverlayUpdate(newOverlay);
    for (ConnectedPlayer player : holder.subscribers) {
      player.getConnection().write(packet);
    }
  }

  @Override
  public void bossBarFlagsChanged(@NonNull BossBar bar, @NonNull Set<Flag> added,
      @NonNull Set<Flag> removed) {
    BossBarHolder holder = this.getHandler(bar);
    if (holder == null) {
      return;
    }
    ClientboundBossBarPacket packet = holder.createFlagsUpdate();
    for (ConnectedPlayer player : holder.subscribers) {
      player.getConnection().write(packet);
    }
  }

  private class BossBarHolder {
    private final UUID id = UUID.randomUUID();
    private final BossBar bar;
    private final Set<ConnectedPlayer> subscribers = Collections.newSetFromMap(
        new MapMaker().weakKeys().makeMap());
    private final Once registrationOnce = new Once();

    BossBarHolder(BossBar bar) {
      this.bar = bar;
    }

    void register() {
      registrationOnce.run(() -> this.bar.addListener(AdventureBossBarManager.this));
    }

    ClientboundBossBarPacket createRemovePacket() {
      return ClientboundBossBarPacket.createRemovePacket(this.id);
    }

    ClientboundBossBarPacket createAddPacket(ConnectedPlayer player) {
      ClientboundBossBarPacket packet = new ClientboundBossBarPacket();
      packet.setUuid(this.id);
      packet.setAction(ClientboundBossBarPacket.ADD);
      packet.setName(ProtocolUtils.getJsonChatSerializer(player.protocolVersion())
          .serialize(player.translateMessage(bar.name())));
      packet.setColor(COLORS_TO_PROTOCOL.get(bar.color()));
      packet.setOverlay(OVERLAY_TO_PROTOCOL.get(bar.overlay()));
      packet.setPercent(bar.progress());
      packet.setFlags(serializeFlags(bar.flags()));
      return packet;
    }

    ClientboundBossBarPacket createPercentUpdate(float newPercent) {
      ClientboundBossBarPacket packet = new ClientboundBossBarPacket();
      packet.setUuid(this.id);
      packet.setAction(ClientboundBossBarPacket.UPDATE_PERCENT);
      packet.setPercent(newPercent);
      return packet;
    }

    ClientboundBossBarPacket createColorUpdate(Color color) {
      ClientboundBossBarPacket packet = new ClientboundBossBarPacket();
      packet.setUuid(this.id);
      packet.setAction(ClientboundBossBarPacket.UPDATE_STYLE);
      packet.setColor(COLORS_TO_PROTOCOL.get(color));
      packet.setOverlay(OVERLAY_TO_PROTOCOL.get(bar.overlay()));
      packet.setFlags(serializeFlags(bar.flags()));
      return packet;
    }

    ClientboundBossBarPacket createTitleUpdate(Component name,
                                               ProtocolVersion version) {
      ClientboundBossBarPacket packet = new ClientboundBossBarPacket();
      packet.setUuid(this.id);
      packet.setAction(ClientboundBossBarPacket.UPDATE_NAME);
      packet.setName(ProtocolUtils.getJsonChatSerializer(version).serialize(name));
      return packet;
    }

    ClientboundBossBarPacket createFlagsUpdate() {
      return createFlagsUpdate(bar.flags());
    }

    ClientboundBossBarPacket createFlagsUpdate(Set<Flag> newFlags) {
      ClientboundBossBarPacket packet = new ClientboundBossBarPacket();
      packet.setUuid(this.id);
      packet.setAction(ClientboundBossBarPacket.UPDATE_PROPERTIES);
      packet.setColor(COLORS_TO_PROTOCOL.get(this.bar.color()));
      packet.setFlags(this.serializeFlags(newFlags));
      return packet;
    }

    ClientboundBossBarPacket createOverlayUpdate(Overlay overlay) {
      ClientboundBossBarPacket packet = new ClientboundBossBarPacket();
      packet.setUuid(this.id);
      packet.setAction(ClientboundBossBarPacket.UPDATE_PROPERTIES);
      packet.setColor(COLORS_TO_PROTOCOL.get(bar.color()));
      packet.setOverlay(OVERLAY_TO_PROTOCOL.get(overlay));
      return packet;
    }

    private byte serializeFlags(Set<Flag> flags) {
      byte val = 0x0;
      for (Flag flag : flags) {
        val = (byte) (val | FLAG_BITS_TO_PROTOCOL.get(flag));
      }
      return val;
    }
  }
}
