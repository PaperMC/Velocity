/*
 * Copyright (C) 2023 Velocity Contributors
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

package com.velocitypowered.proxy.provider;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.event.ClickEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Taken from Paper <a
 * href="https://github.com/PaperMC/Paper/blob/master/patches/server/0010-Adventure.patch">Adventure
 * Patch</a>.
 */
@SuppressWarnings("UnstableApiUsage") // permitted provider
public class ClickCallbackProviderImpl implements ClickCallback.Provider {
  public static final CallbackManager CALLBACK_MANAGER = new CallbackManager();

  @Override
  public @NotNull ClickEvent create(
      final @NotNull ClickCallback<Audience> callback,
      final ClickCallback.@NotNull Options options) {
    return ClickEvent.runCommand(
        "/velocity callback " + CALLBACK_MANAGER.addCallback(callback, options));
  }

  /** The callback manager. */
  public static final class CallbackManager {

    private final Map<UUID, StoredCallback> callbacks = new HashMap<>();
    private final Queue<StoredCallback> queue = new ConcurrentLinkedQueue<>();

    private CallbackManager() {}

    /**
     * Add a callback to the queue.
     *
     * @param callback the callback to add to the queue
     * @param options callback options
     * @return the {@link UUID} of the callback
     */
    public UUID addCallback(
        final @NotNull ClickCallback<Audience> callback,
        final ClickCallback.@NotNull Options options) {
      final UUID id = UUID.randomUUID();
      this.queue.add(new StoredCallback(callback, options, id));
      return id;
    }

    /**
     * Handle queue.
     */
    public void handleQueue() {
      // Evict expired entries
      this.callbacks.values().removeIf(callback -> !callback.valid());

      // Add entries from queue
      StoredCallback callback;
      while ((callback = this.queue.poll()) != null) {
        this.callbacks.put(callback.id(), callback);
      }
    }

    /**
     * Run a callback.
     *
     * @param audience the audience
     * @param id the callbacks id
     */
    public void runCallback(final @NotNull Audience audience, final UUID id) {
      final StoredCallback callback = this.callbacks.get(id);
      if (callback != null && callback.valid()) { // TODO Message if expired/invalid?
        callback.takeUse();
        callback.callback.accept(audience);
      }
    }
  }

  /** A stored callback. */
  private static final class StoredCallback {
    private final long startedAt = System.nanoTime();
    private final ClickCallback<Audience> callback;
    private final long lifetime;
    private final UUID id;
    private int remainingUses;

    private StoredCallback(
        final @NotNull ClickCallback<Audience> callback,
        final ClickCallback.@NotNull Options options,
        final UUID id) {
      this.callback = callback;
      this.lifetime = options.lifetime().toNanos();
      this.remainingUses = options.uses();
      this.id = id;
    }

    public void takeUse() {
      if (this.remainingUses != ClickCallback.UNLIMITED_USES) {
        this.remainingUses--;
      }
    }

    public boolean hasRemainingUses() {
      return this.remainingUses == ClickCallback.UNLIMITED_USES || this.remainingUses > 0;
    }

    public boolean expired() {
      return System.nanoTime() - this.startedAt >= this.lifetime;
    }

    public boolean valid() {
      return hasRemainingUses() && !expired();
    }

    public UUID id() {
      return this.id;
    }
  }
}
