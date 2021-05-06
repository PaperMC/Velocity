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

package com.velocitypowered.proxy.protocol.util;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ByteBufSkipList {

  private final List<ByteBuf> buffers = new ArrayList<>(Runtime.getRuntime().availableProcessors());

  /**
   * Adds {@link ByteBuf} to skip list.
   * @param buf ByteBuf
   */
  public void add(ByteBuf buf) {
    synchronized (buffers) {
      buffers.add(buf);
    }
  }

  /**
   * Checks if {@link ByteBuf} should be skipped.
   * @param buf ByteBuf
   * @return true if ByteBuf should be skipped
   */
  public boolean shouldSkip(ByteBuf buf) {
    synchronized (buffers) {
      for (ByteBuf byteBuf : buffers) {
        if (byteBuf == buf) {
          return true;
        }
      }
      return false;
    }
  }

  /**
   * Removes {@link ByteBuf} from skip list.
   * @param buf ByteBuf
   * @return true if ByteBuf was removed from skip list
   */
  public boolean remove(ByteBuf buf) {
    synchronized (buffers) {
      for (int i = 0; i < buffers.size(); i++) {
        if (buffers.get(i) == buf) {
          buffers.remove(i);
          return true;
        }
      }
    }
    return false;
  }
}
