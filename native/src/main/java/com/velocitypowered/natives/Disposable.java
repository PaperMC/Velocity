/*
 * Copyright (C) 2018-2021 Velocity Contributors
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

package com.velocitypowered.natives;

import java.io.Closeable;

/**
 * This marker interface indicates that this object should be explicitly disposed before the object
 * can no longer be used. Not disposing these objects will likely leak native resources and
 * eventually lead to resource exhaustion.
 */
public interface Disposable extends Closeable {

  /**
   * Disposes this object. After this call returns, any use of this object becomes invalid. Multiple
   * calls to this function should be safe: there should be no side-effects once an object is
   * disposed.
   */
  @Override
  void close();
}
