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

package com.velocitypowered.proxy.adventure;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.event.ClickCallback;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
record RegisteredCallback(
    Duration duration,
    @Nullable AtomicInteger remainingUses,
    ClickCallback<Audience> callback
) {
  RegisteredCallback(
      final Duration duration,
      final int maxUses,
      final ClickCallback<Audience> callback
  ) {
    this(
        duration,
        maxUses == ClickCallback.UNLIMITED_USES
          ? null
          : new AtomicInteger(maxUses),
        callback
    );
  }

  boolean tryUse() {
    if (this.remainingUses != null) {
      return this.remainingUses.decrementAndGet() >= 0;
    }
    return true;
  }
}
