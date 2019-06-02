package com.velocitypowered.proxy.util.bossbar;

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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.kyori.text.Component;
import net.kyori.text.serializer.gson.GsonComponentSerializer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class VelocityBossBar implements com.velocitypowered.api.util.bossbar.BossBar {

  private final List<Player> players;
  private final Set<BossBarFlag> flags;
  private final UUID uuid;
  private boolean visible;
  private Component title;
  private float progress;
  private BossBarColor color;
  private BossBarOverlay overlay;

  public VelocityBossBar(
      Component title, BossBarColor color, BossBarOverlay overlay, float progress, UUID uuid) {
    this.title = title;
    this.color = color;
    this.overlay = overlay;
    this.progress = progress;
    if (progress > 1 || progress < 0) {
      throw new IllegalArgumentException("Progress not between 0 and 1");
    }
    this.uuid = uuid;
    visible = true;
    players = new ArrayList<>();
    flags = new HashSet<>();
  }

  @Override
  public void addPlayers(@NonNull Iterable<Player> players) {
    players.forEach(this::addPlayer);
  }

  @Override
  public void addPlayer(@NonNull Player player) {
    if (!players.contains(player)) {
      players.add(player);
    }
    if (player.isActive() && visible) {
      sendPacket(player, addPacket());
    }
  }

  @Override
  public void removePlayer(@NonNull Player player) {
    players.remove(player);
    if (player.isActive()) {
      sendPacket(player, removePacket());
    }
  }

  @Override
  public void removePlayers(@NonNull Iterable<Player> players) {
    players.forEach(this::removePlayer);
    if (players.equals(this.players)) {
      this.players.clear();
    }
  }

  @Override
  public void removeAllPlayers() {
    removePlayers(players);
  }

  @Override
  public @NonNull Component getTitle() {
    return title;
  }

  @Override
  public void setTitle(@NonNull Component title) {
    this.title = title;
    BossBar bar = new BossBar();
    bar.setUuid(uuid);
    bar.setAction(BossBar.UPDATE_NAME);
    bar.setName(GsonComponentSerializer.INSTANCE.serialize(title));
    players.forEach(
        player -> {
          if (player.isActive() && visible) {
            sendPacket(player, bar);
          }
        });
  }

  @Override
  public float getProgress() {
    return progress;
  }

  @Override
  public void setProgress(float progress) {
    if (progress > 1 || progress < 0) {
      throw new IllegalArgumentException("Progress not between 0 and 1");
    }
    this.progress = progress;
    BossBar bar = new BossBar();
    bar.setUuid(uuid);
    bar.setAction(BossBar.UPDATE_PERCENT);
    bar.setPercent(progress);
    players.forEach(
        player -> {
          if (player.isActive() && visible) {
            sendPacket(player, bar);
          }
        });
  }

  @Override
  public @Nullable Collection<Player> getPlayers() {
    return ImmutableList.copyOf(players);
  }

  @Override
  public @NonNull BossBarColor getColor() {
    return color;
  }

  @Override
  public void setColor(@NonNull BossBarColor color) {
    this.color = color;
    setDivisions(color, overlay);
  }

  @Override
  public @NonNull BossBarOverlay getOverlay() {
    return overlay;
  }

  @Override
  public void setOverlay(@NonNull BossBarOverlay overlay) {
    this.overlay = overlay;
    setDivisions(color, overlay);
  }

  private void setDivisions(BossBarColor color, BossBarOverlay overlay) {
    BossBar bar = new BossBar();
    bar.setUuid(uuid);
    bar.setAction(BossBar.UPDATE_STYLE);
    bar.setColor(color.ordinal());
    bar.setOverlay(overlay.ordinal());
    players.forEach(
        player -> {
          if (player.isActive() && visible) {
            sendPacket(player, bar);
          }
        });
  }

  @Override
  public boolean isVisible() {
    return visible;
  }

  @Override
  public void setVisible(boolean visible) {
    if (!visible) {
      players.forEach(this::removePlayer);
    }
    this.visible = visible;
  }

  @Override
  public Collection<BossBarFlag> getFlags() {
    return ImmutableList.copyOf(flags);
  }

  @Override
  public void addFlags(BossBarFlag... flags) {

  }

  @Override
  public void removeFlag(BossBarFlag flag) {

  }

  @Override
  public void removeFlags(BossBarFlag... flags) {

  }

  private byte get(BossBarFlag flag) {
    switch (flag) {
      case DARKEN_SKY:
        return 0x01;
      case DRAGON_BAR:
        return 0x02;
      default:
        return 0x04;
    }
  }

  private BossBar addPacket() {
    BossBar bossBar = new BossBar();
    bossBar.setUuid(uuid);
    bossBar.setAction(BossBar.ADD);
    bossBar.setName(GsonComponentSerializer.INSTANCE.serialize(title));
    bossBar.setColor(color.ordinal());
    bossBar.setOverlay(overlay.ordinal());
    bossBar.setPercent(progress);
    return bossBar;
  }

  private BossBar removePacket() {
    BossBar bossBar = new BossBar();
    bossBar.setUuid(uuid);
    bossBar.setAction(BossBar.REMOVE);
    return bossBar;
  }

  private void sendPacket(Player player, MinecraftPacket packet) {
    ConnectedPlayer connected = (ConnectedPlayer) player;
    if (connected.getMinecraftConnection().getProtocolVersion().getProtocol()
        < ProtocolVersion.MINECRAFT_1_9.getProtocol()) {
      throw new IllegalArgumentException("Boss bars cannot be send on versions under 1.9!");
    }
    connected.getMinecraftConnection().write(packet);
  }
}
