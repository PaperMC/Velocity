package com.velocitypowered.proxy.util.bossbar;

import com.google.common.base.Preconditions;
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
    flags = EnumSet.noneOf(BossBarFlag.class);
  }

  @Override
  public void addPlayers(@NonNull Iterable<Player> players) {
    Preconditions.checkNotNull(players, "players");
    for (Player player : players) {
      addPlayer(player);
    }
  }

  @Override
  public void addPlayer(@NonNull Player player) {
    Preconditions.checkNotNull(player, "player");
    if (!players.contains(player)) {
      players.add(player);
    }
    if (player.isActive() && visible) {
      sendPacket(player, addPacket());
    }
  }

  @Override
  public void removePlayer(@NonNull Player player) {
    Preconditions.checkNotNull(player, "player");
    players.remove(player);
    if (player.isActive()) {
      sendPacket(player, removePacket());
    }
  }

  @Override
  public void removePlayers(@NonNull Iterable<Player> players) {
    Preconditions.checkNotNull(players, "players");
    for (Player player : players) {
      removePlayer(player);
    }
  }

  @Override
  public void removeAllPlayers() {
    removePlayers(ImmutableList.copyOf(players));
  }

  @Override
  public @NonNull Component getTitle() {
    return title;
  }

  @Override
  public void setTitle(@NonNull Component title) {
    Preconditions.checkNotNull(title, "title");
    this.title = title;
    if (visible) {
      BossBar bar = new BossBar();
      bar.setUuid(uuid);
      bar.setAction(BossBar.UPDATE_NAME);
      bar.setName(GsonComponentSerializer.INSTANCE.serialize(title));
      sendToAffected(bar);
    }
  }

  @Override
  public float getProgress() {
    return progress;
  }

  @Override
  public void setProgress(float progress) {
    if (progress > 1 || progress < 0) {
      throw new IllegalArgumentException("Progress should be between 0 and 1");
    }
    this.progress = progress;
    if (visible) {
      BossBar bar = new BossBar();
      bar.setUuid(uuid);
      bar.setAction(BossBar.UPDATE_PERCENT);
      bar.setPercent(progress);
      sendToAffected(bar);
    }
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
    Preconditions.checkNotNull(color, "color");
    this.color = color;
    if (visible) {
      sendDivisions(color, overlay);
    }
  }

  @Override
  public @NonNull BossBarOverlay getOverlay() {
    return overlay;
  }

  @Override
  public void setOverlay(@NonNull BossBarOverlay overlay) {
    Preconditions.checkNotNull(overlay, "overlay");
    this.overlay = overlay;
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
    Preconditions.checkNotNull(flag, "flag");
    if (this.flags.remove(flag)) {
      sendToAffected(updateFlags());
    }
  }

  @Override
  public void removeFlags(BossBarFlag... flags) {
    if (this.flags.removeAll(Arrays.asList(flags))) {
      sendToAffected(updateFlags());
    }
  }

  private short serializeFlags() {
    short flagMask = 0x0;
    if (flags.contains(BossBarFlag.DARKEN_SKY)) {
      flagMask |= 0x1;
    }
    if (flags.contains(BossBarFlag.DRAGON_BAR)) {
      flagMask |= 0x2;
    }
    if (flags.contains(BossBarFlag.CREATE_FOG)) {
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
    bossBar.setPercent(progress);
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
    connected.getMinecraftConnection().write(packet);
  }
}
