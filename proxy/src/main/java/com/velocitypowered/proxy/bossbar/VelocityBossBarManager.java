package com.velocitypowered.proxy.bossbar;

import com.velocitypowered.api.bossbar.BossBar;
import com.velocitypowered.api.bossbar.BossBarColor;
import com.velocitypowered.api.bossbar.BossBarManager;
import com.velocitypowered.api.bossbar.BossBarOverlay;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.text.Component;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class VelocityBossBarManager implements BossBarManager {

  private final Map<UUID, BossBar> bossBars = new ConcurrentHashMap<>();

  @Override
  public @NonNull BossBar create(
      @NonNull Component title, @NonNull BossBarColor color, @NonNull BossBarOverlay overlay) {
    return create(title, color, overlay, 1);
  }

  @Override
  public @NonNull BossBar create(
      @NonNull Component title, @NonNull BossBarColor color, @NonNull BossBarOverlay overlay,
      float progress) {
    VelocityBossBar bossBar =
        new VelocityBossBar(this, title, color, overlay, progress, UUID.randomUUID());
    bossBars.put(bossBar.getUUID(), bossBar);
    return bossBar;
  }

  @Override
  public @Nullable BossBar create(
      @NonNull Component title,
      @NonNull BossBarColor color,
      @NonNull BossBarOverlay overlay,
      float progress,
      @NonNull UUID uuid) {
    if (bossBars.containsKey(uuid)) {
      return null;
    }
    VelocityBossBar bossBar = new VelocityBossBar(this, title, color, overlay, progress, uuid);
    bossBars.put(uuid, bossBar);
    return bossBar;
  }

  @Override
  public @Nullable BossBar get(@NonNull UUID uuid) {
    return bossBars.get(uuid);
  }

  @Override
  public boolean remove(@NonNull UUID uuid) {
    BossBar bar = get(uuid);
    if (bar == null) {
      return false;
    }
    bar.removeAllPlayers();
    return true;
  }

  void removeFromMap(UUID uuid) {
    bossBars.remove(uuid);
  }

  @Override
  public boolean remove(@NonNull BossBar bossBar) {
    bossBar.removeAllPlayers();
    return true;
  }

  @Override
  public @Nullable Collection<BossBar> getRegisteredBossBars() {
    return Collections.unmodifiableCollection(bossBars.values());
  }
}
