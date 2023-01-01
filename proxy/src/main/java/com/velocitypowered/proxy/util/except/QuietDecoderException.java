/*
 * Copyright (C) 2020-2023 Velocity Contributors
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

package com.velocitypowered.proxy.util.except;

import io.netty.handler.codec.DecoderException;

/**
 * A special-purpose exception thrown when we want to indicate an error decoding but do not want to
 * see a large stack trace in logs.
 */
public class QuietDecoderException extends DecoderException {

  public QuietDecoderException(String message) {
    super(message);
  }

  @Override
  public synchronized Throwable fillInStackTrace() {
    return this;
  }
}
