package com.velocitypowered.proxy.bossbar;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import com.velocitypowered.api.bossbar.BarColor;
import com.velocitypowered.api.bossbar.BarStyle;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.packet.BossBar;
import net.kyori.text.Component;
import net.kyori.text.serializer.gson.GsonComponentSerializer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class VelocityBossBar implements com.velocitypowered.api.bossbar.BossBar {

    private final List<Player> players;
    private final UUID uuid;
    private boolean visible;
    private Component title;
    private float progress;
    private BarColor color;
    private BarStyle style;
    private final VelocityBossBarManager manager;

    public VelocityBossBar(VelocityBossBarManager manager, Component title, BarColor color, BarStyle style, float progress, UUID uuid) {
        this.title = title;
        this.color = color;
        this.style = style;
        this.progress = progress;
        if (progress > 1) {
            throw new IllegalArgumentException("Progress not between 0 and 1");
        }
        this.uuid = uuid;
        visible = true;
        players = new ArrayList<>();
        this.manager = manager;
    }

    private VelocityBossBar() {
        throw new UnsupportedOperationException("This class cannot be instanced.");
    }

    @Override
    public void addAll(@NonNull Collection<Player> players) {
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
    public void removeAll(@NonNull Collection<Player> players) {
        players.forEach(this::removePlayer);
        if (players.equals(this.players)) {
            manager.removeFromMap(uuid);
            this.players.clear();
        }
    }

    @Override
    public void removeAllAdded() {
        removeAll(players);
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
        players.forEach(player -> {
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
        if (progress > 1) {
            throw new IllegalArgumentException("Progress not between 0 and 1");
        }
        this.progress = progress;
        BossBar bar = new BossBar();
        bar.setUuid(uuid);
        bar.setAction(BossBar.UPDATE_PERCENT);
        bar.setPercent(progress);
        players.forEach(player -> {
            if (player.isActive() && visible) {
                sendPacket(player, bar);
            }
        });
    }

    @Override
    public @Nullable Collection<Player> getPlayers() {
        return Collections.unmodifiableCollection(players);
    }

    @Override
    public @NonNull UUID getUUID() {
        return uuid;
    }

    @Override
    public @NonNull BarColor getColor() {
        return color;
    }

    @Override
    public void setColor(@NonNull BarColor color) {
        this.color = color;
        setDivisions(color, style);
    }

    @Override
    public @NonNull BarStyle getStyle() {
        return style;
    }

    @Override
    public void setStyle(@NonNull BarStyle style) {
        this.style = style;
        setDivisions(color, style);
    }

    private void setDivisions(BarColor color, BarStyle style) {
        BossBar bar = new BossBar();
        bar.setUuid(uuid);
        bar.setColor(color.getIntValue());
        bar.setOverlay(style.getIntValue());
        players.forEach(player -> {
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
            removeAllAdded();
        }
        this.visible = visible;
    }

    private BossBar addPacket() {
        BossBar bossBar = new BossBar();
        bossBar.setUuid(uuid);
        bossBar.setAction(BossBar.ADD);
        bossBar.setName(GsonComponentSerializer.INSTANCE.serialize(title));
        bossBar.setColor(color.getIntValue());
        bossBar.setOverlay(style.getIntValue());
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
        connected.getMinecraftConnection().write(packet);
    }
}
