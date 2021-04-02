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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.util.bossbar.BossBarColor;
import com.velocitypowered.api.util.bossbar.BossBarFlag;
import com.velocitypowered.api.util.bossbar.BossBarOverlay;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.packet.BossBar;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.kyori.text.Component;
import net.kyori.text.serializer.gson.GsonComponentSerializer;

public class VelocityBossBar implements com.velocitypowered.api.util.bossbar.BossBar {

  private final List<Player> players;
  private final Set<BossBarFlag> flags;
  private final UUID uuid;
  private boolean visible;
  private Component title;
  private float percent;
  private BossBarColor color;
  private BossBarOverlay overlay;

  /**
   * Creates a new boss bar.
   * @param title the title for the bar
   * @param color the color of the bar
   * @param overlay the overlay to use
   * @param percent the percent of the bar
   */
  public VelocityBossBar(
      Component title, BossBarColor color, BossBarOverlay overlay, float percent) {
    this.title = checkNotNull(title, "title");
    this.color = checkNotNull(color, "color");
    this.overlay = checkNotNull(overlay, "overlay");
    this.percent = percent;
    checkPercent(percent);
    this.uuid = UUID.randomUUID();
    visible = true;
    players = new ArrayList<>();
    flags = EnumSet.noneOf(BossBarFlag.class);
  }

  @Override
  public void addPlayers(Iterable<Player> players) {
    checkNotNull(players, "players");
    for (Player player : players) {
      addPlayer(player);
    }
  }

  @Override
  public void addPlayer(Player player) {
    checkNotNull(player, "player");
    if (!players.contains(player)) {
      players.add(player);
    }
    if (player.isActive() && visible) {
      sendPacket(player, addPacket());
    }
  }

  @Override
  public void removePlayer(Player player) {
    checkNotNull(player, "player");
    players.remove(player);
    if (player.isActive()) {
      sendPacket(player, removePacket());
    }
  }

  @Override
  public void removePlayers(Iterable<Player> players) {
    checkNotNull(players, "players");
    for (Player player : players) {
      removePlayer(player);
    }
  }

  @Override
  public void removeAllPlayers() {
    removePlayers(ImmutableList.copyOf(players));
  }

  @Override
  public Component getTitle() {
    return title;
  }

  @Override
  public void setTitle(Component title) {
    this.title = checkNotNull(title, "title");
    if (visible) {
      BossBar bar = new BossBar();
      bar.setUuid(uuid);
      bar.setAction(BossBar.UPDATE_NAME);
      bar.setName(GsonComponentSerializer.INSTANCE.serialize(title));
      sendToAffected(bar);
    }
  }

  @Override
  public float getPercent() {
    return percent;
  }

  @Override
  public void setPercent(float percent) {
    checkPercent(percent);
    this.percent = percent;
    if (visible) {
      BossBar bar = new BossBar();
      bar.setUuid(uuid);
      bar.setAction(BossBar.UPDATE_PERCENT);
      bar.setPercent(percent);
      sendToAffected(bar);
    }
  }

  private void checkPercent(final float percent) {
    if (percent < 0f || percent > 1f) {
      throw new IllegalArgumentException("Percent must be between 0 and 1");
    }
  }

  @Override
  public Collection<Player> getPlayers() {
    return ImmutableList.copyOf(players);
  }

  @Override
  public BossBarColor getColor() {
    return color;
  }

  @Override
  public void setColor(BossBarColor color) {
    this.color = checkNotNull(color, "color");
    if (visible) {
      sendDivisions(color, overlay);
    }
  }

  @Override
  public BossBarOverlay getOverlay() {
    return overlay;
  }

  @Override
  public void setOverlay(BossBarOverlay overlay) {
    this.overlay = checkNotNull(overlay, "overlay");
    if (visible) {
      sendDivisions(color, overlay);
    }
  }

  private void sendDivisions(BossBarColor color, BossBarOverlay overlay) {
    BossBar bar = new BossBar();
    bar.setUuid(uuid);
    bar.setAction(BossBar.UPDATE_STYLE);
    bar.setColor(color.ordinal());
    bar.setOverlay(overlay.ordinal());
    sendToAffected(bar);
  }

  @Override
  public boolean isVisible() {
    return visible;
  }

  @Override
  public void setVisible(boolean visible) {
    boolean previous = this.visible;
    if (previous && !visible) {
      // The bar is being hidden
      sendToAffected(removePacket());
    } else if (!previous && visible) {
      // The bar is being shown
      sendToAffected(addPacket());
    }
    this.visible = visible;
  }

  @Override
  public Collection<BossBarFlag> getFlags() {
    return ImmutableList.copyOf(flags);
  }

  @Override
  public void addFlags(BossBarFlag... flags) {
    if (this.flags.addAll(Arrays.asList(flags)) && visible) {
      sendToAffected(updateFlags());
    }
  }

  @Override
  public void removeFlag(BossBarFlag flag) {
    checkNotNull(flag, "flag");
    if (this.flags.remove(flag) && visible) {
      sendToAffected(updateFlags());
    }
  }

  @Override
  public void removeFlags(BossBarFlag... flags) {
    if (this.flags.removeAll(Arrays.asList(flags)) && visible) {
      sendToAffected(updateFlags());
    }
  }

  private short serializeFlags() {
    short flagMask = 0x0;
    if (flags.contains(BossBarFlag.DARKEN_SCREEN)) {
      flagMask |= 0x1;
    }
    if (flags.contains(BossBarFlag.PLAY_BOSS_MUSIC)) {
      flagMask |= 0x2;
    }
    if (flags.contains(BossBarFlag.CREATE_WORLD_FOG)) {
      flagMask |= 0x4;
    }
    return flagMask;
  }

  private BossBar addPacket() {
    BossBar bossBar = new BossBar();
    bossBar.setUuid(uuid);
    bossBar.setAction(BossBar.ADD);
    bossBar.setName(GsonComponentSerializer.INSTANCE.serialize(title));
    bossBar.setColor(color.ordinal());
    bossBar.setOverlay(overlay.ordinal());
    bossBar.setPercent(percent);
    bossBar.setFlags(serializeFlags());
    return bossBar;
  }

  private BossBar removePacket() {
    BossBar bossBar = new BossBar();
    bossBar.setUuid(uuid);
    bossBar.setAction(BossBar.REMOVE);
    return bossBar;
  }

  private BossBar updateFlags() {
    BossBar bossBar = new BossBar();
    bossBar.setUuid(uuid);
    bossBar.setAction(BossBar.UPDATE_PROPERTIES);
    bossBar.setFlags(serializeFlags());
    return bossBar;
  }

  private void sendToAffected(MinecraftPacket packet) {
    for (Player player : players) {
      if (player.isActive() && player.getProtocolVersion().getProtocol()
          >= ProtocolVersion.MINECRAFT_1_9.getProtocol()) {
        sendPacket(player, packet);
      }
    }
  }

  private void sendPacket(Player player, MinecraftPacket packet) {
    ConnectedPlayer connected = (ConnectedPlayer) player;
    connected.getConnection().write(packet);
  }
}
