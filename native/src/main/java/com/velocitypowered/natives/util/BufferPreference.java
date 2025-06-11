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

package com.velocitypowered.natives.util;

/**
 * Emumerates Netty buffer preferences and requirements for use with Netty.
 */
public enum BufferPreference {
  /**
   * A heap buffer is required.
   */
  HEAP_REQUIRED,
  /**
   * A heap buffer is preferred (but not required).
   */
  HEAP_PREFERRED,
  /**
   * A direct buffer is preferred (but not required).
   */
  DIRECT_PREFERRED,
  /**
   * A direct buffer is required.
   */
  DIRECT_REQUIRED
}
