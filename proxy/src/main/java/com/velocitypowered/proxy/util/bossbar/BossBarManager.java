package com.velocitypowered.proxy.util.bossbar;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.util.collect.Enum2IntMap;
import com.velocitypowered.proxy.util.collect.concurrent.SyncMap;
import com.velocitypowered.proxy.util.concurrent.Once;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
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
public class BossBarManager implements BossBar.Listener {
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
  private final SyncMap<BossBar, BossBarHolder> bars;

  public BossBarManager() {
    this.bars = SyncMap.of(WeakHashMap::new, 16);
  }

  private @Nullable BossBarHolder getHandler(BossBar bar) {
    return this.bars.get(bar);
  }

  private BossBarHolder getOrCreateHandler(BossBar bar) {
    BossBarHolder holder = this.bars.computeIfAbsent(bar, (k) -> new BossBarHolder(bar));
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
      player.getConnection().write(holder.createAddPacket(player.getProtocolVersion()));
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
    com.velocitypowered.proxy.protocol.packet.BossBar pre116Packet = holder.createTitleUpdate(
        newName, ProtocolVersion.MINECRAFT_1_15_2);
    com.velocitypowered.proxy.protocol.packet.BossBar rgbPacket = holder.createTitleUpdate(
        newName, ProtocolVersion.MINECRAFT_1_16);
    for (ConnectedPlayer player : holder.subscribers) {
      if (player.getProtocolVersion().compareTo(ProtocolVersion.MINECRAFT_1_16) >= 0) {
        player.getConnection().write(rgbPacket);
      } else {
        player.getConnection().write(pre116Packet);
      }
    }
  }

  @Override
  public void bossBarPercentChanged(@NonNull BossBar bar, float oldPercent, float newPercent) {
    BossBarHolder holder = this.getHandler(bar);
    if (holder == null) {
      return;
    }
    com.velocitypowered.proxy.protocol.packet.BossBar packet = holder
        .createPercentUpdate(newPercent);
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
    com.velocitypowered.proxy.protocol.packet.BossBar packet = holder.createColorUpdate(newColor);
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
    com.velocitypowered.proxy.protocol.packet.BossBar packet = holder
        .createOverlayUpdate(newOverlay);
    for (ConnectedPlayer player : holder.subscribers) {
      player.getConnection().write(packet);
    }
  }

  @Override
  public void bossBarFlagsChanged(@NonNull BossBar bar, @NonNull Set<Flag> oldFlags,
      @NonNull Set<Flag> newFlags) {
    BossBarHolder holder = this.getHandler(bar);
    if (holder == null) {
      return;
    }
    com.velocitypowered.proxy.protocol.packet.BossBar packet = holder.createFlagsUpdate(newFlags);
    for (ConnectedPlayer player : holder.subscribers) {
      player.getConnection().write(packet);
    }
  }

  private class BossBarHolder {
    private final UUID id = UUID.randomUUID();
    private final BossBar bar;
    private final Set<ConnectedPlayer> subscribers = SyncMap.setOf(WeakHashMap::new, 16);
    private final Once registrationOnce = new Once();

    BossBarHolder(BossBar bar) {
      this.bar = bar;
    }

    void register() {
      registrationOnce.run(() -> this.bar.addListener(BossBarManager.this));
    }

    com.velocitypowered.proxy.protocol.packet.BossBar createRemovePacket() {
      return com.velocitypowered.proxy.protocol.packet.BossBar.createRemovePacket(this.id);
    }

    com.velocitypowered.proxy.protocol.packet.BossBar createAddPacket(ProtocolVersion version) {
      com.velocitypowered.proxy.protocol.packet.BossBar packet = new com.velocitypowered
          .proxy.protocol.packet.BossBar();
      packet.setUuid(this.id);
      packet.setAction(com.velocitypowered.proxy.protocol.packet.BossBar.ADD);
      packet.setName(ProtocolUtils.getJsonChatSerializer(version).serialize(bar.name()));
      packet.setColor(COLORS_TO_PROTOCOL.get(bar.color()));
      packet.setOverlay(bar.overlay().ordinal());
      packet.setPercent(bar.percent());
      packet.setFlags(serializeFlags(bar.flags()));
      return packet;
    }

    com.velocitypowered.proxy.protocol.packet.BossBar createPercentUpdate(float newPercent) {
      com.velocitypowered.proxy.protocol.packet.BossBar packet = new com.velocitypowered
          .proxy.protocol.packet.BossBar();
      packet.setUuid(this.id);
      packet.setAction(com.velocitypowered.proxy.protocol.packet.BossBar.UPDATE_PERCENT);
      packet.setPercent(newPercent);
      return packet;
    }

    com.velocitypowered.proxy.protocol.packet.BossBar createColorUpdate(Color color) {
      com.velocitypowered.proxy.protocol.packet.BossBar packet = new com.velocitypowered
          .proxy.protocol.packet.BossBar();
      packet.setUuid(this.id);
      packet.setAction(com.velocitypowered.proxy.protocol.packet.BossBar.UPDATE_NAME);
      packet.setColor(COLORS_TO_PROTOCOL.get(color));
      packet.setFlags(serializeFlags(bar.flags()));
      return packet;
    }

    com.velocitypowered.proxy.protocol.packet.BossBar createTitleUpdate(Component name,
        ProtocolVersion version) {
      com.velocitypowered.proxy.protocol.packet.BossBar packet = new com.velocitypowered
          .proxy.protocol.packet.BossBar();
      packet.setUuid(this.id);
      packet.setAction(com.velocitypowered.proxy.protocol.packet.BossBar.UPDATE_NAME);
      packet.setName(ProtocolUtils.getJsonChatSerializer(version).serialize(name));
      return packet;
    }

    com.velocitypowered.proxy.protocol.packet.BossBar createFlagsUpdate(Set<Flag> newFlags) {
      com.velocitypowered.proxy.protocol.packet.BossBar packet = new com.velocitypowered
          .proxy.protocol.packet.BossBar();
      packet.setUuid(this.id);
      packet.setAction(com.velocitypowered.proxy.protocol.packet.BossBar.UPDATE_PROPERTIES);
      packet.setColor(COLORS_TO_PROTOCOL.get(this.bar.color()));
      packet.setFlags(this.serializeFlags(newFlags));
      return packet;
    }

    com.velocitypowered.proxy.protocol.packet.BossBar createOverlayUpdate(Overlay overlay) {
      com.velocitypowered.proxy.protocol.packet.BossBar packet = new com.velocitypowered
          .proxy.protocol.packet.BossBar();
      packet.setUuid(this.id);
      packet.setAction(com.velocitypowered.proxy.protocol.packet.BossBar.UPDATE_PROPERTIES);
      packet.setOverlay(OVERLAY_TO_PROTOCOL.get(overlay));
      return packet;
    }

    private byte serializeFlags(Set<Flag> flags) {
      byte val = 0x0;
      for (Flag flag : flags) {
        val |= FLAG_BITS_TO_PROTOCOL.get(flag);
      }
      return val;
    }
  }
}
