/*
 * Copyright (C) 2020-2021 Velocity Contributors
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

package com.velocitypowered.proxy.protocol.netty;

import io.netty.util.ByteProcessor;

class TwentyOneBitVarintByteDecoder implements ByteProcessor {

  private int readVarint;
  private int bytesRead;
  private DecodeResult result = DecodeResult.TOO_SHORT;

  @Override
  public boolean process(byte k) {
    // if we have a run of zeroes, we want to skip over them
    if (k == 0 && bytesRead == 0) {
      result = DecodeResult.RUN_OF_ZEROES;
      return true;
    }
    if (result == DecodeResult.RUN_OF_ZEROES) {
      // if k is not zero, maybe we can decode a varint, but we don't track
      // how many bytes we read, so break out now. `MinecraftVarintFrameDecoder`
      // will skip over the zeroes and pick up where we left off.
      return false;
    }

    readVarint |= (k & 0x7F) << bytesRead++ * 7;
    if (bytesRead > 3) {
      result = DecodeResult.TOO_BIG;
      return false;
    }
    if ((k & 0x80) != 128) {
      result = DecodeResult.SUCCESS;
      return false;
    }
    return true;
  }

  public int getReadVarint() {
    return readVarint;
  }

  public int getBytesRead() {
    return bytesRead;
  }

  public DecodeResult getResult() {
    return result;
  }

  public enum DecodeResult {
    SUCCESS,
    TOO_SHORT,
    TOO_BIG,
    RUN_OF_ZEROES
  }
}
