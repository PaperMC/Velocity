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

package com.velocitypowered.proxy.event;

import com.velocitypowered.api.event.EventTask;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface UntargetedEventHandler {

  @Nullable EventTask execute(Object targetInstance, Object event);

  interface VoidHandler extends UntargetedEventHandler {

    @Override
    default @Nullable EventTask execute(final Object targetInstance, final Object event) {
      executeVoid(targetInstance, event);
      return null;
    }

    void executeVoid(Object targetInstance, Object event);
  }
}
