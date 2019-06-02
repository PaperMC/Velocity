package com.velocitypowered.proxy.bossbar;

import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.bossbar.BossBar;
import com.velocitypowered.api.bossbar.BossBarColor;
import com.velocitypowered.api.bossbar.BossBarManager;
import com.velocitypowered.api.bossbar.BossBarOverlay;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.kyori.text.Component;
import org.checkerframework.checker.nullness.qual.NonNull;

public class VelocityBossBarManager implements BossBarManager {

  private final Set<BossBar> bossBars = new HashSet<>();

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
    bossBars.add(bossBar);
    return bossBar;
  }

  void removeFromSet(BossBar bar) {
    bossBars.remove(bar);
  }

  @Override
  public Collection<BossBar> getRegisteredBossBars() {
    return ImmutableList.copyOf(bossBars);
  }
}
