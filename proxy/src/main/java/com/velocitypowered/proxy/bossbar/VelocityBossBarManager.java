package com.velocitypowered.proxy.bossbar;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.velocitypowered.api.bossbar.BarColor;
import com.velocitypowered.api.bossbar.BarStyle;
import com.velocitypowered.api.bossbar.BossBar;
import com.velocitypowered.api.bossbar.BossBarManager;
import net.kyori.text.Component;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class VelocityBossBarManager implements BossBarManager {

    private final Map<UUID, BossBar> bossBars = new ConcurrentHashMap<>();

    @Override
    public @NonNull BossBar create(@NonNull Component title, @NonNull BarColor color, @NonNull BarStyle style) {
        return create(title, color, style, 1);
    }

    @Override
    public @NonNull BossBar create(@NonNull Component title, @NonNull BarColor color, @NonNull BarStyle style, float progress) {
        VelocityBossBar bossBar = new VelocityBossBar(this, title, color, style, progress, UUID.randomUUID());
        bossBars.put(bossBar.getUUID(), bossBar);
        return bossBar;
    }

    @Override
    public @Nullable BossBar create(@NonNull Component title, @NonNull BarColor color, @NonNull BarStyle style, float progress, @NonNull UUID uuid) {
        if (bossBars.containsKey(uuid)) {
            return null;
        }
        VelocityBossBar bossBar = new VelocityBossBar(this, title, color, style, progress, uuid);
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
        bar.removeAllAdded();
        return true;
    }

    void removeFromMap(UUID uuid) {
        bossBars.remove(uuid);
    }

    @Override
    public boolean remove(@NonNull BossBar bossBar) {
        bossBar.removeAllAdded();
        return true;
    }

    @Override
    public @Nullable Collection<BossBar> getRegisteredBossBars() {
        return Collections.unmodifiableCollection(bossBars.values());
    }
}
