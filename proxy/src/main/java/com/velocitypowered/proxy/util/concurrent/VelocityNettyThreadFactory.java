/*
 * Copyright (C) 2019-2023 Velocity Contributors
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

package com.velocitypowered.proxy.util.concurrent;

import static com.google.common.base.Preconditions.checkNotNull;

import io.netty.util.concurrent.FastThreadLocalThread;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Factory to create threads for the Netty event loop groups.
 */
public class VelocityNettyThreadFactory implements ThreadFactory {

  private final AtomicInteger threadNumber = new AtomicInteger();
  private final String nameFormat;

  public VelocityNettyThreadFactory(String nameFormat) {
    this.nameFormat = checkNotNull(nameFormat, "nameFormat");
  }

  @Override
  public Thread newThread(Runnable r) {
    String name = String.format(nameFormat, threadNumber.getAndIncrement());
    return new FastThreadLocalThread(r, name);
  }
}
